package battletanks;

import javax.swing.SwingUtilities;

// Starts the program. If you already have a main method, just
// create the window the same way: new MainWindow();
public class Main {

    public static void main(String[] args) {
        // Swing windows must be created on the Swing thread.
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new MainWindow();
            }
        });
    }
}