package f1_game.base;

/**
 * ScreenFit = how the WINDOW_WIDTH x WINDOW_HEIGHT game fits into a panel of any size (fullscreen).
 *
 * The game gets bigger by "scale" so it fills the height, and moves to the middle by
 * "offsetX"/"offsetY". The extra space on the sides gets the background.
 * Both the road thread (RoadRenderer) and paintComponent (GamePanel) need these numbers,
 * so they live in one place and the road and the cars always line up.
 *
 * "record" = a small class that only holds values (Java makes the constructor and getters).
 */
public record ScreenFit(double scale, double offsetX, double offsetY) {

    public static ScreenFit of(int panelWidth, int panelHeight) {
        double scale = Math.min((double) panelWidth / GameConfig.WINDOW_WIDTH,
                                (double) panelHeight / GameConfig.WINDOW_HEIGHT);
        double offsetX = (panelWidth - GameConfig.WINDOW_WIDTH * scale) / 2;
        double offsetY = (panelHeight - GameConfig.WINDOW_HEIGHT * scale) / 2;
        return new ScreenFit(scale, offsetX, offsetY);
    }

    /** Screen x where the road starts. */
    public int roadLeftX() {
        return (int) Math.round(offsetX + GameConfig.ROAD_X * scale);
    }

    /** Screen x where the road ends. */
    public int roadRightX() {
        return (int) Math.round(offsetX + (GameConfig.ROAD_X + GameConfig.ROAD_WIDTH) * scale);
    }

    /** How big one background picture is on the screen. */
    public int tileSize() {
        return Math.max(1, (int) Math.round(GameConfig.BACKGROUND_TILE_SIZE * scale));
    }
}
