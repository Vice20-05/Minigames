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
    
    
    public Road(){
        backgroundImage=ImageLoader.load("Resorces/Textures/f1game_grass_background/background.png");
        // road.png doesn't exist yet -> a gray road is drawn until you add it
        roadImage=ImageLoader.load("Resorces/Textures/f1game_grass_background/road.png");
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

    

    public void draw(Graphics2D g) {
         // 1) Background (grass) - does NOT scroll
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT, null);
        } else {
            g.setColor(new Color(40, 140, 60));
            g.fillRect(0, 0, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);
        }

        // 2) Asphalt - DOES scroll
        if (roadImage != null) {
            drawScrollingRoadImage(g);
        } else {
            g.setColor(new Color(60, 60, 60));
            g.fillRect(GameConfig.ROAD_X, 0, GameConfig.ROAD_WIDTH, GameConfig.WINDOW_HEIGHT);
        }

        // 3) Solid white lines on the left and right edge of the road
        g.setColor(Color.WHITE);
        g.fillRect(GameConfig.ROAD_X, 0, 4, GameConfig.WINDOW_HEIGHT);
        g.fillRect(GameConfig.ROAD_X + GameConfig.ROAD_WIDTH - 4, 0, 4, GameConfig.WINDOW_HEIGHT);

        // 4) Dashed lines between the lanes (these scroll too)
        for (int lane = 1; lane < GameConfig.LANE_COUNT; lane++) {
            int lineX = GameConfig.ROAD_X + lane * GameConfig.LANE_WIDTH - 2;

            // Start a bit above the screen so there is never an empty gap at the top.
            for (int y = -DASH_PERIOD; y < GameConfig.WINDOW_HEIGHT; y += DASH_PERIOD) {
                g.fillRect(lineX, y + (int) scrollOffset, 4, DASH_LENGTH);
            }
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
