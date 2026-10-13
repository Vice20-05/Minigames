package f1_game.base;

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
    public static final int TICK_MS = 1000 / FPS;  // milliseconds between two frames (~16 ms)

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

    // Lane changes: once your score reaches LANE_CHANGE_SCORE, traffic cars sometimes switch lanes.
    public static final double LANE_CHANGE_SCORE = 500;   // score where lane changes start
    public static final double LANE_CHANGE_CHANCE = 0.06; // chance per car per frame (0.01 = 1%)
    public static final double LANE_CHANGE_SPEED = 1.0;   // pixels per frame sideways (smaller = slower, easier to dodge)

    // ---------------- Score ----------------
    public static final double POINTS_PER_SECOND = 10;  // base points while driving
    public static final double MAX_MULTIPLIER = 4.0;    // the speed bonus never goes above this
    public static final double NEAR_MISS_BONUS = 100;    // points for a near-miss (player car and traffic car almost touch)
    public static final int NEAR_MISS_POPUP_TICKS = 60; // how long "NEAR MISS!" stays on screen (60 = 1 second)
    public static final int NEAR_MISS_MARGIN = 24;      // how many pixels around your car count as "near" (bigger = easier)

    // Private constructor: nobody should do "new GameConfig()",
    // this class is only a box of constants.
    private GameConfig() {
    }
}
