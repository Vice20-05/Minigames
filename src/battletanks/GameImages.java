package battletanks;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import java.util.Random;
import javax.imageio.ImageIO;

// Loads and stores all the pictures. If a picture cannot be loaded
// its getter returns null and the GamePanel draws a simple shape instead.
public class GameImages {

    // Put your backgrounds in resources/textures/battletanks with these names.
    private static final String[] BACKGROUND_PATHS = {
        "resources/textures/battletanks/bg1.png",
        "resources/textures/battletanks/bg2.png",
        "resources/textures/battletanks/bg3.png",
        "resources/textures/battletanks/bg4.png",
        "resources/textures/battletanks/bg5.png"
    };

    // volatile: the loader thread writes them, the Swing thread reads them.
    private volatile Image currentBackgroundImage;
    private volatile Image playerOneTankImage;
    private volatile Image playerTwoTankImage;
    private volatile Image brickWallImage;
    private volatile Image steelWallImage;
    private volatile Image bulletImage;
    private volatile Image heartImage;

    private volatile Image[] backgroundImages;

    private final Random random;

    public GameImages() {
        random = new Random();
        backgroundImages = new Image[BACKGROUND_PATHS.length];
    }

    // Loads the PNG pictures from resources/textures/battletanks.
    // This is called by the image loader thread.
    public void loadAll() {
        playerOneTankImage = loadImage("resources/textures/battletanks/tank_blue.png");
        playerTwoTankImage = loadImage("resources/textures/battletanks/tank_bigRed.png");
        brickWallImage = loadImage("resources/textures/battletanks/barrelGreen_side.png");
        steelWallImage = loadImage("resources/textures/battletanks/barrelRust_side.png");
        bulletImage = loadImage("resources/textures/battletanks/bulletDark1_outline.png");
        heartImage = loadImage("resources/textures/battletanks/explosion2.png");

        Image[] loadedBackgrounds = new Image[BACKGROUND_PATHS.length];

        for (int backgroundNumber = 0;
                backgroundNumber < BACKGROUND_PATHS.length;
                backgroundNumber++) {

            loadedBackgrounds[backgroundNumber]
                    = loadImage(BACKGROUND_PATHS[backgroundNumber]);
        }

        backgroundImages = loadedBackgrounds;
        pickRandomBackground();
    }

    // Chooses one of the backgrounds that were really loaded.
    public void pickRandomBackground() {
        Image[] loadedBackgrounds = backgroundImages;
        ArrayList<Image> usableBackgrounds = new ArrayList<Image>();

        for (Image background : loadedBackgrounds) {
            if (background != null) {
                usableBackgrounds.add(background);
            }
        }

        if (usableBackgrounds.isEmpty()) {
            currentBackgroundImage = null;
            return;
        }

        int randomIndex = random.nextInt(usableBackgrounds.size());
        currentBackgroundImage = usableBackgrounds.get(randomIndex);
    }

    // Returns null when the picture is missing or broken.
    private Image loadImage(String imagePath) {
        try {
            // The path is looked up from the folder the program was STARTED in
            // (the project folder), the same way the F1 game loads its pictures.
            File imageFile = new File(imagePath);

            if (!imageFile.isFile()) {
                System.out.println("Image not found: " + imagePath);
                return null;
            }

            BufferedImage loadedImage = ImageIO.read(imageFile);

            if (loadedImage == null) {
                System.out.println("Unsupported image format: " + imagePath);
            }

            return loadedImage;
        } catch (IOException exception) {
            System.out.println("Could not read image " + imagePath
                    + ": " + exception.getMessage());
            return null;
        } catch (IllegalArgumentException exception) {
            System.out.println("Bad image path " + imagePath
                    + ": " + exception.getMessage());
            return null;
        }
    }

    public Image getCurrentBackgroundImage() {
        return currentBackgroundImage;
    }

    public Image getPlayerOneTankImage() {
        return playerOneTankImage;
    }

    public Image getPlayerTwoTankImage() {
        return playerTwoTankImage;
    }

    public Image getBrickWallImage() {
        return brickWallImage;
    }

    public Image getSteelWallImage() {
        return steelWallImage;
    }

    public Image getBulletImage() {
        return bulletImage;
    }

    public Image getHeartImage() {
        return heartImage;
    }
}
