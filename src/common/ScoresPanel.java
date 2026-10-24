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
 * ScoresPanel = the two online leaderboards side by side: F1 on the left, Battle Tanks on the right.
 *
 * Every table shows the top players from Supabase (DB_Main.loadTopScores), best first.
 * Every player is only once in a table (username is UNIQUE in the database).
 *
 * Loading needs the internet, so it runs on a BACKGROUND THREAD (SwingWorker). Until it is
 * done the table says "Loading...". No internet = a message instead of a crash.
 */
public class ScoresPanel extends JPanel {

    private static final Font TITLE_FONT = new Font("Impact", Font.PLAIN, 50);
    private static final Font BOARD_TITLE_FONT = new Font("Arial", Font.BOLD, 26);
    private static final Font TABLE_FONT = new Font("Arial", Font.PLAIN, 18);
    private static final Font BUTTON_FONT = new Font("Arial", Font.BOLD, 24);

    public ScoresPanel(ScreenNavigator screenNavigator) {
        setPreferredSize(new Dimension(MainWindow.MENU_WIDTH, MainWindow.MENU_HEIGHT));
        setBackground(MainWindow.MENU_BACKGROUND);
        setLayout(new BorderLayout(0, 20));
        setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        JLabel titleLabel = new JLabel("SCORES", SwingConstants.CENTER);
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(Color.WHITE);
        add(titleLabel, BorderLayout.NORTH);

        JPanel boards = new JPanel(new GridLayout(1, 2, 40, 0)); // 1 row, 2 columns
        boards.setOpaque(false);
        boards.add(createBoard("F1", DB_Main.F1));
        boards.add(createBoard("Battle Tanks", DB_Main.TANK));
        add(boards, BorderLayout.CENTER);

        JButton backButton = new JButton("Back");
        backButton.setFont(BUTTON_FONT);
        backButton.setFocusable(false);
        backButton.addActionListener(event -> screenNavigator.showTitleScreen());
        add(backButton, BorderLayout.SOUTH);
    }

    /** One leaderboard: a title, a table (Name | Score) and a status line under it. */
    private JPanel createBoard(String title, String table) {
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

        loadInBackground(table, model, status);
        return board;
    }

    /**
     * Reads the scores from Supabase on a background thread, then fills the table on the
     * Swing thread (done() runs there, so changing the table is safe).
     */
    private void loadInBackground(String table, DefaultTableModel model, JLabel status) {
        new SwingWorker<List<Object[]>, Void>() {
            @Override
            protected List<Object[]> doInBackground() throws Exception {
                return DB_Main.loadTopScores(table); // throws if there is no connection
            }

            @Override
            protected void done() {
                try {
                    List<Object[]> rows = get();
                    for (Object[] row : rows) {
                        model.addRow(row); // row[0] = username, row[1] = score
                    }
                    status.setText(rows.isEmpty() ? "No scores yet" : " ");
                } catch (InterruptedException | ExecutionException e) {
                    System.out.println("Couldn't load the scores: " + e.getCause());
                    status.setText("Couldn't load scores (no internet?)");
                    status.setForeground(Color.ORANGE);
                }
            }
        }.execute();
    }
}
