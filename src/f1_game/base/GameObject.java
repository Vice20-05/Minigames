package f1_game.base;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * GameObject = the "parent" of everything we draw on the road (player car,
 * traffic cars, and later roadblocks / potholes).
 *
 * Every object has:
 *   - a position (x, y)       -> top-left corner, in pixels
 *   - a size (width, height)
 *   - a placeholder color     -> used while we have no art yet
 *   - an optional image       -> the real sprite, added later by the artist
 *
 * Child classes (PlayerCar, TrafficCar) write "extends GameObject" and get
 * all of this for free. That is called INHERITANCE.
 *
 * "abstract" means you cannot create a plain GameObject, only its children.
 */
public abstract class GameObject {

    // "protected" = this class AND its children can use these variables.
    // We use double (not int) for x and y so slow movements like 0.5 px
    // per frame still add up correctly.
    protected double x;
    protected double y;
    protected int width;
    protected int height;
    protected Color color;
    protected BufferedImage image; // stays null until there is art

    /**
     * @param imagePath path to a picture, for example "Resorces/Textures/player.png".
     *                  Pass null to just use a colored box (placeholder).
     */
    public GameObject(double x, double y, int width, int height, Color color, String imagePath) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.color = color;
        // If the picture is missing we get null and draw a colored rectangle instead.
        this.image = ImageLoader.load(imagePath);
    }

    /**
     * Draws the object. If we have a picture we draw the picture,
     * otherwise we draw a colored rectangle (the placeholder).
     */
    public void draw(Graphics2D g) {
        if (image != null) {
            g.drawImage(image, (int) x, (int) y, width, height, null);
        } else {
            g.setColor(color);
            g.fillRoundRect((int) x, (int) y, width, height, 12, 12);

            // A dark "windshield" so you can see which way the car faces.
            g.setColor(Color.DARK_GRAY);
            g.fillRect((int) x + 8, (int) y + 18, width - 16, 14);
        }
    }

    /**
     * The invisible box around the object. We use it to check collisions:
     * if two boxes overlap (intersects), the objects touch.
     */
    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, width, height);
    }

    // Getters: let other classes READ the values without changing them.
    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    /** y of the bottom edge (the back bumper for a car). */
    public double getBottomY() {
        return y + height;
    }
}
