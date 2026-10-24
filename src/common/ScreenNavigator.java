package common;

// The screens use this interface to ask the main window to change the screen.
// A screen does not need to know the MainWindow class itself.
public interface ScreenNavigator {

    // Closes the game that is open (if any) and shows the main menu again.
    void showTitleScreen();

    void showF1Game();

    void showTankBattle();

    void showScores();
}
