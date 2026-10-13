package f1_game.base;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * GamePanel = the HEART of the game. It is the "canvas" inside the window.
 *
 * It does 2 jobs, 60 times per second (this is the GAME LOOP):
 *   1) UPDATE -> move everything, check crashes, add score   (updateGame)
 *   2) DRAW   -> paint everything on the screen               (paintComponent)
 *
 * We use ONE javax.swing.Timer for the loop (the plan says: one timer drives everything).
 * Every TICK_MS milliseconds the Timer calls actionPerformed(...).
 */
public class GamePanel extends JPanel implements ActionListener {

    private final Timer timer;
    private final KeyHandler keys = new KeyHandler();

    // Everything that lives in the game:
    private Road road;
    private PlayerCar player;
    private List<TrafficCar> traffic;
    private TrafficSpawner spawner;

    private double baseScore;  // points from time only, WITHOUT the multiplier (see getScore)
    private int ticks;         // how many frames since the start (ticks / FPS = seconds)
    private boolean gameOver;
    private boolean started;   // false until the player presses W the first time

    // Near miss on screen: "NEAR MISS! +50" shows for a moment after every near miss.
    private int nearMissPopupTicks;  // frames left to show the text (0 = hidden)
    private int nearMissPopupPoints; // the points written in the text
    private int nearMissCount;       // how many near misses this run (shown at game over)

    public GamePanel() {
        setPreferredSize(new Dimension(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT));
        setBackground(Color.BLACK);
        setDoubleBuffered(true);  // draws to a hidden image first -> no flickering
        setFocusable(true);       // the panel must have "focus" to receive key presses
        addKeyListener(keys);

        resetGame();

        // "this" = this GamePanel is the ActionListener, so the Timer calls our actionPerformed.
        timer = new Timer(GameConfig.TICK_MS, this);
    }

    /** Starts the game loop. Called by GameWindow after the window is visible. */
    public void start() {
        requestFocusInWindow(); // ask for keyboard focus
        timer.start();
    }

    /** Stops the game loop when the containing window is closed. */
    public void stop() {
        timer.stop();
    }

    /** Puts everything back to the beginning (used at start and after a crash). */
    private void resetGame() {
        road = new Road();
        player = new PlayerCar();
        traffic = new ArrayList<>();
        spawner = new TrafficSpawner();
        baseScore = 0;
        ticks = 0;
        gameOver = false;
        started = false; // after a restart (R) we wait for W again
        nearMissPopupTicks = 0;
        nearMissCount = 0;
    }

    /** The Timer calls this 60 times per second. */
    @Override
    public void actionPerformed(ActionEvent e) {
        updateGame();
        repaint(); // tells Swing "please call paintComponent soon"
    }

    // =====================================================================
    //  1) UPDATE
    // =====================================================================
    private void updateGame() {
        if (gameOver) {
            if (keys.isRestartPressed()) {
                resetGame();
            }
            return; // when the game is over, nothing moves
        }

        // Wait for the first W press. Until then nothing moves: no time, no score, no traffic.
        if (!started) {
            if (keys.isUpPressed()) {
                started = true; // go! from the next line on the game runs normally
            } else {
                return; // still waiting, skip the rest of the update
            }
        }

        ticks++;

        // Move the player (reads the keyboard)
        player.update(keys);

        // Scroll the road based on the player's speed
        road.update(player.getSpeed() * GameConfig.PIXELS_PER_KMH);

        // Maybe add a new traffic car
        spawner.update(traffic);

        // After LANE_CHANGE_SCORE points some cars switch lanes
        spawner.updateLaneChanges(traffic, getScore());

        // Move every traffic car
        for (TrafficCar car : traffic) {
            car.update(player.getSpeed());
        }

        // Delete cars that left the screen ("removeIf" = remove every car where the test is true)
        traffic.removeIf(TrafficCar::isOffScreen);

        checkCollisions();
        updateScore();

        // Count down the "NEAR MISS!" text, it disappears when it reaches 0
        if (nearMissPopupTicks > 0) {
            nearMissPopupTicks--;
        }
    }

    private void checkCollisions() {
        for (TrafficCar car : traffic) {
            // intersects = do the two invisible boxes overlap
            if (player.getBounds().intersects(car.getBounds())) {
                gameOver = true;
                return;
            }
            // We only get here if the car did NOT crash into us.
            // Near miss = the traffic car touches the player's BIGGER box (but not the car itself).
            // !car.hasNearMissed() makes sure each car gives the bonus only once.
            if (player.getNearMissBounds().intersects(car.getBounds()) && !car.hasNearMissed()) {
                baseScore += GameConfig.NEAR_MISS_BONUS;
                car.markNearMissed(); // this car is done, no more bonus from it

                // Show "NEAR MISS! +points" on screen. The bonus goes into baseScore,
                // so on screen it is worth NEAR_MISS_BONUS x the multiplier.
                nearMissCount++;
                nearMissPopupPoints = (int) (GameConfig.NEAR_MISS_BONUS * getMultiplier());
                nearMissPopupTicks = GameConfig.NEAR_MISS_POPUP_TICKS;
            }
        }
    }

    private void updateScore() {
        double pointsThisFrame = GameConfig.POINTS_PER_SECOND / GameConfig.FPS;
        baseScore += pointsThisFrame;
    }

