package f1_game.base;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/**
 * PerfMonitor = hidden DEV overlay that shows frame timings. Press F3 in the game to toggle it.
 * Players aren't told about it, it's for us to spot stutter / slow frames.
 *
 * It measures, every second:
 *   tick    = time between two game loop ticks (should be ~16.7 ms, steady)
 *   frame   = time between two paintComponent calls = frames that really get drawn
 *   update  = how long updateGame takes (moving everything)
 *   paint   = how long paintComponent takes (drawing everything)
 *   skipped = ticks skipped because the Swing thread was still busy with the last one
 * Shown as "average / worst" in milliseconds. Also printed to the console.
 * Start with -Df1.perf=true to have it on from the beginning.
 */
public class PerfMonitor {

    private static final Object lock = new Object();
    private static volatile boolean enabled = Boolean.getBoolean("f1.perf");

    private static final Stat tick = new Stat();
    private static final Stat frame = new Stat();
    private static final Stat update = new Stat();
    private static final Stat paint = new Stat();
    private static long lastTick;
    private static long lastFrame;
    private static int skipped;
    private static long windowStart = System.nanoTime();
    private static String text = "measuring...";

    public static void toggle() {
        enabled = !enabled;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void onTick() {
        long now = System.nanoTime();
        synchronized (lock) {
            if (lastTick != 0) {
                tick.add(now - lastTick);
            }
            lastTick = now;
        }
    }

    public static void onFrameStart() {
        long now = System.nanoTime();
        synchronized (lock) {
            if (lastFrame != 0) {
                frame.add(now - lastFrame);
            }
            lastFrame = now;
        }
    }

    public static void onPaintDone(long nanos, int width, int height) {
        synchronized (lock) {
            paint.add(nanos);
            long now = System.nanoTime();
            if (now - windowStart >= 1_000_000_000L) {
                text = String.format("%dx%d  fps %d | tick %s | frame %s | update %s | paint %s | skipped %d",
                        width, height, frame.count, tick, frame, update, paint, skipped);
                if (enabled) {
                    System.out.println("[perf] " + text);
                }
                tick.reset();
                frame.reset();
                update.reset();
                paint.reset();
                skipped = 0;
                windowStart = now;
            }
        }
    }

    public static void onUpdate(long nanos) {
        synchronized (lock) {
            update.add(nanos);
        }
    }

    public static void onTickSkipped() {
        synchronized (lock) {
            skipped++;
        }
    }

    /** Draws the numbers in the bottom left corner (in screen pixels, not game pixels). */
    public static void draw(Graphics2D g, int panelHeight) {
        if (!enabled) {
            return;
        }
        String shown;
        synchronized (lock) {
            shown = text;
        }
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
        int width = g.getFontMetrics().stringWidth(shown);
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, panelHeight - 24, width + 12, 24);
        g.setColor(Color.GREEN);
        g.drawString(shown, 6, panelHeight - 7);
    }

    /** Average and worst of some times, in ms. */
    private static class Stat {
        long sum;
        long max;
        int count;

        void add(long nanos) {
            sum += nanos;
            max = Math.max(max, nanos);
            count++;
        }

        void reset() {
            sum = 0;
            max = 0;
            count = 0;
        }

        @Override
        public String toString() {
            if (count == 0) {
                return "-";
            }
            return String.format("%.1f/%.1f", sum / 1e6 / count, max / 1e6);
        }
    }

    private PerfMonitor() {
    }
}
