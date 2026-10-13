package f1_game.base;

import java.awt.Color;
import java.awt.Rectangle;

/**
 * PlayerCar = the car YOU drive.
 *
 * - It stays near the bottom of the screen (only x changes, y stays the same).
 * - W / Up arrow    -> accelerate
 * - S / Down arrow  -> brake
 * - A / Left arrow  -> steer left  (hold it: the car turns more and more, like an analog stick)
 * - D / Right arrow -> steer right
 */
public class PlayerCar extends GameObject {

    private double speed;      // current speed in km/h

    // How fast we move SIDEWAYS right now, in pixels per frame.
    // Negative = going left, positive = going right, 0 = straight.
    // Holding A/D changes it a little every frame, so turning starts soft and gets stronger.
    private double steerSpeed;

    public PlayerCar() {
        // "super(...)" calls the constructor of the parent class (GameObject).
        // imagePath is null for now -> red placeholder box.
        // When the art is ready, change null to e.g. "Resorces/Textures/player.png".
        super(0, GameConfig.PLAYER_Y, GameConfig.CAR_WIDTH, GameConfig.CAR_HEIGHT, Color.RED, "Resorces/Textures/f1_game_playerCar/f1.png");

        x = Road.laneCenterX(1) - width / 2.0; // start in the middle of the second lane
        speed = 0;
        steerSpeed = 0;
    }

    /**
     * Called once every frame. Reads the keyboard and moves the car.
     */
    public void update(KeyHandler keys) {
        updateSpeed(keys);
        updateSteering(keys);
    }

    private void updateSpeed(KeyHandler keys) {
        if (keys.isUpPressed()) {
            speed += GameConfig.ACCELERATION;
        } else if (keys.isDownPressed()) {
            speed -= GameConfig.BRAKE_POWER;
        } else {
            speed -= GameConfig.COAST_SLOWDOWN; // no key -> the car slowly rolls out
        }

        // Keep the speed between 0 and MAX_SPEED.
        if (speed < 0) {
            speed = 0;
        }
        if (speed > GameConfig.MAX_SPEED) {
            speed = GameConfig.MAX_SPEED;
        }
    }

    /**
     * Analog-style steering, the same idea as updateSpeed but sideways:
     *   - hold A -> steerSpeed goes down a bit every frame (more and more to the left)
     *   - hold D -> steerSpeed goes up a bit every frame (more and more to the right)
     *   - nothing (or both) -> steerSpeed goes back to 0, the car straightens out
     */
    private void updateSteering(KeyHandler keys) {
        boolean left = keys.isLeftPressed();
        boolean right = keys.isRightPressed();

        if (left && !right) {
            steerSpeed -= GameConfig.STEER_ACCELERATION;
        } else if (right && !left) {
            steerSpeed += GameConfig.STEER_ACCELERATION;
        } else {
            // Let go: move steerSpeed towards 0, but don't shoot past 0 to the other side.
            if (steerSpeed > 0) {
                steerSpeed = Math.max(0, steerSpeed - GameConfig.STEER_RETURN);
            } else {
                steerSpeed = Math.min(0, steerSpeed + GameConfig.STEER_RETURN);
            }
        }

        // Never turn faster than MAX_STEER_SPEED (in both directions).
        steerSpeed = Math.max(-GameConfig.MAX_STEER_SPEED, Math.min(steerSpeed, GameConfig.MAX_STEER_SPEED));

        // Actually move the car.
        x += steerSpeed;

        // Stay on the asphalt. If we hit the edge, stop the sideways movement too,
        // otherwise the car would "stick" to the edge for a moment when you steer back.
        double leftEdge = GameConfig.ROAD_X;
        double rightEdge = GameConfig.ROAD_X + GameConfig.ROAD_WIDTH - width;
        if (x < leftEdge) {
            x = leftEdge;
            steerSpeed = 0;
        }
        if (x > rightEdge) {
            x = rightEdge;
            steerSpeed = 0;
        }
    }

    /**
     * A slightly BIGGER invisible box around the car (from the plan).
     * Traffic touching this box but NOT the car = near miss -> bonus points.
     */
    public Rectangle getNearMissBounds() {
        int margin = GameConfig.NEAR_MISS_MARGIN;
        return new Rectangle((int) x - margin, (int) y - margin, width + 2 * margin, height + 2 * margin);
    }

    public double getSpeed() {
        return speed;
    }
}
