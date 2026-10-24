package battletanks;

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

// The first screen: title and four buttons.
public class TitleScreenPanel extends JPanel {

    private static final Font TITLE_FONT = new Font("Impact", Font.PLAIN, 70);
    private static final Font BUTTON_FONT = new Font("Arial", Font.BOLD, 28);

    private ScreenNavigator screenNavigator;

    public TitleScreenPanel(ScreenNavigator screenNavigator) {
        this.screenNavigator = screenNavigator;

        setPreferredSize(new Dimension(
                GamePanel.ARENA_WIDTH, GamePanel.ARENA_HEIGHT));
        setBackground(new Color(30, 40, 50));
        setLayout(new BorderLayout());

        createTitleLabel();
        createButtons();
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
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(60, 330, 80, 330));

        JButton f1GameButton = createButton("F1 Game");
        f1GameButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                screenNavigator.showF1Game();
            }
        });

        JButton tankBattleButton = createButton("Tank Battle");
        tankBattleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                screenNavigator.showTankBattle();
            }
        });

        JButton leaderboardButton = createButton("Leaderboard");
        leaderboardButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                screenNavigator.showLeaderboard();
            }
        });

        JButton exitButton = createButton("Exit");
        exitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                screenNavigator.exitProgram();
            }
        });

        buttonPanel.add(f1GameButton);
        buttonPanel.add(tankBattleButton);
        buttonPanel.add(leaderboardButton);
        buttonPanel.add(exitButton);

        add(buttonPanel, BorderLayout.CENTER);
    }

    private JButton createButton(String buttonText) {
        JButton button = new JButton(buttonText);
        button.setFont(BUTTON_FONT);
        button.setFocusable(false);
        return button;
    }
}