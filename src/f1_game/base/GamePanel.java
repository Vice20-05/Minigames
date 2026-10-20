package f1_game.base;

import Database.DB_Main;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/**
 * GamePanel = the HEART of the game. It is the "canvas" inside the window.
 *
 * It does 2 jobs, 60 times per second (this is the GAME LOOP):
 *   1) UPDATE -> move everything, check crashes, add score   (updateGame)
 *   2) DRAW   -> paint everything on the screen               (paintComponent)
 *
 * ONE loop drives everything: the "game-loop" thread wakes up exactly every TICK_NANOS
 * (1/60 s) and asks the Swing thread to run tick() = update + draw. See runLoop().
 */
public class GamePanel extends JPanel {

    // The game loop thread. It only keeps the beat, all game work happens on the Swing thread.
    private final Thread loopThread = new Thread(this::runLoop, "game-loop");
    private volatile boolean running; // volatile = the loop thread always sees the newest value
    // true while a tick is waiting for / running on the Swing thread (so ticks never pile up)
    private final AtomicBoolean tickQueued = new AtomicBoolean(false);
    private final KeyHandler keys = new KeyHandler();

    // Everything that lives in the game:
    private Road road;
    private PlayerCar player;
    private List<TrafficCar> traffic;
    private TrafficSpawner spawner;
    private List<Obstacle> obstacles;           // roadblocks + potholes on the road
    private RoadblockSpawner roadblockSpawner;

    private double score;      // every point already has the multiplier in it, so it never goes down
    private int ticks;         // how many frames since the start (ticks / FPS = seconds)
    private boolean gameOver;
    private boolean started;   // false until the player presses W the first time

    // Popup text above the player, for a moment: "NEAR MISS! +50", "ROADBLOCK CLEARED! +15 km/h"
    private int popupTicks;          // frames left to show the text (0 = hidden)
    private int popupTotalTicks;     // how long it was shown in total (for the fade out)
    private String popupText = "";   // what is written
    private int nearMissCount;       // how many near misses this run (shown at game over)

    // Online leaderboard: after a crash the player types a name, then it is saved (see DB_Main).
    //   TYPING -> Enter -> SAVING -> SAVED or FAILED        TYPING -> Esc -> SKIPPED
    private enum SaveState { TYPING, SAVING, SAVED, FAILED, SKIPPED }
    private SaveState saveState;
    private String playerName = "";  // what is typed on the game over screen right now
    private String lastName = "";    // the name used last time, filled in again (only while the game is open)
    // true right after a crash: W/A/S/D are probably still held, and holding a key "types" it
    // again and again (wwwww). So we ignore typing until all driving keys are let go.
    private boolean waitForKeyRelease;

    // The grass next to the road, painted once and then only copied (see BackgroundCache).
    private final BackgroundCache background = new BackgroundCache();

    // false while the pictures are still loading on the background thread -> "Loading..." screen
    private boolean loaded = false;

    public GamePanel() {
        setPreferredSize(new Dimension(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT));
        setBackground(Color.BLACK);
        setDoubleBuffered(true);  // draws to a hidden image first -> no flickering
        setFocusable(true);       // the panel must have "focus" to receive key presses
        addKeyListener(keys);

        loadImagesInBackground(); // resetGame() is called when the loading is done

        loopThread.setDaemon(true); // a daemon thread doesn't keep the program running after the window closes
    }

