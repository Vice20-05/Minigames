package common;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * DB_Main = the online leaderboard (Supabase = a PostgreSQL database on the internet).
 *
 * It works like JDBC with SQLite, only the URL is different:
 *   1) open a Connection   2) run SQL with a PreparedStatement   3) close it
 *
 * The login data (url, user, password) is NOT in the code, it is read from
 * config.properties in the project root (see config.example.properties).
 *
 * NO INTERNET? Nothing crashes: saveScore() returns false, getTopScores() an empty list.
 *
 * Both methods wait for the internet (up to a few seconds), so call them from a
 * BACKGROUND thread (e.g. a SwingWorker), never directly from a button or the game loop.
 */
public class DB_Main {

    // The two leaderboards (table names in Supabase). Always use these, never type the name.
    public static final String F1 = "f1_scores";
    public static final String TANK = "tank_scores";

    private static final String CONFIG_FILE = "config.properties";
    private static final int TOP_COUNT = 10;
    private static final String TIMEOUT_SECONDS = "5"; // give up after 5 s without internet

    private DB_Main() {
        // only static methods, no objects needed: DB_Main.saveScore(...)
    }

    /**
     * Saves a score, but keeps only the player's BEST one:
     *   - new name      -> a new row is added
     *   - name exists   -> the score only changes if the new one is HIGHER (GREATEST)
     *
     * @param table    DB_Main.F1 or DB_Main.TANK
     * @return true if it was saved, false if not (no internet, wrong config...)
     */
    public static boolean saveScore(String table, String username, int score) {
        if (!isValidTable(table)) {
            return false;
        }
        // "ON CONFLICT (username)" = this name is already in the table (username is UNIQUE).
        // EXCLUDED.score = the new score we just tried to insert.
        String sql = "INSERT INTO " + table + " (username, score) VALUES (?, ?) "
                   + "ON CONFLICT (username) DO UPDATE "
                   + "SET score = GREATEST(" + table + ".score, EXCLUDED.score)";

        // try-with-resources: the connection is closed automatically, also on errors
        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setInt(2, score);
            stmt.executeUpdate();
            return true;
        } catch (SQLException | IOException e) {
            System.out.println("Couldn't save the score: " + e.getMessage());
            return false;
        }
    }

    /**
     * The top 10 of a leaderboard, best first.
     * Every row is { username (String), score (Integer) }, ready for a JTable:
     *
     *   DefaultTableModel model = new DefaultTableModel(new String[] {"Name", "Score"}, 0);
     *   for (Object[] row : DB_Main.getTopScores(DB_Main.F1)) {
     *       model.addRow(row);
     *   }
     *
     * @return the rows, or an EMPTY list if it didn't work (no internet...)
     */
    public static List<Object[]> getTopScores(String table) {
        List<Object[]> rows = new ArrayList<>();
        if (!isValidTable(table)) {
            return rows;
        }
        String sql = "SELECT username, score FROM " + table + " ORDER BY score DESC LIMIT " + TOP_COUNT;

        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet result = stmt.executeQuery()) {
            while (result.next()) {
                rows.add(new Object[] { result.getString("username"), result.getInt("score") });
            }
        } catch (SQLException | IOException e) {
            System.out.println("Couldn't load the leaderboard: " + e.getMessage());
            rows.clear(); // don't return half a list
        }
        return rows;
    }

    /** Opens a connection with the data from config.properties. */
    private static Connection connect() throws IOException, SQLException {
        Properties config = new Properties();
        try (FileInputStream file = new FileInputStream(CONFIG_FILE)) {
            config.load(file);
        }

        Properties login = new Properties();
        login.setProperty("user", config.getProperty("db.user", ""));
        login.setProperty("password", config.getProperty("db.password", ""));
        login.setProperty("connectTimeout", TIMEOUT_SECONDS); // finding the server
        login.setProperty("socketTimeout", TIMEOUT_SECONDS);  // waiting for an answer
        return DriverManager.getConnection(config.getProperty("db.url", ""), login);
    }

    /**
     * A table name can't be a "?" in SQL, so it is glued into the SQL text.
     * That's only safe because we allow nothing else than our 2 constants.
     */
    private static boolean isValidTable(String table) {
        if (F1.equals(table) || TANK.equals(table)) {
            return true;
        }
        System.out.println("Unknown leaderboard table: " + table + " (use DB_Main.F1 or DB_Main.TANK)");
        return false;
    }
}
