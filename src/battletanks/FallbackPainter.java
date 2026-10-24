package battletanks;

import java.awt.Color;
import java.awt.Graphics;

// Draws simple shapes with Swing when a picture could not be loaded.
public class FallbackPainter {

    private static final Color BRICK_COLOR = new Color(150, 90, 40);
    private static final Color TRACK_COLOR = new Color(40, 40, 40);

    // The tank is drawn looking UP, the caller rotates the Graphics.
    public void drawTank(Graphics graphics,
            int tankX, int tankY, int tankSize, Color bodyColor) {

        int trackWidth = tankSize / 4;
        int centerX = tankX + tankSize / 2;
        int centerY = tankY + tankSize / 2;

        int turretSize = tankSize / 2;
        int barrelWidth = tankSize / 6;
        int barrelLength = tankSize / 2;

        // Two tracks
        graphics.setColor(TRACK_COLOR);
        graphics.fillRect(tankX, tankY, trackWidth, tankSize);
        graphics.fillRect(tankX + tankSize - trackWidth, tankY,
                trackWidth, tankSize);

        // Body
        graphics.setColor(bodyColor);
        graphics.fillRect(tankX + trackWidth, tankY + tankSize / 8,
                tankSize - 2 * trackWidth, tankSize * 3 / 4);

        // Barrel (from the center to the front)
        graphics.setColor(Color.DARK_GRAY);
        graphics.fillRect(centerX - barrelWidth / 2, centerY - barrelLength,
                barrelWidth, barrelLength);

        // Turret
        graphics.setColor(bodyColor.darker());
        graphics.fillOval(centerX - turretSize / 2, centerY - turretSize / 2,
                turretSize, turretSize);
    }

    public void drawWall(Graphics graphics, int blockX, int blockY,
            int blockWidth, int blockHeight, boolean isIndestructible) {

        if (isIndestructible) {
            graphics.setColor(Color.GRAY);
        } else {
            graphics.setColor(BRICK_COLOR);
        }

        graphics.fillRect(blockX, blockY, blockWidth, blockHeight);

        graphics.setColor(Color.BLACK);
        graphics.drawRect(blockX, blockY, blockWidth, blockHeight);
    }

    public void drawBullet(Graphics graphics,
            int bulletX, int bulletY, int bulletSize) {

        graphics.setColor(Color.BLACK);
        graphics.fillOval(bulletX, bulletY, bulletSize, bulletSize);
    }

    public void drawHeart(Graphics graphics,
            int heartX, int heartY, int heartSize) {

        graphics.setColor(Color.RED);
        graphics.fillOval(heartX, heartY, heartSize, heartSize);
    }
}
