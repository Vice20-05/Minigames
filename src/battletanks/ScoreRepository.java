package battletanks;

import java.sql.SQLException;
import java.util.ArrayList;

// Everything the games need from the database.
// Your colleague's database class should implement this interface.
public interface ScoreRepository {

    // Saves the result of one finished game.
    void saveGameResult(String gameName, String winnerName, String loserName)
            throws SQLException;

    // Returns the lines that the leaderboard screen shows.
    ArrayList<String> getLeaderboardLines() throws SQLException;
}
