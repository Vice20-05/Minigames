package f1_game.base;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Road = draws the grass, the asphalt and the white lane lines.
 *
 * THE SCROLLING TRICK:
 * The player's car never really moves up the screen. Instead, the white
 * lane lines move DOWN. Your brain sees lines moving down and thinks
 * "my car is driving forward". The faster the car, the faster the lines move.
 */
public class Road {

    private static final int DASH_LENGTH = 40; // length of one white dash
    private static final int DASH_GAP = 40;    // empty space between dashes
    private static final int DASH_PERIOD = DASH_LENGTH + DASH_GAP;

    private  final BufferedImage backgroundImage;
    private final BufferedImage roadImage;

    // How far the lines have moved. It always stays between 0 and DASH_PERIOD,
    // because after one full dash+gap the picture looks exactly the same again.
    private double scrollOffset = 0;

    private  double roadImageOffset=0;

    public static final String BACKGROUND_PATH = "Resorces/Textures/f1game_grass_background/background.png";
    // road.png doesn't exist yet -> a gray road is drawn until you add it
    public static final String ROAD_PATH = "Resorces/Textures/f1game_grass_background/road.png";

    public Road(){
        backgroundImage=ImageLoader.load(BACKGROUND_PATH);
        roadImage=ImageLoader.load(ROAD_PATH);
    }

    /**
     * Moves the lane lines down.
     * @param pixels how many pixels to move this frame (bigger = faster)
     */
    public void update(double pixels) {
        scrollOffset = (scrollOffset + pixels) % DASH_PERIOD;

        if (roadImage != null) {
            roadImageOffset = (roadImageOffset + pixels) % roadImage.getHeight();
        }
    }

    

    /**
     * Draws the road in GAME coordinates (0..WINDOW_WIDTH, 0..WINDOW_HEIGHT).
     * The background picture is NOT drawn here, see drawBackground(...).
     */
    public void draw(Graphics2D g) {
        // 1) Asphalt - DOES scroll
        if (roadImage != null) {
            drawScrollingRoadImage(g);
        } else {
            g.setColor(new Color(60, 60, 60));
            g.fillRect(GameConfig.ROAD_X, 0, GameConfig.ROAD_WIDTH, GameConfig.WINDOW_HEIGHT);
        }

        // 2) Solid white lines on the left and right edge of the road
        g.setColor(Color.WHITE);
        g.fillRect(GameConfig.ROAD_X, 0, 4, GameConfig.WINDOW_HEIGHT);
        g.fillRect(GameConfig.ROAD_X + GameConfig.ROAD_WIDTH - 4, 0, 4, GameConfig.WINDOW_HEIGHT);

        // 3) Dashed lines between the lanes (these scroll too)
        for (int lane = 1; lane < GameConfig.LANE_COUNT; lane++) {
            int lineX = GameConfig.ROAD_X + lane * GameConfig.LANE_WIDTH - 2;

            // Start a bit above the screen so there is never an empty gap at the top.
            for (int y = -DASH_PERIOD; y < GameConfig.WINDOW_HEIGHT; y += DASH_PERIOD) {
                g.fillRect(lineX, y + (int) scrollOffset, 4, DASH_LENGTH);
            }
        }
    }


    /**
     * Draws the background picture on the WHOLE panel, LEFT and RIGHT of the road.
     * This is in SCREEN pixels (not game pixels), because in fullscreen the panel
     * is much wider than the game and the picture fills the extra space too.
     *
     * Only called when the panel size changes: BackgroundCache keeps the result.
     *
     * The pictures are lined up starting at the road's edges and going outwards,
     * so the picture right next to the road is never cut on the road side.
     *
     * @param panelWidth   width of the whole panel in screen pixels
     * @param panelHeight  height of the whole panel in screen pixels
     * @param roadLeftX    screen x where the road starts
     * @param roadRightX   screen x where the road ends
     * @param tileSize     how big one picture is on the screen
     */
    public void drawBackground(Graphics2D g, int panelWidth, int panelHeight,
                               int roadLeftX, int roadRightX, int tileSize) {
        if (backgroundImage == null) {
            g.setColor(new Color(40, 140, 60));
            g.fillRect(0, 0, roadLeftX, panelHeight);
            g.fillRect(roadRightX, 0, panelWidth - roadRightX, panelHeight);
            return;
        }

        // left side: from the road edge going LEFT
        for (int x = roadLeftX - tileSize; x > -tileSize; x -= tileSize) {
            drawImageColumn(g, x, tileSize, panelHeight);
        }
        // right side: from the road edge going RIGHT
        for (int x = roadRightX; x < panelWidth; x += tileSize) {
            drawImageColumn(g, x, tileSize, panelHeight);
        }
    }

    /** One column of pictures from the top of the panel to the bottom. */
    private void drawImageColumn(Graphics2D g, int x, int tileSize, int panelHeight) {
        for (int y = 0; y < panelHeight; y += tileSize) {
            g.drawImage(backgroundImage, x, y, tileSize, tileSize, null);
        }
    }

        private void drawScrollingRoadImage(Graphics2D g) {
        int imageHeight = roadImage.getHeight();
        int startY = (int) roadImageOffset - imageHeight;

        for (int y = startY; y < GameConfig.WINDOW_HEIGHT; y += imageHeight) {
            g.drawImage(roadImage, GameConfig.ROAD_X, y, GameConfig.ROAD_WIDTH, imageHeight, null);
        }
    }


    /**
     * Helper: gives back the x coordinate of the MIDDLE of a lane.
     * Lanes are numbered from the left: 0, 1, 2, 3.
     * "static" so anyone can call Road.laneCenterX(2) without a Road object.
     */
    public static int laneCenterX(int lane) {
        return GameConfig.ROAD_X + lane * GameConfig.LANE_WIDTH + GameConfig.LANE_WIDTH / 2;
    }
}
