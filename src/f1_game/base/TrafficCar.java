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

    // km/h. cruiseSpeed is how fast the car LIKES to drive, it never changes.
    // speed is how fast it drives RIGHT NOW: lower when it is stuck behind a slower car.
    private final double cruiseSpeed;
    private double speed;

    // The lane the car is in, or DRIVING TO while it changes lanes.
    private int lane;
    // Each car changes lanes at most once, so it doesn't zig-zag around.
    private boolean hasChangedLane = false;
    // The screen height (y) where this car wants to switch lanes.
    // -1 = this car never switches (most cars). Set by the TrafficSpawner.
    private double laneChangeY = -1;

    // Did this car already give the player a near-miss bonus?
    // A car stays close for many frames, so without this we would give the bonus every frame.
    private boolean nearMissed = false;

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
        this.cruiseSpeed = speed;
        this.speed = speed;
        this.lane = lane;

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

        slideTowardsLane();
    }

    /**
     * If the car is not in the middle of its lane (because it is changing lanes),
     * move it a few pixels sideways. When it is close enough, snap it in place.
     */
    private void slideTowardsLane() {
        double targetX = Road.laneCenterX(lane) - width / 2.0;
        double distance = targetX - x;

        if (Math.abs(distance) <= GameConfig.LANE_CHANGE_SPEED) {
            x = targetX; // close enough: we arrived
        } else if (distance > 0) {
            x += GameConfig.LANE_CHANGE_SPEED; // target is to the right
        } else {
            x -= GameConfig.LANE_CHANGE_SPEED; // target is to the left
        }
    }

    /** Start driving to another lane. The car slides there over the next frames. */
    public void changeLane(int newLane) {
        lane = newLane;
        hasChangedLane = true;
    }

    /**
     * Keeps a safe distance to the car in front of us (in the same lane), so cars never overlap.
     * @param carAhead the closest car in front of us in our lane, or null if the lane is free
     */
    public void followCar(TrafficCar carAhead) {
        if (carAhead == null) {
            speedBackUp();
            return;
        }

        // Empty space between their back bumper and our front bumper.
        // (In front = higher on the screen = smaller y.)
        double gap = y - (carAhead.y + carAhead.height);

        if (gap < 0) {
            // Already overlapping (can happen right after a lane change):
            // put us right behind them and drive at their speed.
            y = carAhead.y + carAhead.height;
            speed = Math.min(speed, carAhead.speed);
        } else if (gap < GameConfig.TRAFFIC_FOLLOW_DISTANCE) {
            // Too close: slow down to their speed, so the gap stops shrinking.
            speed = Math.min(speed, carAhead.speed);
        } else {
            speedBackUp(); // enough space, drive normally again
        }
    }

    /** Slowly speed up again until we are back at our cruise speed. */
    private void speedBackUp() {
        speed = Math.min(speed + GameConfig.TRAFFIC_ACCELERATION, cruiseSpeed);
    }

    public int getLane() {
        return lane;
    }

    public boolean hasChangedLane() {
        return hasChangedLane;
    }

    /** This car will switch lanes once it drives down to height y on the screen. */
    public void planLaneChange(double y) {
        laneChangeY = y;
    }

    /** True if this car was picked to switch lanes AND it reached its spot on the screen. */
    public boolean wantsToChangeLane() {
        return laneChangeY >= 0 && y >= laneChangeY;
    }

    /** True if this car already gave its near-miss bonus. */
    public boolean hasNearMissed() {
        return nearMissed;
    }

    /** Remember that this car gave its bonus, so it only counts ONCE. */
    public void markNearMissed() {
        nearMissed = true;
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
