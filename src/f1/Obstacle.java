package f1;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Obstacle = something that stands STILL on the road: a roadblock or a pothole.
 *
 * It doesn't drive (speed 0), so it moves down the screen exactly as fast as the
 * road lines: playerSpeed * PIXELS_PER_KMH. Stop the car and it stops too.
 *
 * A "work zone" is 2 obstacles in the same lane: the roadblock (the barrier you see first)
 * and the pothole right BEHIND it (further up the road = higher on the screen).
 * Hitting either one = game over. Use Obstacle.roadblock(...) / Obstacle.pothole(...) to make them.
 */
public class Obstacle extends GameObject {

    private final int lane;
    private final boolean isRoadblock; // true = roadblock, false = pothole
    private boolean passed = false;    // did the player already drive past it?

    // "private": nobody calls this directly, use the two methods below.
    private Obstacle(int lane, double y, int width, int height, Color color, BufferedImage image, boolean isRoadblock) {
        super(Road.laneCenterX(lane) - width / 2.0, y, width, height, color, null);
        this.image = image; // already loaded by the RoadblockSpawner (null = draw a shape instead)
        this.lane = lane;
        this.isRoadblock = isRoadblock;
    }

    /** A roadblock just above the top of the screen, in the middle of the lane. */
    public static Obstacle roadblock(int lane, BufferedImage image) {
        int width = GameConfig.ROADBLOCK_WIDTH;
        // Keep the picture's shape (448 x 144 -> 170 x 54). No picture: a 50 px tall bar.
        int height = image != null ? width * image.getHeight() / image.getWidth() : 50;
        return new Obstacle(lane, -height, width, height, Color.ORANGE, image, true);
    }

    /** A pothole right behind (above) the roadblock, in the same lane. */
    public static Obstacle pothole(int lane, Obstacle roadblock, BufferedImage image) {
        int width = GameConfig.POTHOLE_WIDTH;
        int height = image != null ? width * image.getHeight() / image.getWidth() : 45;
        double y = roadblock.getY() - GameConfig.POTHOLE_GAP - height;
        return new Obstacle(lane, y, width, height, new Color(35, 30, 28), image, false);
    }

    /** Called once every frame: it stands still, so it moves with the road. */
    public void update(double playerSpeed) {
        y += playerSpeed * GameConfig.PIXELS_PER_KMH;
    }

    /** Uses the picture if there is one, otherwise draws a simple shape. */
    @Override
    public void draw(Graphics2D g) {
        if (image != null) {
            g.drawImage(image, (int) x, (int) y, width, height, null);
        } else if (isRoadblock) {
            // orange and white stripes, like a real barrier
            for (int i = 0; i * 20 < width; i++) {
                g.setColor(i % 2 == 0 ? Color.ORANGE : Color.WHITE);
                g.fillRect((int) x + i * 20, (int) y, Math.min(20, width - i * 20), height);
            }
        } else {
            // a dark hole with a lighter rim
            g.setColor(new Color(90, 85, 80));
            g.fillOval((int) x - 3, (int) y - 3, width + 6, height + 6);
            g.setColor(color);
            g.fillOval((int) x, (int) y, width, height);
        }
    }

    public int getLane() {
        return lane;
    }

    public boolean isRoadblock() {
        return isRoadblock;
    }

    public boolean isPassed() {
        return passed;
    }

    public void markPassed() {
        passed = true;
    }

    /** True when it is below the screen, then the GamePanel deletes it. */
    public boolean isOffScreen() {
        return y > GameConfig.WINDOW_HEIGHT + 50;
    }
}
