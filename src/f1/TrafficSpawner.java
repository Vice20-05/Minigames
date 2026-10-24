package f1;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
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

    public static final String TRAFFIC_FOLDER = "resources/textures/f1/traffic";

    // All the pictures from the traffic folder. GamePanel loads them ONCE on a background
    // thread when the game opens, so here they come from ImageLoader's memory (instant).
    // Every new car gets a random one. Add a .png to the folder = a new car type.
    private final List<BufferedImage> carImages = ImageLoader.loadFolder(TRAFFIC_FOLDER);

    private final Random random = new Random();
    private int ticksUntilNextSpawn = GameConfig.SPAWN_EVERY_TICKS;

    /**
     * Called once every frame. Counts down, and when it reaches 0 it tries to add a car.
     * @param traffic   the list of cars on the road (we add the new car into it)
     * @param obstacles the roadblocks/potholes (a car never spawns on top of one)
     * @param score     the player's score right now (lane changes start at LANE_CHANGE_SCORE)
     */
    public void update(List<TrafficCar> traffic, List<Obstacle> obstacles, double score) {
        ticksUntilNextSpawn--;
        if (ticksUntilNextSpawn > 0) {
            return; // not time yet
        }
        ticksUntilNextSpawn = GameConfig.SPAWN_EVERY_TICKS;

        int lane = random.nextInt(GameConfig.LANE_COUNT); // 0, 1, 2 or 3

        // Is that lane already busy near the top? Then skip, so cars don't overlap.
        if (isLaneBusyAtTop(traffic, obstacles, lane)) {
            return;
        }

        // Would this car close the LAST free lane? Then skip it.
        if (countBusyLanesAtTop(traffic, obstacles) >= GameConfig.LANE_COUNT - 1) {
            return;
        }

        // Random speed between TRAFFIC_MIN_SPEED and TRAFFIC_MAX_SPEED.
        double speed = GameConfig.TRAFFIC_MIN_SPEED
                + random.nextDouble() * (GameConfig.TRAFFIC_MAX_SPEED - GameConfig.TRAFFIC_MIN_SPEED);

        // A roadblock in the OTHER lane? Then this car is in the open lane: make it a bit faster.
        int blockedLane = RoadblockSpawner.blockedLane(obstacles);
        if (blockedLane >= 0 && lane != blockedLane) {
            speed += openLaneBonus(score);
        }

        Color color = CAR_COLORS[random.nextInt(CAR_COLORS.length)];

        // Pick a random picture for the new car.
        BufferedImage image = null; // null = no pictures found, use the color box
        if (!carImages.isEmpty()) {
            // nextInt(6) gives 0..5, so any index in the list. With more pictures
            // in the folder the list is longer and this still works.
            image = carImages.get(random.nextInt(carImages.size()));
        }

        TrafficCar car = new TrafficCar(lane, speed, color, image);

        // After LANE_CHANGE_SCORE points, roll the dice ONCE for this car:
        // will it switch lanes, and if yes, WHERE on the screen?
        // Only new cars get this, so the lane changes start a few at a time
        // instead of every car on the road switching at once when you hit the score.
        if (score >= GameConfig.LANE_CHANGE_SCORE && random.nextDouble() < laneChangeChance(score)) {
            // A random height between the top of the screen and LANE_CHANGE_LAST_Y,
            // so every lane-changer moves over at a different spot.
            double spot = random.nextDouble() * LANE_CHANGE_LAST_Y;
            car.planLaneChange(spot);
        }

        traffic.add(car);
    }

    /**
     * Extra km/h for a car in the open lane next to a roadblock.
     * 1000-1999 points: nothing. Then up to +OPEN_LANE_BONUS_PER_1000 for every 1000 points
     * (2000 -> up to +10, 3000 -> up to +20 ...), capped at OPEN_LANE_MAX_BONUS.
     * Random between half and the full bonus, so the timing is a little different every time.
     */
    private double openLaneBonus(double score) {
        int steps = (int) ((score - GameConfig.ROADBLOCK_SCORE) / 1000);
        if (steps <= 0) {
            return 0;
        }
        double maxBonus = Math.min(steps * GameConfig.OPEN_LANE_BONUS_PER_1000, GameConfig.OPEN_LANE_MAX_BONUS);
        return maxBonus * (0.5 + 0.5 * random.nextDouble());
    }

    /**
     * How likely a NEW car is to become a lane-changer at this score.
     * Starts small at LANE_CHANGE_SCORE and goes up one step every LANE_CHANGE_STEP_SCORE points,
     * but never above LANE_CHANGE_MAX_CHANCE, so there are always cars that don't switch.
     * Example: score 830 -> (830 - 500) / 150 = 2 full steps -> 0.08 + 2 * 0.04 = 0.16 (16%).
     */
    private double laneChangeChance(double score) {
        // (int) cuts off the decimals: 2.2 steps -> 2, so the chance goes up in jumps, not smoothly
        int steps = (int) ((score - GameConfig.LANE_CHANGE_SCORE) / GameConfig.LANE_CHANGE_STEP_SCORE);
        double chance = GameConfig.LANE_CHANGE_START_CHANCE + steps * GameConfig.LANE_CHANGE_STEP_CHANCE;
        return Math.min(chance, GameConfig.LANE_CHANGE_MAX_CHANCE);
    }

    // The lowest point where a car may START a lane change: 2 car-lengths above
    // the player, so you always have time to react.
    private static final double LANE_CHANGE_LAST_Y = GameConfig.PLAYER_Y - GameConfig.CAR_HEIGHT * 2;

    /**
     * Called once every frame (from GamePanel). Cars that were picked to switch lanes
     * (see update) move to the lane next to them when they reach their spot.
     *
     * A car only changes lanes if:
     *   - it was picked as a lane-changer and reached its spot on the screen
     *   - it hasn't changed lanes before (once per car, no zig-zag)
     *   - it is still far enough IN FRONT of the player (so you can react)
     *   - the lane next to it is free (no other car or roadblock close to it there)
     * If the lane is busy, it tries again next frame (until it gets too close to you).
     */
    public void updateLaneChanges(List<TrafficCar> traffic, List<Obstacle> obstacles) {
        for (TrafficCar car : traffic) {
            if (car.hasChangedLane() || !car.wantsToChangeLane()) {
                continue; // not a lane-changer, already done, or not at its spot yet
            }

            if (car.getY() > LANE_CHANGE_LAST_Y) {
                continue; // too close to the player now, it stays in its lane
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

            if (isLaneFreeNear(traffic, obstacles, car, newLane)) {
                car.changeLane(newLane);
            }
        }
    }

    /**
     * Called once every frame (from GamePanel), after the cars moved.
     * Every car looks at what is in front of it (a car OR a roadblock) and brakes if it
     * gets too close, so it never drives into (through) it. Behind a roadblock the cars
     * stop one after the other and make a queue, each keeping its distance.
     */
    public void keepDistance(List<TrafficCar> traffic, List<Obstacle> obstacles) {
        // Handle the cars from the TOP of the screen down (front car first).
        // Then in a line of 3 cars, the middle one is already fixed before the last one looks at it.
        List<TrafficCar> frontToBack = new ArrayList<>(traffic);
        frontToBack.sort(Comparator.comparingDouble(TrafficCar::getY));

        for (TrafficCar car : frontToBack) {
            TrafficCar carAhead = findAhead(traffic, car);
            Obstacle obstacleAhead = findAhead(obstacles, car);

            // Both in front of us? Follow the CLOSER one (its back edge is lower = bigger y).
            if (obstacleAhead != null
                    && (carAhead == null || obstacleAhead.getBottomY() > carAhead.getBottomY())) {
                car.followAhead(obstacleAhead.getBottomY(), 0); // a roadblock has speed 0 -> we stop
            } else if (carAhead != null) {
                car.followAhead(carAhead.getBottomY(), carAhead.getSpeed());
            } else {
                car.driveFreely();
            }
        }
    }

    /**
     * The closest thing IN FRONT of this car (smaller y) that is in its way, or null if there is none.
     * "In its way" = the two overlap sideways. We check pixels instead of lane numbers,
     * so a car that is halfway through a lane change counts too.
     * "<T extends GameObject>" = works for a list of TrafficCars AND a list of Obstacles.
     */
    private <T extends GameObject> T findAhead(List<T> things, TrafficCar car) {
        T closest = null;
        for (T other : things) {
            if (other == car || other.getY() >= car.getY()) {
                continue; // itself, or something behind us
            }
            boolean sideBySide = other.getX() < car.getX() + car.getWidth()
                              && car.getX() < other.getX() + other.getWidth();
            // A roadblock also counts when it's in the lane we are MOVING INTO (getLane() is the
            // new lane during a lane change), so we brake in time, not only once we're in front of it.
            boolean inOurNewLane = other instanceof Obstacle obstacle && obstacle.getLane() == car.getLane();
            if ((sideBySide || inOurNewLane) && (closest == null || other.getY() > closest.getY())) {
                closest = other; // the lowest one above us = the one right in front
            }
        }
        return closest;
    }

    /** True if no OTHER car and no roadblock/pothole in that lane is close to this car. */
    private boolean isLaneFreeNear(List<TrafficCar> traffic, List<Obstacle> obstacles, TrafficCar car, int lane) {
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
        for (Obstacle obstacle : obstacles) {
            // Never move into a lane with a roadblock AHEAD of us (anywhere up the road):
            // sliding over takes a while, and the roadblock comes closer the whole time.
            // Also not when it is right next to us or just behind us.
            boolean aheadOrNextToUs = obstacle.getY() < car.getY() + GameConfig.CAR_HEIGHT * 2.5;
            if (obstacle.getLane() == lane && aheadOrNextToUs) {
                return false;
            }
        }
        return true;
    }

    /**
     * "Top" = the area from above the screen down to 3 car-lengths.
     * True if a car or an obstacle is there. "static" so the RoadblockSpawner can use it too.
     */
    static boolean isLaneBusyAtTop(List<TrafficCar> traffic, List<Obstacle> obstacles, int lane) {
        return isAnyNearTop(traffic, lane) || isAnyNearTop(obstacles, lane);
    }

    private static boolean isAnyNearTop(List<? extends GameObject> things, int lane) {
        int laneLeft = GameConfig.ROAD_X + lane * GameConfig.LANE_WIDTH;
        int laneRight = laneLeft + GameConfig.LANE_WIDTH;

        for (GameObject thing : things) {
            boolean inThisLane = thing.getX() >= laneLeft && thing.getX() < laneRight;
            // A car that is still sliding INTO this lane counts too
            if (thing instanceof TrafficCar car && car.getLane() == lane) {
                inThisLane = true;
            }
            boolean nearTop = thing.getY() < GameConfig.CAR_HEIGHT * 3;
            if (inThisLane && nearTop) {
                return true;
            }
        }
        return false;
    }

    private int countBusyLanesAtTop(List<TrafficCar> traffic, List<Obstacle> obstacles) {
        int busy = 0;
        for (int lane = 0; lane < GameConfig.LANE_COUNT; lane++) {
            if (isLaneBusyAtTop(traffic, obstacles, lane)) {
                busy++;
            }
        }
        return busy;
    }
}
