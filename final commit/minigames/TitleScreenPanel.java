package common;

import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import battletanks.MusicPlayer;
import battletanks.ScreenNavigator;
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

    // Path of the title music inside src/main/resources
    private static final String TITLE_MUSIC_PATH = "/sounds/monkeys.wav";

    private ScreenNavigator screenNavigator;

    // The same music player class the games use
    private MusicPlayer musicPlayer;

    public TitleScreenPanel(ScreenNavigator screenNavigator) {
    this.screenNavigator = screenNavigator;
    this.musicPlayer = new MusicPlayer();

    setPreferredSize(new Dimension(
            MainWindow.MENU_WIDTH, MainWindow.MENU_HEIGHT));
    setBackground(MainWindow.MENU_BACKGROUND);
    setLayout(new BorderLayout());

    createTitleLabel();
    createButtons();
    createMusicListener();
}
   // Starts the music when the title panel becomes visible
// and stops it when the panel is no longer visible (for example, when a game opens).
private void createMusicListener() {
    addHierarchyListener(new HierarchyListener() {
        @Override
        public void hierarchyChanged(HierarchyEvent event) {
            // We only care about the event that says "showing on screen" changed.
            boolean showingChanged =
                    (event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0;

            if (!showingChanged) {
                return;
            }

            if (isShowing()) {
                musicPlayer.playMusic(TITLE_MUSIC_PATH);
            } else {
                musicPlayer.stopMusic();
            }
        }
    });
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