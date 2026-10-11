package f1_game.base;

import java.awt.Color;
import java.awt.Rectangle;

/**
 * PlayerCar = the car YOU drive.
 *
 * - It stays near the bottom of the screen (only x changes, y stays the same).
 * - W / Up arrow    -> accelerate
 * - S / Down arrow  -> brake
 * - A / Left arrow  -> move one lane to the left
 * - D / Right arrow -> move one lane to the right
 */
public class PlayerCar extends GameObject {

    private int lane;          // which lane we are in (0 = leftmost)
    private double speed;      // current speed in km/h

    // We remember if left/right was already held down last frame.
    // Without this, holding A for 1 second would jump 60 lanes (one per frame)!
    private boolean leftWasPressed = false;
    private boolean rightWasPressed = false;

    public PlayerCar() {
        // "super(...)" calls the constructor of the parent class (GameObject).
        // imagePath is null for now -> red placeholder box.
        // When the art is ready, change null to e.g. "Resorces/Textures/player.png".
        super(0, GameConfig.PLAYER_Y, GameConfig.CAR_WIDTH, GameConfig.CAR_HEIGHT, Color.RED, null);

        lane = 1; // start in the second lane
        x = targetX();  // put the car exactly in its lane
        speed = 0;
    }

    /**
     * Called once every frame. Reads the keyboard and moves the car.
     */
    public void update(KeyHandler keys) {
        updateSpeed(keys);
        updateLane(keys);
        slideTowardsLane();
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

    private void updateLane(KeyHandler keys) {
        // "&& !leftWasPressed" means: only react the FIRST frame the key goes down.
        if (keys.isLeftPressed() && !leftWasPressed && lane > 0) {
            lane--;
        }
        if (keys.isRightPressed() && !rightWasPressed && lane < GameConfig.LANE_COUNT - 1) {
            lane++;
        }
        leftWasPressed = keys.isLeftPressed();
        rightWasPressed = keys.isRightPressed();
    }

    /**
     * Instead of teleporting to the new lane, the car moves 25% of the
     * remaining distance every frame. This makes a smooth slide.
     */
    private void slideTowardsLane() {
        x += (targetX() - x) * 0.25;
    }

    /** The x position the car wants to reach (centered in its lane). */
    private double targetX() {
        return Road.laneCenterX(lane) - width / 2.0;
    }

    /**
     * A slightly BIGGER invisible box around the car (from the plan).
     * Traffic touching this box but NOT the car = near miss -> bonus points.
     * Not used yet, it is ready for you (see Victor.txt, "Next steps").
     */
    public Rectangle getNearMissBounds() {
        int margin = 15;
        return new Rectangle((int) x - margin, (int) y - margin, width + 2 * margin, height + 2 * margin);
    }

    public double getSpeed() {
        return speed;
    }
}
