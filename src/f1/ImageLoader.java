package f1;

import common.Resources;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;

/**
 * ImageLoader = loads pictures for the game from the resources folder.
 *
 * A path like "resources/textures/f1/player.png" is found by common.Resources:
 * inside Minigames.jar, or in the project folder (and its parent folders, so
 * starting the game from "src" works too).
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

    /** Actually reads the picture (from the jar or the project folder, see common.Resources). */
    private static BufferedImage readImage(String imagePath) {
        BufferedImage image = readFromResources(imagePath);
        if (image == null) {
            System.out.println("Could not load image: " + imagePath + " -> using a color instead");
        }
        return image;
    }

    /**
     * Loads EVERY .png inside a folder, for example "resources/textures/f1/traffic".
     * Want a new traffic car? Just drop another .png into that folder, no code change needed.
     * Returns an empty list if the folder can't be found (then we use colored boxes).
     */
    public static List<BufferedImage> loadFolder(String folderPath) {
        return folderCache.computeIfAbsent(folderPath, ImageLoader::readFolder);
    }

    /** Actually reads the folder (from the jar or the project folder). */
    private static List<BufferedImage> readFolder(String folderPath) {
        // The list we fill up and give back. It starts empty.
        List<BufferedImage> images = new ArrayList<>();

        // Every file in the folder, sorted by name (same order every time, nicer for debugging)
        List<String> paths = Resources.list(folderPath);
        if (paths.isEmpty()) {
            System.out.println("Could not find image folder: " + folderPath + " -> using colors instead");
            return images;
        }

        // Go through the files one by one.
        for (String path : paths) {
            // toLowerCase() so "CAR.PNG" works too, not only "car.png".
            if (!path.toLowerCase().endsWith(".png")) {
                continue; // skip anything that is not a picture
            }
            BufferedImage image = readFromResources(path);
            // null if the file is not a real picture (e.g. a broken .png).
            if (image != null) {
                images.add(image);
            } else {
                // Reading failed: tell us in the console, skip this file, keep going.
                System.out.println("Could not load image: " + path);
            }
        }
        // copyOf = a list nobody can change, so several threads can safely share it
        return List.copyOf(images);
    }

    // ImageIO.read turns the file into a picture Java can draw. null = missing or broken.
    private static BufferedImage readFromResources(String path) {
        try (InputStream stream = Resources.open(path)) {
            return stream == null ? null : toFastFormat(ImageIO.read(stream));
        } catch (IOException e) {
            return null;
        }
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
