package common;

import java.sql.SQLException;

// Everything the tank game needs from the database.
// TankScoreRepository saves it to Supabase (see DB_Main).
public interface ScoreRepository {

    // Saves the result of one finished game.
    void saveGameResult(String gameName, String winnerName, String loserName)
            throws SQLException;
}
