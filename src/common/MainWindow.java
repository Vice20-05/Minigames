package common;

import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * MainWindow = the main menu window. It opens first and swaps its screen between the
 * menu (TitleScreenPanel) and the Scores screen (ScoresPanel).
 *
 * The games open in their OWN window, because the F1 game can be resized / fullscreen
 * and the tank arena has a fixed size. While a game is open the menu is hidden.
 * When the game window closes (Esc in F1, "TITLE SCREEN" in Battle Tanks, or the X),
 * the menu comes back.
 */
public class MainWindow extends JFrame implements ScreenNavigator {

    public static final int MENU_WIDTH = 1000;
    public static final int MENU_HEIGHT = 700;
    public static final Color MENU_BACKGROUND = new Color(30, 40, 50);

    private final ScoreRepository tankScores = new TankScoreRepository();

    private JFrame gameWindow; // the game that is open right now, null while the menu is shown

    public MainWindow() {
        super("Mini Games");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        showPanel(new TitleScreenPanel(this));

        setLocationRelativeTo(null);
        setVisible(true);
    }

    // Removes the old screen and shows the new one.
    private void showPanel(JPanel newPanel) {
        getContentPane().removeAll();
        getContentPane().add(newPanel);

        pack();
        revalidate();
        repaint();
    }

    @Override
    public void showTitleScreen() {
        if (gameWindow != null) {
            JFrame closingWindow = gameWindow;
            gameWindow = null;          // first, so the close listener below does nothing twice
            closingWindow.dispose();    // stops the game loop (F1: windowClosed, tanks: removeNotify)
        }
        showPanel(new TitleScreenPanel(this));
        setVisible(true);
        toFront();
    }

    @Override
    public void showF1Game() {
        // GameWindow opens itself and starts the game loop (see f1.GameWindow).
        openGame(new f1.GameWindow());
    }

    @Override
    public void showTankBattle() {
        JFrame tankWindow = new JFrame("Battle Tanks");
        tankWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        tankWindow.setResizable(false);

        // "this" = the TITLE SCREEN button of the tank game calls our showTitleScreen()
        battletanks.GamePanel tankPanel = new battletanks.GamePanel(this, tankScores);
        tankWindow.add(tankPanel);
        tankWindow.pack();
        tankWindow.setLocationRelativeTo(null);

        openGame(tankWindow);
        tankWindow.setVisible(true);
        tankPanel.requestFocusInWindow(); // the keys only work when the panel has the focus
    }

    @Override
    public void showScores() {
        showPanel(new ScoresPanel(this));
    }

    // Hides the menu while the game is open, and brings it back when the game window closes.
    private void openGame(JFrame window) {
        gameWindow = window;
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                if (gameWindow == window) { // still the open game (not already closed by showTitleScreen)
                    gameWindow = null;
                    showTitleScreen();
                }
            }
        });
        setVisible(false);
    }
}
