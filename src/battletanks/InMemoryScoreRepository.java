package battletanks;

import java.util.ArrayList;

// TEMPORARY storage so the program works before the real database is connected.
// The results are lost when the program is closed.
// Replace it with your colleague's database class in MainWindow.
public class InMemoryScoreRepository implements ScoreRepository {

    private ArrayList<String> savedResultLines;

    public InMemoryScoreRepository() {
        savedResultLines = new ArrayList<String>();
    }

    @Override
    public void saveGameResult(String gameName,
            String winnerName, String loserName) {

        String resultLine = gameName + ": " + winnerName
                + " won against " + loserName;

        // Newest result goes to the top of the list.
        savedResultLines.add(0, resultLine);
    }

    @Override
    public ArrayList<String> getLeaderboardLines() {
        return new ArrayList<String>(savedResultLines);
    }
}