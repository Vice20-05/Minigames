package common;

import battletanks.ScoreRepository;
import java.sql.SQLException;

/**
 * TankScoreRepository = saves Battle Tanks results to the tank table in Supabase.
 *
 * Score = number of wins: the winner gets +1. The loser gets +0, so a new player
 * still shows up on the leaderboard (with 0) after their first match.
 *
 * The tank GamePanel calls saveGameResult() after asking the two names. If saving
 * doesn't work it throws, and the GamePanel shows its "Could not save" message.
 */
public class TankScoreRepository implements ScoreRepository {

    @Override
    public void saveGameResult(String gameName, String winnerName, String loserName)
            throws SQLException {
        if (!DB_Main.addPoints(DB_Main.TANK, winnerName, 1)) {
            throw new SQLException("Couldn't save score (no internet?)");
        }
        DB_Main.addPoints(DB_Main.TANK, loserName, 0);
    }
}
