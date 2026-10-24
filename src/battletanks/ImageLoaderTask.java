package battletanks;

import javax.swing.JPanel;

// THREAD 2: loads the pictures in the background so the window opens at once.
// Until the pictures are ready the game is drawn with simple shapes.
public class ImageLoaderTask implements Runnable {

    private final GameImages gameImages;
    private final JPanel panelToRepaint;

    public ImageLoaderTask(GameImages gameImages, JPanel panelToRepaint) {
        this.gameImages = gameImages;
        this.panelToRepaint = panelToRepaint;
    }

    @Override
    public void run() {
        try {
            gameImages.loadAll();
        } catch (RuntimeException exception) {
            // The game still works with the fallback shapes.
            System.out.println("Image loading failed: " + exception.getMessage());
        }

        // repaint() is safe to call from any thread.
        panelToRepaint.repaint();
    }
}