    /**
     * The score you see = base points x the multiplier RIGHT NOW.
     * Example: 10 base points at x2.0 -> 20. Brake back to x1.0 -> 10 again.
     * After a crash nothing moves anymore, so the multiplier at the crash is the final one.
     */
    private double getScore() {
        return baseScore * getMultiplier();
    }

    /**
     * Faster = more points. 0 km/h -> x1.0, 100 km/h -> x1.5, 200 km/h -> x2.0,
     * and it stops at MAX_MULTIPLIER.
     */
    private double getMultiplier() {
        double multiplier = 1.0 + player.getSpeed() / 200.0;
        return Math.min(multiplier, GameConfig.MAX_MULTIPLIER);
    }

    // =====================================================================
    //  2) DRAW
    // =====================================================================

    /**
     * Swing calls this every time the panel needs to be painted.
     * NEVER call it yourself, call repaint() instead.
     * ORDER MATTERS: what you draw first is in the back, what you draw last is on top.
     */
    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics); // clears the old frame
        Graphics2D g = (Graphics2D) graphics; // Graphics2D has more features than Graphics

        // Smooth edges on shapes and text, and smooth pictures when they are scaled
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // FULLSCREEN / RESIZING:
        // The game is always WINDOW_WIDTH x WINDOW_HEIGHT "game pixels". If the panel is bigger
        // (fullscreen), we make the game bigger by "scale" so it fills the height, and move it
        // to the middle by "offsetX"/"offsetY". The extra space on the sides gets the background.
        double scale = Math.min((double) getWidth() / GameConfig.WINDOW_WIDTH,
                                (double) getHeight() / GameConfig.WINDOW_HEIGHT);
        double offsetX = (getWidth() - GameConfig.WINDOW_WIDTH * scale) / 2;
        double offsetY = (getHeight() - GameConfig.WINDOW_HEIGHT * scale) / 2;

        int roadLeftX = (int) Math.round(offsetX + GameConfig.ROAD_X * scale);
        int roadRightX = (int) Math.round(offsetX + (GameConfig.ROAD_X + GameConfig.ROAD_WIDTH) * scale);
        int tileSize = Math.max(1, (int) Math.round(GameConfig.BACKGROUND_TILE_SIZE * scale));
        road.drawBackground(g, getWidth(), getHeight(), roadLeftX, roadRightX, tileSize); // very back

        // From here on we draw in GAME pixels: move + zoom the "pen", and don't draw outside the game.
        g.translate(offsetX, offsetY);
        g.scale(scale, scale);
        g.clipRect(0, 0, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);

        road.draw(g);               // back
        for (TrafficCar car : traffic) {
            car.draw(g);
        }
        player.draw(g);
        drawNearMissPopup(g);
        drawHud(g);                 // front
        if (!started) {
            drawStartScreen(g);     // "Press W to start" on top of everything
        }
        if (gameOver) {
            drawGameOver(g);
        }
    }

    /** HUD = the text on top of the game (speed, score, time). */
    private void drawHud(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 150)); // black, half transparent
        g.fillRect(0, 0, GameConfig.WINDOW_WIDTH, 40);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Speed: " + (int) player.getSpeed() + " km/h", 10, 26);
        g.drawString("Score: " + (int) getScore(), 190, 26);
        g.drawString(String.format("x%.1f", getMultiplier()), 300, 26);
        g.drawString("Time: " + (ticks / GameConfig.FPS) + "s", 380, 26);
    }

    /**
     * "NEAR MISS! +50" above the player. It floats up and fades out:
     * the fewer frames are left, the higher and more transparent it is.
     */
    private void drawNearMissPopup(Graphics2D g) {
        if (nearMissPopupTicks <= 0) {
            return; // nothing to show
        }
        // 1.0 right after the near miss, going down to 0.0 at the end
        double timeLeft = (double) nearMissPopupTicks / GameConfig.NEAR_MISS_POPUP_TICKS;

        int alpha = (int) (255 * timeLeft);        // 255 = solid, 0 = invisible
        int floatUp = (int) (40 * (1 - timeLeft)); // moves 40 pixels up over time

        g.setColor(new Color(255, 215, 0, alpha)); // gold
        g.setFont(new Font("Arial", Font.BOLD, 24));
        drawCentered(g, "NEAR MISS! +" + nearMissPopupPoints, GameConfig.PLAYER_Y - 30 - floatUp);
    }

    /** Shown before the game starts, until W is pressed. */
    private void drawStartScreen(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 150)); // black, half transparent, so you still see the road
        g.fillRect(0, 0, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 32));
        drawCentered(g, "Press W  or ↑ to start, ", GameConfig.WINDOW_HEIGHT / 2);
    }

    private void drawGameOver(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);

        g.setColor(Color.RED);
        g.setFont(new Font("Hardigan", Font.BOLD, 48));
        drawCentered(g, "Game Over !", GameConfig.WINDOW_HEIGHT / 2 - 40);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 22));
        drawCentered(g, "Score: " + (int) getScore(), GameConfig.WINDOW_HEIGHT / 2 + 10);
        drawCentered(g, "Near misses: " + nearMissCount, GameConfig.WINDOW_HEIGHT / 2 + 45);
        drawCentered(g, "Press R to restart", GameConfig.WINDOW_HEIGHT / 2 + 85);
    }

    /** Draws text in the horizontal middle of the window. */
    private void drawCentered(Graphics2D g, String text, int y) {
        int textWidth = g.getFontMetrics().stringWidth(text);
        g.drawString(text, (GameConfig.WINDOW_WIDTH - textWidth) / 2, y);
    }
}
