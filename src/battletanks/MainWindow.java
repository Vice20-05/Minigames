package battletanks;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

// The only window of the program. It swaps the panel inside it.
public class MainWindow extends JFrame implements ScreenNavigator {

    private ScoreRepository scoreRepository;

    public MainWindow() {
        super("Mini Games");

        // TODO: replace this with your colleague's database class.
        scoreRepository = new InMemoryScoreRepository();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        showTitleScreen();

        setLocationRelativeTo(null);
        setVisible(true);
    }

    // Removes the old screen and shows the new one.
    // The old GamePanel stops its game loop thread in removeNotify().
    private void showPanel(JPanel newPanel) {
        getContentPane().removeAll();
        getContentPane().add(newPanel);

        pack();
        revalidate();
        repaint();

        newPanel.requestFocusInWindow();
    }

    @Override
    public void showTitleScreen() {
        showPanel(new TitleScreenPanel(this));
    }

    @Override
    public void showTankBattle() {
        showPanel(new GamePanel(this, scoreRepository));
    }

    @Override
    public void showLeaderboard() {
        showPanel(new LeaderboardPanel(this, scoreRepository));
    }

    // Put your F1 game panel here when it exists.
    @Override
    public void showF1Game() {
        showPanel(createPlaceholderPanel("F1 Game is not connected yet."));
    }

    @Override
    public void exitProgram() {
        System.exit(0);
    }

    private JPanel createPlaceholderPanel(String message) {
        JPanel placeholderPanel = new JPanel(new BorderLayout());
        placeholderPanel.setPreferredSize(new Dimension(
                GamePanel.ARENA_WIDTH, GamePanel.ARENA_HEIGHT));

        JLabel messageLabel = new JLabel(message, SwingConstants.CENTER);
        messageLabel.setFont(new Font("Arial", Font.BOLD, 32));
        placeholderPanel.add(messageLabel, BorderLayout.CENTER);

        JButton backButton = new JButton("Back to title screen");
        backButton.setFont(new Font("Arial", Font.BOLD, 24));
        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                showTitleScreen();
            }
        });
        placeholderPanel.add(backButton, BorderLayout.SOUTH);

        return placeholderPanel;
    }
}