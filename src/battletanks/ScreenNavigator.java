package battletanks;

// The screens use this interface to ask the main window to change the screen.
// A screen does not need to know the MainWindow class itself.
public interface ScreenNavigator {

    void showTitleScreen();

    void showF1Game();

    void showTankBattle();

    void showLeaderboard();

    void exitProgram();
}