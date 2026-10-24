package f1;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * GameWindow = the window (JFrame) that holds the game.
 *
 * It also has a main() method, so you can start JUST the F1 game for testing,
 * without the main menu. The main menu (common.MainWindow) opens it with:
 *     new f1.GameWindow();
 * and shows itself again when this window closes (Esc on the game over screen, or the X).
 */
public class GameWindow extends JFrame {

    private final GamePanel panel;

    public GameWindow() {
        setTitle("Minigame - Traffic Dogde ");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(true); // so it can go fullscreen (Super+F), the game scales to fit

        panel = new GamePanel();
        add(panel);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                panel.stop();
            }
        });

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
