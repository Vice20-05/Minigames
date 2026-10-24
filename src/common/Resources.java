package common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Resources = finds the files in the "resources" folder (pictures, music), both when the
 * game runs from the project folder (VS Code, Ctrl+Shift+B) and from Minigames.jar.
 *
 *   From the jar:            the files are INSIDE the jar, same path ("resources/...").
 *   From the project folder: they are normal files. We look in the folder the game was
 *                            started in, and in its parent folders (so starting it from
 *                            "src" works too).
 *
 * Paths always use "/" and start with "resources/", for example
 * "resources/textures/f1/f1_game_playerCar/f1.png".
 */
public class Resources {

    private Resources() {
        // only static methods: Resources.open(...)
    }

    /**
     * Opens a file for reading. Close it when you are done (try-with-resources).
     * @return the stream, or null if the file is not found
     */
    public static InputStream open(String path) {
        // 1) inside the jar (getResourceAsStream wants a "/" in front)
        InputStream inJar = Resources.class.getResourceAsStream("/" + path);
        if (inJar != null) {
            return inJar;
        }
        // 2) a normal file in the project folder
        File file = findFile(path);
        if (file == null || !file.isFile()) {
            return null;
        }
        try {
            return new FileInputStream(file);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * The paths of every file directly inside a folder, sorted by name,
     * for example "resources/textures/f1/traffic" -> [".../moto_B.png", ".../var1_B.png", ...].
     * @return the paths (usable with open()), or an empty list if the folder is not found
     */
    public static List<String> list(String folderPath) {
        List<String> paths = new ArrayList<>();

        File jar = runningJar();
        if (jar != null) {
            // From the jar: go through every entry and keep the ones right inside the folder
            String prefix = folderPath + "/";
            try (JarFile jarFile = new JarFile(jar)) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    String name = entries.nextElement().getName();
                    String rest = name.substring(Math.min(prefix.length(), name.length()));
                    if (name.startsWith(prefix) && !rest.isEmpty() && !rest.contains("/")) {
                        paths.add(name);
                    }
                }
            } catch (IOException e) {
                System.out.println("Couldn't read the jar: " + e.getMessage());
            }
        } else {
            File folder = findFile(folderPath);
            File[] files = folder == null ? null : folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        paths.add(folderPath + "/" + file.getName());
                    }
                }
            }
        }

        Collections.sort(paths); // same order every time
        return paths;
    }

    /**
     * The folder where the game keeps its own files on this computer (scores.db, settings):
     * ".minigames" in the user's home folder. It is made the first time.
     */
    public static File dataFolder() {
        File folder = new File(System.getProperty("user.home"), ".minigames");
        folder.mkdirs();
        return folder;
    }

    // Start in the current folder and go up one folder at a time. null = not found anywhere.
    private static File findFile(String path) {
        // Our paths use "/", a file path on this computer uses its own separator ("\" on Windows)
        String filePath = path.replace('/', File.separatorChar);
        File folder = new File("").getAbsoluteFile();
        while (folder != null) {
            File file = new File(folder, filePath);
            if (file.exists()) {
                return file;
            }
            folder = folder.getParentFile();
        }
        return null;
    }

    // The jar the game was started from, or null when it runs from a folder of .class files.
    private static File runningJar() {
        try {
            File location = new File(Resources.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            return location.isFile() ? location : null;
        } catch (URISyntaxException | SecurityException | NullPointerException e) {
            return null;
        }
    }
}
