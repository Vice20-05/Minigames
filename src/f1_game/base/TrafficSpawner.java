package f1_game.base;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Random;

/**
 * TrafficSpawner = decides WHEN and WHERE a new traffic car appears.
 *
 * Rules (from the plan):
 *   - A new car is tried every SPAWN_EVERY_TICKS frames.
 *   - It goes into a random lane.
 *   - We NEVER block every lane, so there is always a way through.
 *
 * This is also the place where you will add the difficulty later
 * (more cars every 30 seconds, roadblocks after 2 minutes...).
 */
public class TrafficSpawner {

    // Some colors so the traffic cars don't all look the same.
    private static final Color[] CAR_COLORS = {
        Color.BLUE, Color.YELLOW, Color.ORANGE, Color.CYAN, Color.MAGENTA, Color.LIGHT_GRAY
    };

    // All the pictures from the traffic folder, loaded ONCE when the game starts.
    // Every new car gets a random one. Add a .png to the folder = a new car type.
    private final List<BufferedImage> carImages = ImageLoader.loadFolder("Resorces/Textures/traffic");

    private final Random random = new Random();
    private int ticksUntilNextSpawn = GameConfig.SPAWN_EVERY_TICKS;

    /**
     * Called once every frame. Counts down, and when it reaches 0 it tries to add a car.
     * @param traffic the list of cars on the road (we add the new car into it)
     */
    public void update(List<TrafficCar> traffic) {
        ticksUntilNextSpawn--;
        if (ticksUntilNextSpawn > 0) {
            return; // not time yet
        }
        ticksUntilNextSpawn = GameConfig.SPAWN_EVERY_TICKS;

        int lane = random.nextInt(GameConfig.LANE_COUNT); // 0, 1, 2 or 3

        // Is that lane already busy near the top? Then skip, so cars don't overlap.
        if (isLaneBusyAtTop(traffic, lane)) {
            return;
        }

        // Would this car close the LAST free lane? Then skip it.
        if (countBusyLanesAtTop(traffic) >= GameConfig.LANE_COUNT - 1) {
            return;
        }

        // Random speed between TRAFFIC_MIN_SPEED and TRAFFIC_MAX_SPEED.
        double speed = GameConfig.TRAFFIC_MIN_SPEED
                + random.nextDouble() * (GameConfig.TRAFFIC_MAX_SPEED - GameConfig.TRAFFIC_MIN_SPEED);
        Color color = CAR_COLORS[random.nextInt(CAR_COLORS.length)];

        // Pick a random picture for the new car.
        BufferedImage image = null; // null = no pictures found, use the color box
        if (!carImages.isEmpty()) {
            // nextInt(6) gives 0..5, so any index in the list. With more pictures
            // in the folder the list is longer and this still works.
            image = carImages.get(random.nextInt(carImages.size()));
        }

        traffic.add(new TrafficCar(lane, speed, color, image));
    }

    /**
     * Called once every frame (from GamePanel). After LANE_CHANGE_SCORE points,
     * cars sometimes switch to the lane next to them, so you can't just sit in one spot.
     *
     * A car only changes lanes if:
     *   - it hasn't changed lanes before (once per car, no zig-zag)
     *   - it is on screen and still far enough IN FRONT of the player (so you can react)
     *   - the lane next to it is free (no other car close to it there)
     */
    public void updateLaneChanges(List<TrafficCar> traffic, double score) {
        if (score < GameConfig.LANE_CHANGE_SCORE) {
            return; // too early in the game
        }

        for (TrafficCar car : traffic) {
            if (car.hasChangedLane()) {
                continue; // this car already did its lane change
            }

            // Only cars between the top of the screen and a bit above the player.
            boolean onScreen = car.getY() > 0;
            boolean farEnoughAhead = car.getY() < GameConfig.PLAYER_Y - GameConfig.CAR_HEIGHT * 2;
            if (!onScreen || !farEnoughAhead) {
                continue;
            }

            // Roll the dice: most frames nothing happens.
            if (random.nextDouble() > GameConfig.LANE_CHANGE_CHANCE) {
                continue;
            }

            // Pick the lane to the left or to the right (randomly).
            int newLane = car.getLane() + (random.nextBoolean() ? 1 : -1);
            // That lane doesn't exist (we are at the edge)? Then take the other side.
            if (newLane < 0 || newLane >= GameConfig.LANE_COUNT) {
                newLane = car.getLane() - (newLane - car.getLane());
            }
            if (newLane < 0 || newLane >= GameConfig.LANE_COUNT) {
                continue; // a road with only 1 lane: nowhere to go
            }

            if (isLaneFreeNear(traffic, car, newLane)) {
                car.changeLane(newLane);
            }
        }
    }

    /** True if no OTHER car in that lane is close to this car (above or below it). */
    private boolean isLaneFreeNear(List<TrafficCar> traffic, TrafficCar car, int lane) {
        for (TrafficCar other : traffic) {
            if (other == car) {
                continue; // don't compare the car with itself
            }
            boolean sameLane = other.getLane() == lane;
            boolean close = Math.abs(other.getY() - car.getY()) < GameConfig.CAR_HEIGHT * 2.5;
            if (sameLane && close) {
                return false;
            }
        }
        return true;
    }

    /** "Top" = the area from above the screen down to 3 car-lengths. */
    private boolean isLaneBusyAtTop(List<TrafficCar> traffic, int lane) {
        int laneLeft = GameConfig.ROAD_X + lane * GameConfig.LANE_WIDTH;
        int laneRight = laneLeft + GameConfig.LANE_WIDTH;

        for (TrafficCar car : traffic) {
            boolean inThisLane = car.getX() >= laneLeft && car.getX() < laneRight;
            boolean nearTop = car.getY() < GameConfig.CAR_HEIGHT * 3;
            if (inThisLane && nearTop) {
                return true;
            }
        }
        return false;
    }

    private int countBusyLanesAtTop(List<TrafficCar> traffic) {
        int busy = 0;
        for (int lane = 0; lane < GameConfig.LANE_COUNT; lane++) {
            if (isLaneBusyAtTop(traffic, lane)) {
                busy++;
            }
        }
        return busy;
    }
}
