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
import java.util.concurrent.ExecutionException;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
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
    private List<Obstacle> obstacles;           // roadblocks + potholes on the road
    private RoadblockSpawner roadblockSpawner;

    private double baseScore;  // points from time only, WITHOUT the multiplier (see getScore)
    private int ticks;         // how many frames since the start (ticks / FPS = seconds)
    private boolean gameOver;
    private boolean started;   // false until the player presses W the first time

    // Popup text above the player, for a moment: "NEAR MISS! +50", "ROADBLOCK CLEARED! +15 km/h"
    private int popupTicks;          // frames left to show the text (0 = hidden)
    private int popupTotalTicks;     // how long it was shown in total (for the fade out)
    private String popupText = "";   // what is written
    private int nearMissCount;       // how many near misses this run (shown at game over)

    // Paints the road on its own background thread (see RoadRenderer).
    // "this::repaint" = when a new road picture is ready, call our repaint().
    private final RoadRenderer roadRenderer = new RoadRenderer(this::repaint);

    // false while the pictures are still loading on the background thread -> "Loading..." screen
    private boolean loaded = false;

    public GamePanel() {
        setPreferredSize(new Dimension(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT));
        setBackground(Color.BLACK);
        setDoubleBuffered(true);  // draws to a hidden image first -> no flickering
        setFocusable(true);       // the panel must have "focus" to receive key presses
        addKeyListener(keys);

        loadImagesInBackground(); // resetGame() is called when the loading is done

        // "this" = this GamePanel is the ActionListener, so the Timer calls our actionPerformed.
        timer = new Timer(GameConfig.TICK_MS, this);
    }

    /**
     * Reads all the pictures from the disk on a BACKGROUND THREAD, so the window opens
     * right away and doesn't freeze while the files load.
     *
     * SwingWorker is Swing's helper for this:
     *   doInBackground() -> runs on a background thread (slow work: reading files)
     *   done()           -> runs on the Swing thread afterwards (safe to touch the game again)
     *
     * ImageLoader remembers every picture, so the "new Road()", "new TrafficSpawner()"...
     * in resetGame() just take them from memory: no loading on the Swing thread, also not on restart.
     */
    private void loadImagesInBackground() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                ImageLoader.load(PlayerCar.IMAGE_PATH);
                ImageLoader.load(Road.BACKGROUND_PATH);
                ImageLoader.load(Road.ROAD_PATH);
                ImageLoader.loadFolder(TrafficSpawner.TRAFFIC_FOLDER);
                ImageLoader.load(RoadblockSpawner.ROADBLOCK_PATH);
                ImageLoader.load(RoadblockSpawner.POTHOLE_PATH);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // throws if doInBackground crashed
                } catch (InterruptedException | ExecutionException e) {
                    System.out.println("Loading pictures failed -> using colors instead: " + e);
                }
                resetGame();
                loaded = true;
                repaint();
            }
        }.execute();
    }

    /** Starts the game loop. Called by GameWindow after the window is visible. */
    public void start() {
        requestFocusInWindow(); // ask for keyboard focus
        timer.start();
    }

    /** Stops the game loop when the containing window is closed. */
    public void stop() {
        timer.stop();
        roadRenderer.stop();
    }

    /** Puts everything back to the beginning (used at start and after a crash). */
    private void resetGame() {
        road = new Road();
        player = new PlayerCar();
        traffic = new ArrayList<>();
        spawner = new TrafficSpawner();
        obstacles = new ArrayList<>();
        roadblockSpawner = new RoadblockSpawner();
        baseScore = 0;
        ticks = 0;
        gameOver = false;
        started = false; // after a restart (R) we wait for W again
        popupTicks = 0;
        nearMissCount = 0;
    }

    /** The Timer calls this 60 times per second. */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!loaded) {
            return; // pictures still loading, nothing to update yet
        }
        updateGame();
        // Hand the road to the road thread. When it's painted, the road thread calls
        // repaint() ("please call paintComponent soon"), which then also draws the cars.
        roadRenderer.requestFrame(road, getWidth(), getHeight());
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

        // Maybe add a roadblock + pothole (from 1000 points on), and a new traffic car
        roadblockSpawner.update(obstacles, traffic, getScore());
        spawner.update(traffic, obstacles, getScore());

        // Cars that were picked to switch lanes do it when they reach their spot
        spawner.updateLaneChanges(traffic, obstacles);

        // Move every traffic car and every obstacle
        for (TrafficCar car : traffic) {
            car.update(player.getSpeed());
        }
        for (Obstacle obstacle : obstacles) {
            obstacle.update(player.getSpeed());
        }

        // Faster cars brake behind slower ones and stop behind roadblocks, so nothing overlaps
        spawner.keepDistance(traffic, obstacles);

        // Delete things that left the screen ("removeIf" = remove every one where the test is true)
        traffic.removeIf(TrafficCar::isOffScreen);
        obstacles.removeIf(Obstacle::isOffScreen);

        checkCollisions();
        checkRoadblockPassed();
        updateScore();

        // Count down the popup text, it disappears when it reaches 0
        if (popupTicks > 0) {
            popupTicks--;
        }
    }

    /** Shows a text above the player for "ticks" frames. */
    private void showPopup(String text, int ticks) {
        popupText = text;
        popupTicks = ticks;
        popupTotalTicks = ticks;
    }

    /**
     * When the WHOLE work zone is behind the player (the pothole is the last part),
     * the player gets a small speed boost. Each work zone only once (markPassed).
     */
    private void checkRoadblockPassed() {
        for (Obstacle obstacle : obstacles) {
            boolean behindPlayer = obstacle.getY() > player.getBottomY();
            if (!obstacle.isRoadblock() && !obstacle.isPassed() && behindPlayer) {
                obstacle.markPassed();
                player.addSpeedBoost(GameConfig.ROADBLOCK_PASS_BOOST);
                showPopup("CLEARED! +" + (int) GameConfig.ROADBLOCK_PASS_BOOST + " km/h", GameConfig.POPUP_TICKS);
            }
        }
    }

    private void checkCollisions() {
        // Roadblock or pothole: always game over, there is no near miss with a wall
        for (Obstacle obstacle : obstacles) {
            if (player.getBounds().intersects(obstacle.getBounds())) {
                gameOver = true;
                return;
            }
        }

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
                int points = (int) (GameConfig.NEAR_MISS_BONUS * getMultiplier());
                showPopup("NEAR MISS! +" + points, GameConfig.NEAR_MISS_POPUP_TICKS);
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
        // (fullscreen), ScreenFit tells us how much to zoom ("scale") and where the middle is.
        ScreenFit fit = ScreenFit.of(getWidth(), getHeight());

        // Background + road (very back). The road thread already painted them into a
        // picture, here we only copy that picture onto the screen.
        roadRenderer.draw(g);

        // From here on we draw in GAME pixels: move + zoom the "pen", and don't draw outside the game.
        g.translate(fit.offsetX(), fit.offsetY());
        g.scale(fit.scale(), fit.scale());
        g.clipRect(0, 0, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);

        if (!loaded) {
            drawLoadingScreen(g); // the game objects don't exist yet
            return;
        }

        for (Obstacle obstacle : obstacles) {
            obstacle.draw(g);       // on the ground, so below the cars
        }
        for (TrafficCar car : traffic) {
            car.draw(g);
        }
        player.draw(g);
        drawPopup(g);
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
     * The popup ("NEAR MISS! +50", "CLEARED! +15 km/h") above the player. It floats up and
     * fades out: the fewer frames are left, the higher and more transparent it is.
     */
    private void drawPopup(Graphics2D g) {
        if (popupTicks <= 0) {
            return; // nothing to show
        }
        // 1.0 right after it appeared, going down to 0.0 at the end
        double timeLeft = (double) popupTicks / popupTotalTicks;

        int alpha = (int) (255 * timeLeft);        // 255 = solid, 0 = invisible
        int floatUp = (int) (40 * (1 - timeLeft)); // moves 40 pixels up over time

        g.setColor(new Color(255, 215, 0, alpha)); // gold
        g.setFont(new Font("Arial", Font.BOLD, 24));
        drawCentered(g, popupText, GameConfig.PLAYER_Y - 30 - floatUp);
    }

    /** Shown while the pictures load on the background thread. */
    private void drawLoadingScreen(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 32));
        drawCentered(g, "Loading...", GameConfig.WINDOW_HEIGHT / 2);
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
