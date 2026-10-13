package minigames;

// The two parts of a round
public enum GameState {
    BUILDING,   // the players place walls, the tanks cannot move or shoot
    FIGHTING    // the normal game
}