package common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * DB_Main = the leaderboards. ONLINE they are in Supabase (a PostgreSQL database on the
 * internet), OFFLINE in a local SQLite file (scores.db in the ".minigames" folder in the
 * user's home folder, see Resources.dataFolder()).
 *
 * It works the same way for both, only the URL is different:
 *   1) open a Connection   2) run SQL with a PreparedStatement   3) close it
 *
 * The Supabase login data (url, user, password) is NOT in the code, it is read from
 * config.properties (see config.example.properties):
 *   - from the project folder, when the game runs from VS Code / the terminal
 *   - from inside Minigames.jar, where build.sh puts a copy, so players don't need the file.
 *     That copy must be the LIMITED game user (docs/supabase_game_user.sql), never the
 *     admin password, because anyone can unzip a jar and read it.
 *
 * ONLINE OR OFFLINE?
 *   checkConnection() tries Supabase (the menu calls it at startup and every time the menu
 *   or the Scores screen opens). Until then, and when it fails, we are offline.
 *   Online:  scores are saved to / loaded from Supabase.
 *   Offline: scores are saved to / loaded from scores.db. They stay there (no syncing).
 *   If an online save or load fails, DB_Main switches to offline and uses scores.db instead.
 *
 * Online calls wait for the internet (up to a few seconds), so call these methods from a
 * BACKGROUND thread (e.g. a SwingWorker), never directly from a button or the game loop.
 */
public class DB_Main {

    // The two leaderboards (same table names in Supabase and in scores.db). Always use these.
    public static final String F1 = "f1_scores";
    public static final String TANK = "tank_scores";

    private static final String CONFIG_FILE = "config.properties";
    private static final int TOP_COUNT = 10;
    private static final String TIMEOUT_SECONDS = "5";       // give up after 5 s without internet
    private static final String CHECK_TIMEOUT_SECONDS = "3"; // the online check gives up sooner

    // volatile = every thread sees the newest value (the check runs on a background thread)
    private static volatile boolean online = false;

    private DB_Main() {
        // only static methods, no objects needed: DB_Main.saveScore(...)
    }

    /**
     * Tries to reach Supabase (short timeout) and remembers the answer.
     * @return true = online (Supabase works), false = offline (use scores.db)
     */
    public static boolean checkConnection() {
        try (Connection conn = connect(CHECK_TIMEOUT_SECONDS)) {
            online = conn.isValid(Integer.parseInt(CHECK_TIMEOUT_SECONDS));
        } catch (SQLException | IOException e) {
            System.out.println("Supabase not reachable, scores are saved on this computer: " + e.getMessage());
            online = false;
        }
        return online;
    }

    /** The answer of the last checkConnection() (or false after an online call failed). */
    public static boolean isOnline() {
        return online;
    }

    /**
     * Saves a score, but keeps only the player's BEST one:
     *   - new name      -> a new row is added
     *   - name exists   -> the score only changes if the new one is HIGHER
     *
     * @param table    DB_Main.F1 or DB_Main.TANK
     * @return true if it was saved (online or on this computer), false if not at all
     */
    public static boolean saveScore(String table, String username, int score) {
        if (!isValidTable(table)) {
            return false;
        }
        // PostgreSQL calls "the bigger one" GREATEST, SQLite calls it MAX
        String onlineSql = upsert(table, "GREATEST(" + table + ".score, EXCLUDED.score)");
        String localSql = upsert(table, "MAX(" + table + ".score, EXCLUDED.score)");
        return save(onlineSql, localSql, username, score);
    }

    /**
     * ADDS points to the player's score instead of keeping the best one (used for Battle Tanks:
     * every win = +1, so the score is the number of wins):
     *   - new name      -> a new row with these points
     *   - name exists   -> old score + points
     *
     * @return true if it was saved (online or on this computer), false if not at all
     */
    public static boolean addPoints(String table, String username, int points) {
        if (!isValidTable(table)) {
            return false;
        }
        String sql = upsert(table, table + ".score + EXCLUDED.score"); // same SQL in both databases
        return save(sql, sql, username, points);
    }

    /**
     * The top 10 of a leaderboard, best first, from Supabase when online,
     * otherwise from scores.db. Every row is { username (String), score (Integer) },
     * ready for a JTable:
     *
     *   DefaultTableModel model = new DefaultTableModel(new String[] {"Name", "Score"}, 0);
     *   for (Object[] row : DB_Main.getTopScores(DB_Main.F1)) {
     *       model.addRow(row);
     *   }
     *
     * @return the rows, or an EMPTY list if it didn't work at all
     */
    public static List<Object[]> getTopScores(String table) {
        try {
            return loadTopScores(table);
        } catch (SQLException e) {
            System.out.println("Couldn't load the leaderboard: " + e.getMessage());
            return new ArrayList<>(); // don't return half a list
        }
    }

