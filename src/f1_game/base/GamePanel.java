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

    private double score;
    private int ticks;         // how many frames since the start (ticks / FPS = seconds)
    private boolean gameOver;

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

    /** Puts everything back to the beginning (used at start and after a crash). */
    private void resetGame() {
        road = new Road();
        player = new PlayerCar();
        traffic = new ArrayList<>();
        spawner = new TrafficSpawner();
        score = 0;
        ticks = 0;
        gameOver = false;
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

        ticks++;

        // Move the player (reads the keyboard)
        player.update(keys);

        // Scroll the road based on the player's speed
        road.update(player.getSpeed() * GameConfig.PIXELS_PER_KMH);

        // Maybe add a new traffic car
        spawner.update(traffic);

        // Move every traffic car
        for (TrafficCar car : traffic) {
            car.update(player.getSpeed());
        }

        // Delete cars that left the screen ("removeIf" = remove every car where the test is true)
        traffic.removeIf(TrafficCar::isOffScreen);

        checkCollisions();
        updateScore();
    }

    private void checkCollisions() {
        for (TrafficCar car : traffic) {
            // intersects = do the two invisible boxes overlap
            if (player.getBounds().intersects(car.getBounds())) {
                gameOver = true;
                return;
            }
        }
        // TODO (Victor): near-miss bonus -> see Victor.txt, "Next steps"
    }

    private void updateScore() {
        double pointsThisFrame = GameConfig.POINTS_PER_SECOND / GameConfig.FPS;
        score += pointsThisFrame * getMultiplier();
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

        // Smooth edges on shapes and text
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        road.draw(g);               // back
        for (TrafficCar car : traffic) {
            car.draw(g);
        }
        player.draw(g);
        drawHud(g);                 // front
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
        g.drawString("Score: " + (int) score, 190, 26);
        g.drawString(String.format("x%.1f", getMultiplier()), 300, 26);
        g.drawString("Time: " + (ticks / GameConfig.FPS) + "s", 380, 26);
    }

    private void drawGameOver(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);

        g.setColor(Color.RED);
        g.setFont(new Font("Arial", Font.BOLD, 48));
        drawCentered(g, "CRASH!", GameConfig.WINDOW_HEIGHT / 2 - 40);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 22));
        drawCentered(g, "Score: " + (int) score, GameConfig.WINDOW_HEIGHT / 2 + 10);
        drawCentered(g, "Press R to restart", GameConfig.WINDOW_HEIGHT / 2 + 50);
    }

    /** Draws text in the horizontal middle of the window. */
    private void drawCentered(Graphics2D g, String text, int y) {
        int textWidth = g.getFontMetrics().stringWidth(text);
        g.drawString(text, (GameConfig.WINDOW_WIDTH - textWidth) / 2, y);
    }
}
