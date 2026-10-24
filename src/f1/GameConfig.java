package f1;

/**
 * GameConfig = one place for ALL the numbers of the game.
 *
 * Instead of writing "480" or "300" all over the code, we give every number
 * a name here. If you want to change something (window size, top speed,
 * how many lanes...), you only change it HERE and the whole game follows.
 *
 * "public static final" means:
 *   public  -> every other class can read it
 *   static  -> it belongs to the class, so you write GameConfig.WINDOW_WIDTH
 *   final   -> it is a constant, it can never change while the game runs
 */
public class GameConfig {

    // ---------------- Window ----------------
    public static final int WINDOW_WIDTH = 480;   // pixels , and keep these two like so
    //the width and height are gonna be centered , so the game is in the middle

    public static final int WINDOW_HEIGHT = 720;  // pixels (tall, because the road goes up)

    // ---------------- Game loop ----------------
    public static final int FPS = 60;              // frames (updates) per second
    public static final long TICK_NANOS = 1_000_000_000L / FPS; // nanoseconds between two frames (~16.7 ms)

    // ---------------- Road ----------------
    public static final int LANE_COUNT = 2;
    public static final int ROAD_X = 40;                         // where the asphalt starts (left edge)
    public static final int ROAD_WIDTH = 400;                    // how wide the asphalt is
    public static final int LANE_WIDTH = ROAD_WIDTH / LANE_COUNT; // = 100 pixels per lane

    // ---------------- Background ----------------
    // Size of one background picture next to the road (in game pixels, it grows in fullscreen).
    // The grass strip next to the road is ROAD_X (= 40) wide, so 60 is a bit bigger than it
    // and the picture gets cut by the window edge. Make it bigger or smaller to taste.
    public static final int BACKGROUND_TILE_SIZE = 60;

    // ---------------- Cars ----------------
    public static final int CAR_WIDTH = 50;
    public static final int CAR_HEIGHT = 90;
    public static final int PLAYER_Y = WINDOW_HEIGHT - 150;      // the player stays at this height

    // ---------------- Player speed (km/h) ----------------
    public static final double MAX_SPEED = 300;    // top speed
    public static final double ACCELERATION = 0.30; // km/h gained per frame while holding W
    public static final double BRAKE_POWER = 3.0;  // km/h lost per frame while holding S
    public static final double COAST_SLOWDOWN = 0.2; // km/h lost per frame when pressing nothing

    /**
     * Converts speed into movement on the screen.
     * Example: 100 km/h * 0.08 = 8 pixels per frame.
     * Make it bigger and the game feels faster.
     */
    public static final double PIXELS_PER_KMH = 0.08;

    // ---------------- Steering (A / D) ----------------
    // All in pixels per frame. Hold A/D: the sideways speed grows by STEER_ACCELERATION
    // every frame until MAX_STEER_SPEED. Let go: it shrinks by STEER_RETURN every frame.
    public static final double STEER_ACCELERATION = 0.8; // bigger = turns in faster
    public static final double MAX_STEER_SPEED = 6.0;    // top sideways speed
    public static final double STEER_RETURN = 0.9;       // bigger = straightens out faster

    // ---------------- Traffic ----------------
    public static final double TRAFFIC_MIN_SPEED = 50;  // km/h
    public static final double TRAFFIC_MAX_SPEED = 100; // km/h
    public static final int SPAWN_EVERY_TICKS = 25;     // try to spawn a car every 45 frames (~0.75 s)
    // A faster car behind a slower one brakes to the slower car's speed, so they never overlap.
    public static final double TRAFFIC_FOLLOW_DISTANCE = CAR_HEIGHT / 2.0; // pixels of space it keeps to the car in front
    public static final double TRAFFIC_ACCELERATION = 0.2; // km/h per frame it speeds back up when the road is free

