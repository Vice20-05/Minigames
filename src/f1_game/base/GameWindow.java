package f1_game.base;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * GameWindow = the window (JFrame) that holds the game.
 *
 * It also has a main() method, so you can start JUST the F1 game for testing,
 * without the main menu. Later the main menu (src/Main.java) can open it with:
 *     new f1_game.base.GameWindow();
 */
public class GameWindow extends JFrame {

    public GameWindow() {
        setTitle("F1 Minigame - Traffic Racer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // closing the window stops the program
        setResizable(false);

        GamePanel panel = new GamePanel();
        add(panel);

        pack();                       // make the window exactly as big as the panel wants
        setLocationRelativeTo(null);  // put the window in the middle of the screen
        setVisible(true);

        panel.start();                // start the game loop
    }

    /**
     * START HERE: run this to play.
     * SwingUtilities.invokeLater makes sure the window is created on the
     * special Swing thread (the "Event Dispatch Thread"). Always do it like this in Swing.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(GameWindow::new);
    }
}
