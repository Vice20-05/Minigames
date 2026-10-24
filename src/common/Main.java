package common;

import javax.swing.SwingUtilities;

// START HERE: opens the main menu. From there the player picks F1, Battle Tanks or Scores.
public class Main {
    public static void main(String[] args) {
        // Swing windows must be created on the Swing thread.
        SwingUtilities.invokeLater(MainWindow::new);
    }
}
