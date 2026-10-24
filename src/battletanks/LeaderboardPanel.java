package battletanks;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

// Shows the saved results from the database.
public class LeaderboardPanel extends JPanel {

    private ScreenNavigator screenNavigator;
    private ScoreRepository scoreRepository;

    private JTextArea resultsTextArea;

    public LeaderboardPanel(ScreenNavigator screenNavigator,
            ScoreRepository scoreRepository) {

        this.screenNavigator = screenNavigator;
        this.scoreRepository = scoreRepository;

        setPreferredSize(new Dimension(
                GamePanel.ARENA_WIDTH, GamePanel.ARENA_HEIGHT));
        setLayout(new BorderLayout(10, 10));

        createTitleLabel();
        createResultsArea();
        createBackButton();

        loadResults();
    }

    private void createTitleLabel() {
        JLabel titleLabel = new JLabel("LEADERBOARD", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Impact", Font.PLAIN, 50));
        add(titleLabel, BorderLayout.NORTH);
    }

    private void createResultsArea() {
        resultsTextArea = new JTextArea();
        resultsTextArea.setEditable(false);
        resultsTextArea.setFont(new Font("Consolas", Font.PLAIN, 20));

        add(new JScrollPane(resultsTextArea), BorderLayout.CENTER);
    }

    private void createBackButton() {
        JButton backButton = new JButton("Back to title screen");
        backButton.setFont(new Font("Arial", Font.BOLD, 24));
        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                screenNavigator.showTitleScreen();
            }
        });

        add(backButton, BorderLayout.SOUTH);
    }

    // Reads the results from the database. If that fails, the
    // user sees a message instead of a crash.
    private void loadResults() {
        try {
            ArrayList<String> resultLines = scoreRepository.getLeaderboardLines();

            if (resultLines.isEmpty()) {
                resultsTextArea.setText("No results yet.");
                return;
            }

            StringBuilder allLines = new StringBuilder();

            for (String currentLine : resultLines) {
                allLines.append(currentLine);
                allLines.append("\n");
            }

            resultsTextArea.setText(allLines.toString());
        } catch (SQLException exception) {
            resultsTextArea.setText("Could not load the results:\n"
                    + exception.getMessage());
        }
    }
}
