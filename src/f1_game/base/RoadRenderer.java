package f1_game.base;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * RoadRenderer = paints the road on a BACKGROUND THREAD, so the Swing thread doesn't have to.
 *
 * WHY?
 * Swing does everything on ONE thread (the "Event Dispatch Thread", EDT): key presses,
 * the game loop Timer AND painting. Painting the grass tiles + asphalt + lane lines is
 * the biggest part of every frame. Here a second thread ("road-painter") paints the road
 * into a hidden picture (a BufferedImage), and the EDT only has to copy that finished
 * picture onto the screen, which is very fast.
 *
 * HOW IT STAYS SAFE (threads must never fight over the same data):
 *   1) The road thread gets a FROZEN COPY of the road (Road.snapshot()). The game loop
 *      can keep moving the real road, the copy never changes.
 *   2) Two pictures: "finished" (the EDT shows it) and "spare" (the road thread paints
 *      in it). When a new frame is done they swap, inside a synchronized block, so the
 *      EDT never shows a half-painted picture.
 *   3) If the road thread is still busy with the last frame, the new request is SKIPPED
 *      instead of waiting in a queue. So it can never pile up and freeze the game.
 */
public class RoadRenderer {

    // A "single thread executor" = one worker thread that runs the jobs we give it.
    private final ExecutorService roadThread = Executors.newSingleThreadExecutor(job -> {
        Thread thread = new Thread(job, "road-painter");
        thread.setDaemon(true); // a daemon thread doesn't keep the program running after the window closes
        return thread;
    });

    // true while the road thread is painting. Atomic = safe to read/change from 2 threads.
    private final AtomicBoolean painting = new AtomicBoolean(false);

    // What to do when a new frame is ready (GamePanel passes its repaint()).
    private final Runnable onFrameReady;

    // Only one thread at a time may be inside synchronized (swapLock) { ... }
    private final Object swapLock = new Object();
    private BufferedImage finished; // the last complete road picture, shown by the EDT
    private BufferedImage spare;    // the picture the road thread paints into next

    public RoadRenderer(Runnable onFrameReady) {
        this.onFrameReady = onFrameReady;
    }

    /**
     * Called by the game loop (on the EDT) once per frame: "please paint this road".
     * Returns right away, the painting happens on the road thread.
     */
    public void requestFrame(Road road, int panelWidth, int panelHeight) {
        if (panelWidth <= 0 || panelHeight <= 0) {
            onFrameReady.run(); // window not on the screen yet, nothing to paint
            return;
        }
        // compareAndSet(false, true): "if not painting, start painting" in ONE step,
        // so two frames can never start at the same time.
        if (!painting.compareAndSet(false, true)) {
            return; // still busy with the last frame -> skip this one (it will repaint when done)
        }

        Road frozenRoad = road.snapshot(); // copy it NOW, on the EDT, before the game moves it again
        try {
            roadThread.execute(() -> {
                try {
                    paintFrame(frozenRoad, panelWidth, panelHeight);
                    onFrameReady.run(); // repaint() is one of the few Swing methods that is safe from any thread
                } finally {
                    painting.set(false); // ready for the next frame, even if painting crashed
                }
            });
        } catch (RejectedExecutionException e) {
            painting.set(false); // stop() was already called, the thread is gone
        }
    }

    /** Runs on the ROAD THREAD. Paints the background + road into the spare picture, then swaps. */
    private void paintFrame(Road road, int panelWidth, int panelHeight) {
        BufferedImage image = spare;
        // First frame, or the window changed size: make a new picture with the right size.
        if (image == null || image.getWidth() != panelWidth || image.getHeight() != panelHeight) {
            image = new BufferedImage(panelWidth, panelHeight, BufferedImage.TYPE_INT_RGB);
        }

        Graphics2D g = image.createGraphics();
        try {
            g.setColor(Color.BLACK); // clear the old frame (like super.paintComponent does)
            g.fillRect(0, 0, panelWidth, panelHeight);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            // The same steps paintComponent used to do for the road (see ScreenFit for the math).
            ScreenFit fit = ScreenFit.of(panelWidth, panelHeight);
            road.drawBackground(g, panelWidth, panelHeight, fit.roadLeftX(), fit.roadRightX(), fit.tileSize());

            g.translate(fit.offsetX(), fit.offsetY());
            g.scale(fit.scale(), fit.scale());
            g.clipRect(0, 0, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);
            road.draw(g);
        } finally {
            g.dispose(); // free the "pen"
        }

        // Swap: the new picture becomes "finished", the old one becomes the next "spare".
        synchronized (swapLock) {
            spare = finished;
            finished = image;
        }
    }

    /** Called by paintComponent (on the EDT): copies the last finished road picture onto the screen. */
    public void draw(Graphics2D g) {
        synchronized (swapLock) {
            if (finished != null) {
                g.drawImage(finished, 0, 0, null);
            }
        }
    }

    /** Stops the road thread when the window is closed. */
    public void stop() {
        roadThread.shutdownNow();
    }
}