    // Lane changes: cars that appear after LANE_CHANGE_SCORE points can switch lanes.
    // Every new car rolls the dice ONCE: only some of them become lane-changers,
    // and each one switches at its own random spot on the screen (not all at the same time).
    // The chance GROWS with the score: rare at 500 points, a bit more every 150 points, until the max.
    //   500 -> 8%   650 -> 12%   800 -> 16%   ...   ~2600 and up -> 35% (so most cars still drive straight)
    public static final double LANE_CHANGE_SCORE = 500;          // score where lane changes start
    public static final double LANE_CHANGE_START_CHANCE = 0.08;  // chance at LANE_CHANGE_SCORE (0.08 = 8% of new cars)
    public static final double LANE_CHANGE_STEP_SCORE = 150;     // every this many points...
    public static final double LANE_CHANGE_STEP_CHANCE = 0.04;   // ...the chance goes up by this much (+4%)
    public static final double LANE_CHANGE_MAX_CHANCE = 0.35;    // the chance never goes above this
    public static final double LANE_CHANGE_SPEED = 1.60;   // pixels per frame sideways (smaller = slower, easier to dodge)

    // Braking: a car starts slowing down this far (pixels) behind something slower in its lane,
    // and reaches its speed at TRAFFIC_FOLLOW_DISTANCE. Bigger = brakes earlier and softer.
    public static final double TRAFFIC_BRAKE_DISTANCE = CAR_HEIGHT * 3;

    // ---------------- Roadblocks (work zones) ----------------
    // From ROADBLOCK_SCORE on, now and then one lane gets a roadblock with a pothole behind it.
    // Traffic in that lane brakes and stops in a queue, the other lane keeps driving.
    // Hitting the roadblock or the pothole = game over.
    public static final double ROADBLOCK_SCORE = 1000;           // score where roadblocks start
    public static final int ROADBLOCK_MIN_WAIT_TICKS = 4 * FPS;  // wait at least 4 s between two roadblocks...
    public static final int ROADBLOCK_MAX_WAIT_TICKS = 10 * FPS; // ...and at most 10 s (random in between)
    public static final int ROADBLOCK_WIDTH = LANE_WIDTH - 30;   // the barrier almost fills its lane
    public static final int POTHOLE_WIDTH = 70;
    public static final int POTHOLE_GAP = 25;                    // pixels between the barrier and the pothole behind it

    // While a roadblock is on the road, new cars in the OTHER lane drive a bit faster, so you have
    // to time your lane switch. Nothing extra below 2000 points, then up to +10 km/h more for
    // every 1000 points (random, so not every car is the same), but never more than the max.
    public static final double OPEN_LANE_BONUS_PER_1000 = 10;    // km/h
    public static final double OPEN_LANE_MAX_BONUS = 40;         // km/h

    // Getting past a roadblock gives a small speed boost.
    public static final double ROADBLOCK_PASS_BOOST = 15;        // km/h
    public static final int POPUP_TICKS = 60;                    // how long a popup text stays (60 = 1 second)

    // ---------------- Score ----------------
    public static final double POINTS_PER_SECOND = 10;  // base points while driving
    public static final double MAX_MULTIPLIER = 4.0;    // the speed bonus never goes above this
    public static final double NEAR_MISS_BONUS = 100;    // points for a near-miss (player car and traffic car almost touch)
    public static final double ROADBLOCK_CLEAR_BONUS = 100; // points for getting past a whole work zone
    public static final int NEAR_MISS_POPUP_TICKS = 60; // how long "NEAR MISS!" stays on screen (60 = 1 second)
    public static final int NEAR_MISS_MARGIN = 24;      // how many pixels around your car count as "near" (bigger = easier)

    // ---------------- Online leaderboard ----------------
    public static final int NAME_MAX_LENGTH = 12;       // letters/numbers in the name on the game over screen

    // Private constructor: nobody should do "new GameConfig()",
    // this class is only a box of constants.
    private GameConfig() {
    }
}
