package f1_game.base;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * ImageLoader = loads pictures for the game from the Resorces folder.
 *
 * A path like "Resorces/Textures/player.png" is looked up from the folder
 * the program was STARTED in. If you start it from "src" (or another folder
 * inside the project), that folder has no "Resorces" in it and the picture
 * is not found. So we also look in the parent folders until we find it.
 */
public class ImageLoader {

    /**
     * Returns the picture, or null if the path is null or the file can't be found / read.
     * Null means: draw a colored box instead, the game never crashes because of a picture.
     */
    public static BufferedImage load(String imagePath) {
        if (imagePath == null) {
            return null;
        }

        // Start in the current folder and go up one folder at a time.
        File folder = new File("").getAbsoluteFile();
        while (folder != null) {
            File file = new File(folder, imagePath);
            if (file.isFile()) {
                try {
                    return ImageIO.read(file);
                } catch (IOException e) {
                    break; // the file exists but is not a valid picture
                }
            }
            folder = folder.getParentFile();
        }

        System.out.println("Could not load image: " + imagePath + " -> using a color instead");
        return null;
    }

    // Only static methods, so nobody needs to create an ImageLoader.
    private ImageLoader() {
    }
}
