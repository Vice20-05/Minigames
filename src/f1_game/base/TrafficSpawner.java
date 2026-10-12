package f1_game.base;

import java.awt.Color;
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

        traffic.add(new TrafficCar(lane, speed, color));
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
