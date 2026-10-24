package f1;

import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;

/**
 * ImageLoader = loads pictures for the game from the resources folder.
 *
 * A path like "resources/textures/f1/player.png" is looked up from the folder
 * the program was STARTED in. If you start it from "src" (or another folder
 * inside the project), that folder has no "resources" in it and the picture
 * is not found. So we also look in the parent folders until we find it.
 */
public class ImageLoader {

    // MEMORY (cache): every picture is read from the disk only ONCE, after that it comes from here.
    // So a restart (R) doesn't load everything again and the game doesn't stutter.
    // ConcurrentHashMap = a HashMap that is safe to use from several threads at once
    // (GamePanel loads the pictures on a background thread, the game uses them on the Swing thread).
    // Optional because a missing picture (null) can't be stored in a ConcurrentHashMap.
    private static final Map<String, Optional<BufferedImage>> imageCache = new ConcurrentHashMap<>();
    private static final Map<String, List<BufferedImage>> folderCache = new ConcurrentHashMap<>();

    /**
     * Returns the picture, or null if the path is null or the file can't be found / read.
     * Null means: draw a colored box instead, the game never crashes because of a picture.
     */
    public static BufferedImage load(String imagePath) {
        if (imagePath == null) {
            return null;
        }
        // computeIfAbsent = "if it's not in the memory yet, read it and remember it"
        return imageCache.computeIfAbsent(imagePath, path -> Optional.ofNullable(readImage(path))).orElse(null);
    }

    /** Actually reads the picture from the disk. */
    private static BufferedImage readImage(String imagePath) {
        // Start in the current folder and go up one folder at a time.
        File folder = new File("").getAbsoluteFile();
        while (folder != null) {
            File file = new File(folder, imagePath);
            if (file.isFile()) {
                try {
                    return toFastFormat(ImageIO.read(file));
                } catch (IOException e) {
                    break; // the file exists but is not a valid picture
                }
            }
            folder = folder.getParentFile();
        }

        System.out.println("Could not load image: " + imagePath + " -> using a color instead");
        return null;
    }

    /**
     * Loads EVERY .png inside a folder, for example "resources/textures/f1/traffic".
     * Want a new traffic car? Just drop another .png into that folder, no code change needed.
     * Returns an empty list if the folder can't be found (then we use colored boxes).
     */
    public static List<BufferedImage> loadFolder(String folderPath) {
        return folderCache.computeIfAbsent(folderPath, ImageLoader::readFolder);
    }

    /** Actually reads the folder from the disk. */
    private static List<BufferedImage> readFolder(String folderPath) {
        // The list we fill up and give back. It starts empty.
        List<BufferedImage> images = new ArrayList<>();

        // Same search as in load(): start in the current folder and go up.
        File folder = new File("").getAbsoluteFile();
        // Keep going up while the folder we look for is NOT inside the current one.
        // getParentFile() gives null when we are at the very top (/), then we stop.
        while (folder != null && !new File(folder, folderPath).isDirectory()) {
            folder = folder.getParentFile();
        }
        // We went all the way up and never found it.
        if (folder == null) {
            System.out.println("Could not find image folder: " + folderPath + " -> using colors instead");
            return images;
        }

        // listFiles() = every file inside the folder (an array, like {moto_B.png, var1_B.png, ...}).
        File[] files = new File(folder, folderPath).listFiles();
        // Sort by name: the order from listFiles() can be random,
        Arrays.sort(files); // same order every time, nicer for debugging
        // Go through the files one by one.
        for (File file : files) {
            // toLowerCase() so "CAR.PNG" works too, not only "car.png".
            if (!file.getName().toLowerCase().endsWith(".png")) {
                continue; // skip anything that is not a picture
            }
            try {
                // ImageIO.read turns the file into a picture Java can draw.
                BufferedImage image = toFastFormat(ImageIO.read(file));
                // It gives null if the file is not a real picture (e.g. a broken .png).
                if (image != null) {
                    images.add(image);
                }
            } catch (IOException e) {
                // Reading failed: tell us in the console, skip this file, keep going.
                System.out.println("Could not load image: " + file.getName());
            }
        }
        // copyOf = a list nobody can change, so several threads can safely share it
        return List.copyOf(images);
    }

    /**
     * PNGs come in many formats (ours are mostly "8-bit colormap"). Java has to convert
     * those EVERY time they are drawn, which is slow, especially when scaled in fullscreen.
     * So we convert each picture ONCE, right after loading, into the format the screen uses.
     */
    private static BufferedImage toFastFormat(BufferedImage image) {
        if (image == null) {
            return null; // not a real picture, ImageIO.read gave null
        }
        BufferedImage fast = createImage(image.getWidth(), image.getHeight(), Transparency.TRANSLUCENT);
        Graphics2D g = fast.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return fast;
    }

    /**
     * A new empty picture in the screen's own format ("compatible image"), the fastest to draw.
     * transparency: Transparency.OPAQUE (no see-through parts) or Transparency.TRANSLUCENT.
     */
    public static BufferedImage createImage(int width, int height, int transparency) {
        if (GraphicsEnvironment.isHeadless()) { // no screen at all (e.g. a test), use a normal picture
            int type = transparency == Transparency.OPAQUE ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
            return new BufferedImage(width, height, type);
        }
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                .getDefaultConfiguration().createCompatibleImage(width, height, transparency);
    }

    // Only static methods, so nobody needs to create an ImageLoader.
    private ImageLoader() {
    }
}
