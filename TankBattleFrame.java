package minigames;

import javax.swing.JFrame;

// The window that holds the game
public class TankBattleFrame extends JFrame {

    private GamePanel gamePanel;

    public TankBattleFrame() {
        setUpWindow();
    }

    private void setUpWindow() {
        gamePanel = new GamePanel();

        this.setTitle("Battle Tanks");
        this.add(gamePanel);
        this.setResizable(false);
        this.pack();                           // makes the window fit the panel size
        this.setLocationRelativeTo(null);      // centers the window on the screen
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    // Temporary main method, so we can test the game on its own.
    // Later the title screen will open this window instead.
    public static void main(String[] args) {
        TankBattleFrame tankBattleFrame = new TankBattleFrame();
        tankBattleFrame.setVisible(true);
    }
}