    /**
     * Same as getTopScores(), but it THROWS when even scores.db can't be read, instead of
     * giving back an empty list. Ask isOnline() afterwards to know where the rows came from.
     */
    public static List<Object[]> loadTopScores(String table) throws SQLException {
        if (!isValidTable(table)) {
            return new ArrayList<>();
        }
        String sql = "SELECT username, score FROM " + table + " ORDER BY score DESC LIMIT " + TOP_COUNT;

        if (online) {
            try (Connection conn = connect(TIMEOUT_SECONDS)) {
                return readRows(conn, sql);
            } catch (SQLException | IOException e) {
                System.out.println("Couldn't load online scores, showing the ones on this computer: "
                        + e.getMessage());
                online = false;
            }
        }
        try (Connection conn = connectLocal()) {
            return readRows(conn, sql);
        }
    }

    // "ON CONFLICT (username)" = this name is already in the table (username is UNIQUE).
    // EXCLUDED.score = the new score we just tried to insert.
    private static String upsert(String table, String newScore) {
        return "INSERT INTO " + table + " (username, score) VALUES (?, ?) "
             + "ON CONFLICT (username) DO UPDATE SET score = " + newScore;
    }

    /** Online: try Supabase first. Offline (or Supabase failed): save in scores.db. */
    private static boolean save(String onlineSql, String localSql, String username, int score) {
        if (online) {
            // try-with-resources: the connection is closed automatically, also on errors
            try (Connection conn = connect(TIMEOUT_SECONDS)) {
                runUpdate(conn, onlineSql, username, score);
                return true;
            } catch (SQLException | IOException e) {
                System.out.println("Couldn't save online, saving on this computer: " + e.getMessage());
                online = false;
            }
        }
        try (Connection conn = connectLocal()) {
            runUpdate(conn, localSql, username, score);
            return true;
        } catch (SQLException e) {
            System.out.println("Couldn't save the score: " + e.getMessage());
            return false;
        }
    }

    private static void runUpdate(Connection conn, String sql, String username, int score) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setInt(2, score);
            stmt.executeUpdate();
        }
    }

    private static List<Object[]> readRows(Connection conn, String sql) throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet result = stmt.executeQuery()) {
            while (result.next()) {
                rows.add(new Object[] { result.getString("username"), result.getInt("score") });
            }
        }
        return rows;
    }

    /** Opens a Supabase connection with the data from config.properties. */
    private static Connection connect(String timeoutSeconds) throws IOException, SQLException {
        Properties config = loadConfig();

        Properties login = new Properties();
        login.setProperty("user", config.getProperty("db.user", ""));
        login.setProperty("password", config.getProperty("db.password", ""));
        login.setProperty("connectTimeout", timeoutSeconds); // finding the server
        login.setProperty("socketTimeout", timeoutSeconds);  // waiting for an answer
        login.setProperty("loginTimeout", timeoutSeconds);   // the whole login
        return DriverManager.getConnection(config.getProperty("db.url", ""), login);
    }

    /**
     * Reads config.properties: the one in the project folder if there is one,
     * otherwise the copy inside Minigames.jar.
     */
    private static Properties loadConfig() throws IOException {
        Properties config = new Properties();
        File file = new File(CONFIG_FILE);
        try (InputStream stream = file.isFile()
                ? new FileInputStream(file)
                : DB_Main.class.getResourceAsStream("/" + CONFIG_FILE)) {
            if (stream == null) {
                throw new IOException(CONFIG_FILE + " not found (not in the project folder, not in the jar)");
            }
            config.load(stream);
        }
        return config;
    }

    /**
     * Opens scores.db (SQLite makes the file the first time) and makes sure both
     * tables exist, with the same columns as in Supabase.
     */
    private static Connection connectLocal() throws SQLException {
        File scoresFile = new File(Resources.dataFolder(), "scores.db");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + scoresFile.getAbsolutePath());
        try (Statement stmt = conn.createStatement()) {
            for (String table : new String[] { F1, TANK }) {
                stmt.execute("CREATE TABLE IF NOT EXISTS " + table + " ("
                           + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                           + "username TEXT NOT NULL UNIQUE, "
                           + "score INTEGER NOT NULL)");
            }
        } catch (SQLException e) {
            conn.close();
            throw e;
        }
        return conn;
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
