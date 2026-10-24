package battletanks;
 
// Anything that can be updated once per frame by the game loop thread.
// The game loop only knows this interface, not the whole GamePanel.
public interface Updatable {
 
    void update();
}
 
