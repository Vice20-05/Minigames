package f1;

import battletanks.ImageLoader;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Random;

/**
 * RoadblockSpawner = decides WHEN and in WHICH lane a work zone (roadblock + pothole) appears.
 *
 * Rules:
 *   - Only from ROADBLOCK_SCORE (1000) points on.
 *   - Only ONE work zone on the road at a time, and only in ONE lane, so the other lane
 *     is always open.
 *   - A random wait (ROADBLOCK_MIN/MAX_WAIT_TICKS) between two work zones.
 *   - Only if the top of that lane is free, so it never appears on top of a car.
 */
public class RoadblockSpawner {

    public static final String ROADBLOCK_PATH = "resources/textures/f1/Roadblock/tribune_overhang_striped.png";
    // Put the pothole picture here with exactly this name. Until then a dark hole is drawn.
    public static final String POTHOLE_PATH = "resources/textures/f1/Roadblock/pothole.png";

    // Loaded once by GamePanel on the background thread, here they come from memory.
    private final BufferedImage roadblockImage = ImageLoader.load(ROADBLOCK_PATH);
    private final BufferedImage potholeImage = ImageLoader.load(POTHOLE_PATH);

    private final Random random = new Random();
    private int ticksUntilNextRoadblock = randomWait();

    /**
     * Called once every frame. Counts down, and when it reaches 0 it adds a work zone.
     * @param obstacles the roadblocks/potholes on the road (we add the new ones into it)
     * @param traffic   the cars, to check that the lane is free at the top
     * @param score     the player's score right now
     */
    public void update(List<Obstacle> obstacles, List<TrafficCar> traffic, double score) {
        if (score < GameConfig.ROADBLOCK_SCORE) {
            return; // too early in the game
        }
        if (!obstacles.isEmpty()) {
            return; // there is already a work zone on the road, wait until it's gone
        }
        ticksUntilNextRoadblock--;
        if (ticksUntilNextRoadblock > 0) {
            return; // not time yet
        }

        int lane = random.nextInt(GameConfig.LANE_COUNT); // left or right
        if (TrafficSpawner.isLaneBusyAtTop(traffic, obstacles, lane)) {
            return; // a car is there, try again next frame
        }

        Obstacle roadblock = Obstacle.roadblock(lane, roadblockImage);
        obstacles.add(roadblock);
        obstacles.add(Obstacle.pothole(lane, roadblock, potholeImage));
        ticksUntilNextRoadblock = randomWait();
    }

    /** The lane that has a roadblock right now, or -1 if the road is clear. */
    public static int blockedLane(List<Obstacle> obstacles) {
        for (Obstacle obstacle : obstacles) {
            if (obstacle.isRoadblock()) {
                return obstacle.getLane();
            }
        }
        return -1;
    }

    /** A random number of frames between the MIN and MAX wait. */
    private int randomWait() {
        return GameConfig.ROADBLOCK_MIN_WAIT_TICKS
                + random.nextInt(GameConfig.ROADBLOCK_MAX_WAIT_TICKS - GameConfig.ROADBLOCK_MIN_WAIT_TICKS + 1);
    }
}
