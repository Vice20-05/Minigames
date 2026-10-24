package common;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

/**
 * ScoresPanel = the two leaderboards side by side: F1 on the left, Battle Tanks on the right.
 *
 * Every table shows the top players, best first (DB_Main.loadTopScores). Every player is only
 * once in a table (username is UNIQUE in the database).
 *   Online:  the global leaderboard from Supabase, label "Online".
 *   Offline: the scores saved on this computer (scores.db), label "Offline".
 *
 * Checking the connection and loading can take a few seconds, so it runs on a BACKGROUND
 * THREAD (SwingWorker). Until it is done the tables say "Loading...".
 */
public class ScoresPanel extends JPanel {

    private static final Font TITLE_FONT = new Font("Impact", Font.PLAIN, 50);
    private static final Font BOARD_TITLE_FONT = new Font("Arial", Font.BOLD, 26);
    private static final Font TABLE_FONT = new Font("Arial", Font.PLAIN, 18);
    private static final Font BUTTON_FONT = new Font("Arial", Font.BOLD, 24);
    private static final Font SOURCE_FONT = new Font("Arial", Font.BOLD, 18);

    // "Online - global leaderboard" or "Offline - scores saved on this computer"
    private final JLabel sourceLabel = new JLabel("Checking connection...", SwingConstants.CENTER);
    private final Board f1Board;
    private final Board tankBoard;

    /** One leaderboard on the screen: its table and the status line under it. */
    private record Board(DefaultTableModel model, JLabel status) {
    }

    public ScoresPanel(ScreenNavigator screenNavigator) {
        setPreferredSize(new Dimension(MainWindow.MENU_WIDTH, MainWindow.MENU_HEIGHT));
        setBackground(MainWindow.MENU_BACKGROUND);
        setLayout(new BorderLayout(0, 20));
        setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        JLabel titleLabel = new JLabel("SCORES", SwingConstants.CENTER);
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(Color.WHITE);
        sourceLabel.setFont(SOURCE_FONT);
        sourceLabel.setForeground(Color.LIGHT_GRAY);
        JPanel header = new JPanel(new GridLayout(2, 1));
        header.setOpaque(false);
        header.add(titleLabel);
        header.add(sourceLabel);
        add(header, BorderLayout.NORTH);

        JPanel boards = new JPanel(new GridLayout(1, 2, 40, 0)); // 1 row, 2 columns
        boards.setOpaque(false);
        f1Board = createBoard("F1", boards);
        tankBoard = createBoard("Battle Tanks", boards);
        add(boards, BorderLayout.CENTER);

        JButton backButton = new JButton("Back");
        backButton.setFont(BUTTON_FONT);
        backButton.setFocusable(false);
        backButton.addActionListener(event -> screenNavigator.showTitleScreen());
        add(backButton, BorderLayout.SOUTH);

        loadInBackground();
    }

    /** One leaderboard: a title, a table (Name | Score) and a status line under it. */
    private Board createBoard(String title, JPanel boards) {
        JPanel board = new JPanel(new BorderLayout(0, 8));
        board.setOpaque(false);

        JLabel boardTitle = new JLabel(title, SwingConstants.CENTER);
        boardTitle.setFont(BOARD_TITLE_FONT);
        boardTitle.setForeground(Color.WHITE);
        board.add(boardTitle, BorderLayout.NORTH);

        // isCellEditable false = the player can't change the scores by double clicking
        DefaultTableModel model = new DefaultTableModel(new String[] {"Name", "Score"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable scoreTable = new JTable(model);
        scoreTable.setFont(TABLE_FONT);
        scoreTable.setRowHeight(28);
        scoreTable.getTableHeader().setFont(TABLE_FONT);
        scoreTable.getTableHeader().setReorderingAllowed(false);
        scoreTable.setFocusable(false);
        board.add(new JScrollPane(scoreTable), BorderLayout.CENTER);

        JLabel status = new JLabel("Loading...", SwingConstants.CENTER);
        status.setFont(TABLE_FONT);
        status.setForeground(Color.LIGHT_GRAY);
        board.add(status, BorderLayout.SOUTH);

        boards.add(board);
        return new Board(model, status);
    }

    /** What the background thread found: both leaderboards and where they came from. */
    private record Loaded(List<Object[]> f1Rows, List<Object[]> tankRows, boolean online) {
    }

    /**
     * Checks the connection and reads both leaderboards on a background thread, then fills
     * the tables on the Swing thread (done() runs there, so changing the tables is safe).
     */
    private void loadInBackground() {
        new SwingWorker<Loaded, Void>() {
            @Override
            protected Loaded doInBackground() throws Exception {
                DB_Main.checkConnection();
                List<Object[]> f1Rows = DB_Main.loadTopScores(DB_Main.F1);
                List<Object[]> tankRows = DB_Main.loadTopScores(DB_Main.TANK);
                boolean online = DB_Main.isOnline();
                if (!online) {
                    // went offline while loading: read F1 again, so both come from scores.db
                    f1Rows = DB_Main.loadTopScores(DB_Main.F1);
                }
                return new Loaded(f1Rows, tankRows, online);
            }

            @Override
            protected void done() {
                try {
                    Loaded loaded = get();
                    if (loaded.online()) {
                        sourceLabel.setText("Online - global leaderboard");
                        sourceLabel.setForeground(Color.GREEN);
                    } else {
                        sourceLabel.setText("Offline - scores saved on this computer");
                        sourceLabel.setForeground(Color.ORANGE);
                    }
                    fill(f1Board, loaded.f1Rows());
                    fill(tankBoard, loaded.tankRows());
                } catch (InterruptedException | ExecutionException e) {
                    System.out.println("Couldn't load the scores: " + e.getCause());
                    sourceLabel.setText("Offline");
                    sourceLabel.setForeground(Color.ORANGE);
                    for (Board board : new Board[] { f1Board, tankBoard }) {
                        board.status().setText("Couldn't load scores");
                        board.status().setForeground(Color.ORANGE);
                    }
                }
            }
        }.execute();
    }

    private void fill(Board board, List<Object[]> rows) {
        for (Object[] row : rows) {
            board.model().addRow(row); // row[0] = username, row[1] = score
        }
        board.status().setText(rows.isEmpty() ? "No scores yet" : " ");
    }
}
