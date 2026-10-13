package f1_game.base;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * TrafficCar = the other cars on the road. They are NOT AI,
 * they just drive straight forward in their lane at their own speed.
 *
 * HOW THEY MOVE ON THE SCREEN:
 * What matters is the DIFFERENCE between your speed and their speed.
 *   - You drive 200 km/h, they drive 80 km/h -> you catch up, they move DOWN the screen.
 *   - You drive 50 km/h,  they drive 80 km/h -> they pull away, they move UP the screen.
 */
public class TrafficCar extends GameObject {

    private final double speed; // km/h, never changes for this car

    /**
     * @param lane  which lane the car drives in (0 = left)
     * @param speed how fast this car drives, in km/h
     * @param color the placeholder color, only used if image is null
     * @param image the picture for this car (picked by the TrafficSpawner),
     *              or null to draw a colored box instead.
     */
    public TrafficCar(int lane, double speed, Color color, BufferedImage image) {
        // Start just ABOVE the top of the screen (negative y), so the car
        // "drives in" instead of popping up in the middle of the road.
        super(Road.laneCenterX(lane) - GameConfig.CAR_WIDTH / 2.0,
              -GameConfig.CAR_HEIGHT,
              GameConfig.CAR_WIDTH,
              GameConfig.CAR_HEIGHT,
              color,
              null); // no path: the picture is already loaded, we set it below
        this.speed = speed;

        // We got a picture: use it instead of the colored box.
        if (image != null) {
            this.image = image; // "image" lives in GameObject, we can use it because it is protected
            // Keep the picture's shape: same height for everyone, but a thin
            // motorbike gets a thin box (so it isn't stretched and the hitbox fits).
            // Example: motorbike picture 44 x 100 -> 90 * 44 / 100 = 39 pixels wide.
            //          car picture       70 x 131 -> 90 * 70 / 131 = 48 pixels wide.
            this.width = GameConfig.CAR_HEIGHT * image.getWidth() / image.getHeight();
            // The width changed, so center the car in its lane again
            // (lane center minus half the width = the left edge).
            this.x = Road.laneCenterX(lane) - width / 2.0;
        }
    }

    /**
     * Called once every frame.
     * @param playerSpeed the player's speed in km/h
     */
    public void update(double playerSpeed) {
        double relativeSpeed = playerSpeed - speed;
        y += relativeSpeed * GameConfig.PIXELS_PER_KMH;
    }

    /**
     * True if the car is far outside the screen (below OR above).
     * Then the GamePanel deletes it, so the list doesn't grow forever.
     */
    public boolean isOffScreen() {
        boolean belowScreen = y > GameConfig.WINDOW_HEIGHT + 50;
        boolean farAboveScreen = y < -height * 3;
        return belowScreen || farAboveScreen;
    }
}
