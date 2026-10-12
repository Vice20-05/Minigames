package f1_game;

import f1_game.base.GameWindow;
import javax.swing.SwingUtilities;

public class f1Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(GameWindow::new);
    }
}
