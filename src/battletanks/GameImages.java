package battletanks;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Random;
import javax.imageio.ImageIO;

// Loads and stores all the pictures. If a picture cannot be loaded
// its getter returns null and the GamePanel draws a simple shape instead.
public class GameImages {

    private static final String[] BACKGROUND_PATHS = {
        "/textures/battletanks/bg1.png",
        "/textures/battletanks/bg2.png",
        "/textures/battletanks/bg3.png",
        "/textures/battletanks/bg4.png",
        "/textures/battletanks/bg5.png"
    };

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

    // Loads the pictures from the resources folder.
    public void loadAll() {
        playerOneTankImage = loadImage("/textures/battletanks/tank_blue.png");
        playerTwoTankImage = loadImage("/textures/battletanks/tank_bigRed.png");
        brickWallImage = loadImage("/textures/battletanks/barrelGreen_side.png");
        steelWallImage = loadImage("/textures/battletanks/barrelRust_side.png");
        bulletImage = loadImage("/textures/battletanks/bulletDark1_outline.png");
        heartImage = loadImage("/textures/battletanks/explosion2.png");

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
        try (InputStream input = GameImages.class.getResourceAsStream(imagePath)) {
            if (input == null) {
                System.out.println("Image not found: " + imagePath);
                return null;
            }

            BufferedImage loadedImage = ImageIO.read(input);

            if (loadedImage == null) {
                System.out.println("Unsupported image format: " + imagePath);
            }

            return loadedImage;
        } catch (IOException exception) {
            System.out.println("Could not read image " + imagePath
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
