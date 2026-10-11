package f1_game.base;

import java.awt.Color;

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

    public TrafficCar(int lane, double speed, Color color) {
        // Start just ABOVE the top of the screen (negative y), so the car
        // "drives in" instead of popping up in the middle of the road.
        super(Road.laneCenterX(lane) - GameConfig.CAR_WIDTH / 2.0,
              -GameConfig.CAR_HEIGHT,
              GameConfig.CAR_WIDTH,
              GameConfig.CAR_HEIGHT,
              color,
              null); // later: an image path like "Resorces/Textures/traffic1.png"
        this.speed = speed;
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
