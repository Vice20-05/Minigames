package common;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

// The main menu: title, four buttons, the music settings and the connection status.
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
        createBottomPanel();
    }

    // Bottom of the menu: the music settings, and under them the connection status.
    private void createBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);

        bottomPanel.add(createMusicPanel(), BorderLayout.NORTH);
        bottomPanel.add(createConnectionLabel(), BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    // "Music" + volume slider + mute button. The setting counts for the menu AND both games.
    private JPanel createMusicPanel() {
        JPanel musicPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        musicPanel.setOpaque(false);
        musicPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

        JLabel musicLabel = new JLabel("Music");
        musicLabel.setFont(STATUS_FONT);
        musicLabel.setForeground(Color.WHITE);

        JSlider volumeSlider = new JSlider(0, 100, MusicPlayer.getVolume());
        volumeSlider.setOpaque(false);
        volumeSlider.setFocusable(false);
        volumeSlider.setPreferredSize(new Dimension(250, 30));
        volumeSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent event) {
                MusicPlayer.setVolume(volumeSlider.getValue());
                if (!volumeSlider.getValueIsAdjusting()) { // the mouse was let go: save it
                    MusicPlayer.saveSettings();
                }
            }
        });

        JButton muteButton = new JButton(muteButtonText());
        muteButton.setFont(STATUS_FONT);
        muteButton.setFocusable(false);
        muteButton.setPreferredSize(new Dimension(110, 30));
        muteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                MusicPlayer.setMuted(!MusicPlayer.isMuted());
                MusicPlayer.saveSettings();
                muteButton.setText(muteButtonText());
            }
        });

        musicPanel.add(musicLabel);
        musicPanel.add(volumeSlider);
        musicPanel.add(muteButton);
        return musicPanel;
    }

    // The button says what clicking it WILL do
    private String muteButtonText() {
        return MusicPlayer.isMuted() ? "Unmute" : "Mute";
    }

    private JLabel createConnectionLabel() {
        connectionLabel = new JLabel("Checking connection...", SwingConstants.CENTER);
        connectionLabel.setFont(STATUS_FONT);
        connectionLabel.setForeground(Color.LIGHT_GRAY);
        connectionLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 25, 0));
        return connectionLabel;
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
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(40, 330, 30, 330));

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
