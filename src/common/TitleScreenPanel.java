package common;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

// The main menu: title and four buttons.
public class TitleScreenPanel extends JPanel {

    private static final Font TITLE_FONT = new Font("Impact", Font.PLAIN, 70);
    private static final Font BUTTON_FONT = new Font("Arial", Font.BOLD, 28);

    private static final Font STATUS_FONT = new Font("Arial", Font.BOLD, 18);

    private ScreenNavigator screenNavigator;

    // Bottom line: is Supabase reachable? (filled in by MainWindow after its check)
    private JLabel connectionLabel;

    public TitleScreenPanel(ScreenNavigator screenNavigator) {
        this.screenNavigator = screenNavigator;

        setPreferredSize(new Dimension(
                MainWindow.MENU_WIDTH, MainWindow.MENU_HEIGHT));
        setBackground(MainWindow.MENU_BACKGROUND);
        setLayout(new BorderLayout());

        createTitleLabel();
        createButtons();
        createConnectionLabel();
    }

    private void createConnectionLabel() {
        connectionLabel = new JLabel("Checking connection...", SwingConstants.CENTER);
        connectionLabel.setFont(STATUS_FONT);
        connectionLabel.setForeground(Color.LIGHT_GRAY);
        connectionLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 25, 0));

        add(connectionLabel, BorderLayout.SOUTH);
    }

    // true = online, false = offline
    public void setConnectionStatus(boolean online) {
        if (online) {
            connectionLabel.setText("Online - scores go to the global leaderboard");
            connectionLabel.setForeground(Color.GREEN);
        } else {
            connectionLabel.setText("Offline - scores are saved on this computer");
            connectionLabel.setForeground(Color.ORANGE);
        }
    }

    private void createTitleLabel() {
        JLabel titleLabel = new JLabel("MINI GAMES", SwingConstants.CENTER);
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(60, 0, 0, 0));

        add(titleLabel, BorderLayout.NORTH);
    }

    private void createButtons() {
        JPanel buttonPanel = new JPanel(new GridLayout(4, 1, 0, 20));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(50, 330, 40, 330));

        JButton f1GameButton = createButton("F1 Game");
        f1GameButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                screenNavigator.showF1Game();
            }
        });

        JButton tankBattleButton = createButton("Battle Tanks");
        tankBattleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                screenNavigator.showTankBattle();
            }
        });

        JButton scoresButton = createButton("Scores");
        scoresButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                screenNavigator.showScores();
            }
        });

        JButton quitButton = createButton("Quit");
        quitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                screenNavigator.quit();
            }
        });

        buttonPanel.add(f1GameButton);
        buttonPanel.add(tankBattleButton);
        buttonPanel.add(scoresButton);
        buttonPanel.add(quitButton);

        add(buttonPanel, BorderLayout.CENTER);
    }

    private JButton createButton(String buttonText) {
        JButton button = new JButton(buttonText);
        button.setFont(BUTTON_FONT);
        button.setFocusable(false);
        return button;
    }
}