    /**
     * Reads all the pictures from the disk on a BACKGROUND THREAD, so the window opens
     * right away and doesn't freeze while the files load.
     *
     * SwingWorker is Swing's helper for this:
     *   doInBackground() -> runs on a background thread (slow work: reading files)
     *   done()           -> runs on the Swing thread afterwards (safe to touch the game again)
     *
     * ImageLoader remembers every picture, so the "new Road()", "new TrafficSpawner()"...
     * in resetGame() just take them from memory: no loading on the Swing thread, also not on restart.
     */
    private void loadImagesInBackground() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                ImageLoader.load(PlayerCar.IMAGE_PATH);
                ImageLoader.load(Road.BACKGROUND_PATH);
                ImageLoader.load(Road.ROAD_PATH);
                ImageLoader.loadFolder(TrafficSpawner.TRAFFIC_FOLDER);
                ImageLoader.load(RoadblockSpawner.ROADBLOCK_PATH);
                ImageLoader.load(RoadblockSpawner.POTHOLE_PATH);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // throws if doInBackground crashed
                } catch (InterruptedException | ExecutionException e) {
                    System.out.println("Loading pictures failed -> using colors instead: " + e);
                }
                resetGame();
                loaded = true;
                repaint();
            }
        }.execute();
    }

    /** Starts the game loop. Called by GameWindow after the window is visible. */
    public void start() {
        requestFocusInWindow(); // ask for keyboard focus
        running = true;
        loopThread.start();
    }

    /** Stops the game loop when the containing window is closed. */
    public void stop() {
        running = false;
    }

    /**
     * Runs on the "game-loop" thread: keeps a steady beat of exactly FPS ticks per second.
     *
     * WHY NOT a javax.swing.Timer? A Timer only counts whole milliseconds, so "every 16 ms"
     * is 62.5 ticks per second while the screen shows 60 frames. Every second ~2-3 ticks
     * were never shown, and the road jumped a bit each time (stutter). Here we count in
     * nanoseconds, so it's exactly 60.
     *
     * The game objects are only touched on the Swing thread (inside tick()), so the two
     * threads never fight over them. If the Swing thread is still busy with the last tick,
     * this one is skipped instead of queued, so it can never pile up and freeze the game.
     */
    private void runLoop() {
        long nextTick = System.nanoTime();
        while (running) {
            if (tickQueued.compareAndSet(false, true)) { // "if no tick is waiting, queue one" in ONE step
                SwingUtilities.invokeLater(this::tick);
            } else {
                PerfMonitor.onTickSkipped();
            }

            nextTick += GameConfig.TICK_NANOS;
            long wait = nextTick - System.nanoTime();
            if (wait > 0) {
                LockSupport.parkNanos(wait); // sleep until the next tick (more exact than Thread.sleep)
            } else if (wait < -GameConfig.TICK_NANOS * 5) {
                nextTick = System.nanoTime(); // way behind (e.g. the PC froze): don't try to catch up
            }
        }
    }

    /** Puts everything back to the beginning (used at start and after a crash). */
    private void resetGame() {
        road = new Road();
        player = new PlayerCar();
        traffic = new ArrayList<>();
        spawner = new TrafficSpawner();
        obstacles = new ArrayList<>();
        roadblockSpawner = new RoadblockSpawner();
        score = 0;
        ticks = 0;
        gameOver = false;
        started = false; // after a restart (R) we wait for W again
        popupTicks = 0;
        nearMissCount = 0;
    }

    /** One frame, 60 times per second, on the Swing thread (queued by runLoop). */
    private void tick() {
        try {
            PerfMonitor.onTick(); // dev overlay timing (F3, see PerfMonitor)
            if (loaded) { // pictures still loading -> nothing to update yet, only draw "Loading..."
                long updateStart = System.nanoTime();
                updateGame();
                PerfMonitor.onUpdate(System.nanoTime() - updateStart);
            }
            // Draw NOW (repaint() would only say "draw soon"), so every update gets exactly one frame.
            paintImmediately(0, 0, getWidth(), getHeight());
            // Linux: push the frame to the screen right away instead of letting it wait in a buffer.
            Toolkit.getDefaultToolkit().sync();
        } finally {
            tickQueued.set(false); // the loop may queue the next tick, even if this one crashed
        }
    }

    // =====================================================================
    //  1) UPDATE
    // =====================================================================
    private void updateGame() {
        // Take the typed letters EVERY frame, so they don't pile up while driving
        String typed = keys.takeTypedKeys();

        if (gameOver) {
            if (saveState == SaveState.TYPING) {
                updateNameInput(typed); // R is a letter here, so it does NOT restart
            } else if (keys.isRestartPressed()) {
                resetGame();
            }
            return; // when the game is over, nothing moves
        }

        // Wait for the first W press. Until then nothing moves: no time, no score, no traffic.
        if (!started) {
            if (keys.isUpPressed()) {
                started = true; // go! from the next line on the game runs normally
            } else {
                return; // still waiting, skip the rest of the update
            }
        }

        ticks++;

        // Move the player (reads the keyboard)
        player.update(keys);

        // Scroll the road based on the player's speed
        road.update(player.getSpeed() * GameConfig.PIXELS_PER_KMH);

        // Maybe add a roadblock + pothole (from 1000 points on), and a new traffic car
        roadblockSpawner.update(obstacles, traffic, getScore());
        spawner.update(traffic, obstacles, getScore());

        // Cars that were picked to switch lanes do it when they reach their spot
        spawner.updateLaneChanges(traffic, obstacles);

        // Move every traffic car and every obstacle
        for (TrafficCar car : traffic) {
            car.update(player.getSpeed());
        }
        for (Obstacle obstacle : obstacles) {
            obstacle.update(player.getSpeed());
        }

        // Faster cars brake behind slower ones and stop behind roadblocks, so nothing overlaps
        spawner.keepDistance(traffic, obstacles);

        // Delete things that left the screen ("removeIf" = remove every one where the test is true)
        traffic.removeIf(TrafficCar::isOffScreen);
        obstacles.removeIf(Obstacle::isOffScreen);

        checkCollisions();
        checkRoadblockPassed();
        updateScore();

        // Count down the popup text, it disappears when it reaches 0
        if (popupTicks > 0) {
            popupTicks--;
        }
    }

    /** Shows a text above the player for "ticks" frames. */
    private void showPopup(String text, int ticks) {
        popupText = text;
        popupTicks = ticks;
        popupTotalTicks = ticks;
    }

    /**
     * When the WHOLE work zone is behind the player (the pothole is the last part),
     * the player gets points and a small speed boost. Each work zone only once (markPassed).
     */
    private void checkRoadblockPassed() {
        for (Obstacle obstacle : obstacles) {
            boolean behindPlayer = obstacle.getY() > player.getBottomY();
            if (!obstacle.isRoadblock() && !obstacle.isPassed() && behindPlayer) {
                obstacle.markPassed();
                int points = addPoints(GameConfig.ROADBLOCK_CLEAR_BONUS);
                player.addSpeedBoost(GameConfig.ROADBLOCK_PASS_BOOST);
                showPopup("CLEARED! +" + points + "  +" + (int) GameConfig.ROADBLOCK_PASS_BOOST + " km/h",
                          GameConfig.POPUP_TICKS);
            }
        }
    }

    private void checkCollisions() {
        // Roadblock or pothole: always game over, there is no near miss with a wall
        for (Obstacle obstacle : obstacles) {
            if (player.getBounds().intersects(obstacle.getBounds())) {
                onGameOver();
                return;
            }
        }

        for (TrafficCar car : traffic) {
            // intersects = do the two invisible boxes overlap
            if (player.getBounds().intersects(car.getBounds())) {
                onGameOver();
                return;
            }
            // We only get here if the car did NOT crash into us.
            // Near miss = the traffic car touches the player's BIGGER box (but not the car itself).
            // !car.hasNearMissed() makes sure each car gives the bonus only once.
            if (player.getNearMissBounds().intersects(car.getBounds()) && !car.hasNearMissed()) {
                int points = addPoints(GameConfig.NEAR_MISS_BONUS);
                car.markNearMissed(); // this car is done, no more bonus from it

                nearMissCount++;
                showPopup("NEAR MISS! +" + points, GameConfig.NEAR_MISS_POPUP_TICKS);
            }
        }
    }

    /** The player crashed: stop the game and ask for a name for the leaderboard. */
    private void onGameOver() {
        gameOver = true;
        saveState = SaveState.TYPING;
        playerName = lastName; // already filled in, Enter is enough to save again
        waitForKeyRelease = true;
    }

    /** Game over screen, while typing the name. "typed" = the keys typed this frame. */
    private void updateNameInput(String typed) {
        if (waitForKeyRelease) {
            if (!keys.isDrivingKeyPressed()) {
                waitForKeyRelease = false; // all let go: from the next frame on typing works
            }
            return;
        }

        for (char c : typed.toCharArray()) {
            if (c == KeyHandler.ENTER) {
                if (!playerName.isEmpty()) { // empty name + Enter = do nothing
                    saveScoreOnline();
                    return;
                }
            } else if (c == KeyHandler.ESCAPE) {
                saveState = SaveState.SKIPPED;
                return;
            } else if (c == KeyHandler.BACKSPACE) {
                if (!playerName.isEmpty()) {
                    playerName = playerName.substring(0, playerName.length() - 1); // cut the last letter
                }
            } else if (playerName.length() < GameConfig.NAME_MAX_LENGTH) {
                playerName += c; // KeyHandler only lets letters and numbers through
            }
        }
    }

    /**
     * Sends the score to the online leaderboard on a BACKGROUND THREAD.
     * Without internet it can take a few seconds before it gives up, and tick() runs
     * on the Swing thread: waiting there would freeze the whole game.
     * Same idea as loadImagesInBackground(): doInBackground() = slow work, done() = back on Swing.
     */
    private void saveScoreOnline() {
        saveState = SaveState.SAVING;
        lastName = playerName;
        // Copy them now: the background thread must not read the game's variables later
        String name = playerName;
        int finalScore = (int) score;

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return DB_Main.saveScore(DB_Main.F1, name, finalScore);
            }

            @Override
            protected void done() {
                boolean saved;
                try {
                    saved = get(); // the true/false from doInBackground
                } catch (InterruptedException | ExecutionException e) {
                    saved = false;
                }
                if (saveState == SaveState.SAVING) { // the player may have already restarted
                    saveState = saved ? SaveState.SAVED : SaveState.FAILED;
                }
            }
        }.execute();
    }

    /** Points for driving: POINTS_PER_SECOND every second, times the multiplier. */
    private void updateScore() {
        addPoints(GameConfig.POINTS_PER_SECOND / GameConfig.FPS);
    }

    /**
     * Adds points x the multiplier RIGHT NOW and gives back how many were added (for popups).
     * The multiplier is used only at the moment the points are earned. So braking later
     * doesn't take anything away: the score only goes up (time, near misses, cleared roadblocks).
     */
    private int addPoints(double basePoints) {
        double points = basePoints * getMultiplier();
        score += points;
        return (int) points;
    }

    private double getScore() {
        return score;
    }

    /**
     * Faster = more points. 0 km/h -> x1.0, 100 km/h -> x1.5, 200 km/h -> x2.0,
     * and it stops at MAX_MULTIPLIER.
     */
    private double getMultiplier() {
        double multiplier = 1.0 + player.getSpeed() / 200.0;
        return Math.min(multiplier, GameConfig.MAX_MULTIPLIER);
    }

    // =====================================================================
    //  2) DRAW
    // =====================================================================

    /**
     * Swing calls this every time the panel needs to be painted.
     * NEVER call it yourself, call repaint() instead.
     * ORDER MATTERS: what you draw first is in the back, what you draw last is on top.
     */
    @Override
    protected void paintComponent(Graphics graphics) {
        PerfMonitor.onFrameStart(); // dev overlay timing (F3, see PerfMonitor)
        long paintStart = System.nanoTime();
        super.paintComponent(graphics); // clears the old frame
        Graphics2D g = (Graphics2D) graphics; // Graphics2D has more features than Graphics

        // Smooth edges on shapes and text, and smooth pictures when they are scaled
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // FULLSCREEN / RESIZING:
        // The game is always WINDOW_WIDTH x WINDOW_HEIGHT "game pixels". If the panel is bigger
        // (fullscreen), ScreenFit tells us how much to zoom ("scale") and where the middle is.
        ScreenFit fit = ScreenFit.of(getWidth(), getHeight());

        // From here on we draw in GAME pixels: move + zoom the "pen", and don't draw outside the game.
        AffineTransform screenPixels = g.getTransform(); // remember, for the dev overlay at the end
        if (!loaded) {
            g.translate(fit.offsetX(), fit.offsetY());
            g.scale(fit.scale(), fit.scale());
            drawLoadingScreen(g); // the game objects don't exist yet
            return;
        }

        // Grass (very back): painted once, here only copied onto the screen (see BackgroundCache).
        background.draw(g, road, getWidth(), getHeight());

        g.translate(fit.offsetX(), fit.offsetY());
        g.scale(fit.scale(), fit.scale());
        g.clipRect(0, 0, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);

        road.draw(g);               // asphalt + lane lines
        for (Obstacle obstacle : obstacles) {
            obstacle.draw(g);       // on the ground, so below the cars
        }
        for (TrafficCar car : traffic) {
            car.draw(g);
        }
        player.draw(g);
        drawPopup(g);
        drawHud(g);                 // front
        if (!started) {
            drawStartScreen(g);     // "Press W to start" on top of everything
        }
        if (gameOver) {
            drawGameOver(g);
        }

        // Dev overlay (F3, see PerfMonitor). Back to screen pixels for the numbers.
        g.setTransform(screenPixels);
        g.setClip(null);
        PerfMonitor.draw(g, getHeight());
        PerfMonitor.onPaintDone(System.nanoTime() - paintStart, getWidth(), getHeight());
    }

    /** HUD = the text on top of the game (speed, score, time). */
    private void drawHud(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 150)); // black, half transparent
        g.fillRect(0, 0, GameConfig.WINDOW_WIDTH, 40);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Speed: " + (int) player.getSpeed() + " km/h", 10, 26);
        g.drawString("Score: " + (int) getScore(), 190, 26);
        g.drawString(String.format("x%.1f", getMultiplier()), 300, 26);
        g.drawString("Time: " + (ticks / GameConfig.FPS) + "s", 380, 26);
    }

    /**
     * The popup ("NEAR MISS! +50", "CLEARED! +15 km/h") above the player. It floats up and
     * fades out: the fewer frames are left, the higher and more transparent it is.
     */
    private void drawPopup(Graphics2D g) {
        if (popupTicks <= 0) {
            return; // nothing to show
        }
        // 1.0 right after it appeared, going down to 0.0 at the end
        double timeLeft = (double) popupTicks / popupTotalTicks;

        int alpha = (int) (255 * timeLeft);        // 255 = solid, 0 = invisible
        int floatUp = (int) (40 * (1 - timeLeft)); // moves 40 pixels up over time

        g.setColor(new Color(255, 215, 0, alpha)); // gold
        g.setFont(new Font("Arial", Font.BOLD, 24));
        drawCentered(g, popupText, GameConfig.PLAYER_Y - 30 - floatUp);
    }

    /** Shown while the pictures load on the background thread. */
    private void drawLoadingScreen(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 32));
        drawCentered(g, "Loading...", GameConfig.WINDOW_HEIGHT / 2);
    }

    /** Shown before the game starts, until W is pressed. */
    private void drawStartScreen(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 150)); // black, half transparent, so you still see the road
        g.fillRect(0, 0, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 32));
        drawCentered(g, "Press W  or ↑ to start, ", GameConfig.WINDOW_HEIGHT / 2);
    }

    private void drawGameOver(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);

        g.setColor(Color.RED);
        g.setFont(new Font("Hardigan", Font.BOLD, 48));
        drawCentered(g, "Game Over !", GameConfig.WINDOW_HEIGHT / 2 - 40);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 22));
        drawCentered(g, "Score: " + (int) getScore(), GameConfig.WINDOW_HEIGHT / 2 + 10);
        drawCentered(g, "Near misses: " + nearMissCount, GameConfig.WINDOW_HEIGHT / 2 + 45);
        drawSaveScore(g, GameConfig.WINDOW_HEIGHT / 2 + 95);
    }

    /** The name input / saving status under the score. y = the first line. */
    private void drawSaveScore(Graphics2D g, int y) {
        g.setFont(new Font("Arial", Font.PLAIN, 22));
        if (saveState == SaveState.TYPING) {
            g.setColor(Color.WHITE);
            drawCentered(g, "Enter your name: " + playerName + "_", y);
            g.setFont(new Font("Arial", Font.PLAIN, 16));
            drawCentered(g, "[Enter] save    [Esc] skip", y + 35);
            return; // no "Press R" while typing, R is a letter now
        }

        if (saveState == SaveState.SAVING) {
            g.setColor(Color.LIGHT_GRAY);
            drawCentered(g, "Saving...", y);
        } else if (saveState == SaveState.SAVED) {
            g.setColor(Color.GREEN);
            drawCentered(g, "Saved!", y);
        } else if (saveState == SaveState.FAILED) {
            g.setColor(Color.ORANGE);
            drawCentered(g, "Couldn't save (no internet?)", y);
        }
        // SKIPPED: no status text

        g.setColor(Color.WHITE);
        drawCentered(g, "Press R to restart", y + 35);
    }

    /** Draws text in the horizontal middle of the window. */
    private void drawCentered(Graphics2D g, String text, int y) {
        int textWidth = g.getFontMetrics().stringWidth(text);
        g.drawString(text, (GameConfig.WINDOW_WIDTH - textWidth) / 2, y);
    }
}
