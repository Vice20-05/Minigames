package battletanks;

import java.awt.Rectangle;

// One small block of a wall. A wall is made of 3 of these blocks.
public class WallBlock {

    public static final int DEFAULT_SIZE = 20;
    private static final int SHRINK_PER_LOST_HEALTH = 3;   // pixels the block shrinks on each side per hit

    private int positionX;
    private int positionY;
    private int size;
    private int health;
    private int maxHealth;
    private boolean indestructible;   // true for the main walls where the tanks live

    public WallBlock(int startX, int startY, boolean indestructible) {
        this.positionX = startX;
        this.positionY = startY;
        this.size = DEFAULT_SIZE;
        this.maxHealth = 3;
        this.health = 3;
        this.indestructible = indestructible;
    }

    // The block shrinks towards its center every time it loses health
    public Rectangle getBounds() {
        int lostHealth = maxHealth - health;
        int shrinkAmount = lostHealth * SHRINK_PER_LOST_HEALTH;
        int currentSize = size - 2 * shrinkAmount;

        return new Rectangle(positionX + shrinkAmount, positionY + shrinkAmount, currentSize, currentSize);
    }

    public boolean isDestroyed() {
        return health <= 0;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public boolean isIndestructible() {
        return indestructible;
    }
}
