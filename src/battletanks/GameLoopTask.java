package battletanks;

// THREAD 1: runs the game loop (timing + game logic) away from the Swing thread.
public class GameLoopTask implements Runnable {

    private final Updatable updatableObject;
    private final int frameDelayMilliseconds;

    // volatile: stop() is called from another thread, run() must see the change.
    private volatile boolean isRunning;

    public GameLoopTask(Updatable updatableObject, int frameDelayMilliseconds) {
        this.updatableObject = updatableObject;
        this.frameDelayMilliseconds = frameDelayMilliseconds;
        this.isRunning = true;
    }

    @Override
    public void run() {
        while (isRunning) {
            try {
                updatableObject.update();
                Thread.sleep(frameDelayMilliseconds);
            } catch (InterruptedException exception) {
                // Somebody asked the thread to stop while it was sleeping.
                isRunning = false;
                Thread.currentThread().interrupt();
            } catch (RuntimeException exception) {
                // A bug in the game logic must not kill the thread silently.
                System.out.println("Game loop stopped because of an error: "
                        + exception.getMessage());
                exception.printStackTrace();
                isRunning = false;
            }
        }
    }

    public void stop() {
        isRunning = false;
    }
}
