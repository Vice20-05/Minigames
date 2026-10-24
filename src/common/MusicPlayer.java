package common;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * MusicPlayer = the background music. One song plays at a time, on repeat:
 *   MusicPlayer.play(MusicPlayer.MENU);   // the menu calls this
 *   MusicPlayer.play(MusicPlayer.F1);     // the F1 game
 *   MusicPlayer.play(MusicPlayer.TANK);   // Battle Tanks
 *
 * The volume (0-100) and mute button are on the main menu and count for every game.
 * They are saved in settings.properties (in Resources.dataFolder()), so they stay
 * the same the next time the game starts.
 *
 * Java can play .wav files without any extra library, so the songs are WAVs in
 * resources/music (credits: resources/music/CREDITS.txt).
 *
 * Loading a song and opening the sound card can take a moment, so that happens on
 * its OWN background thread: the menu never freezes. No sound card? Then there is
 * simply no music, the game still works.
 */
public class MusicPlayer {

    public static final String MENU = "resources/music/menu.wav";
    public static final String F1 = "resources/music/f1.wav";
    public static final String TANK = "resources/music/tank.wav";

    private static final String SETTINGS_FILE = "settings.properties";
    private static final int DEFAULT_VOLUME = 50;

    // One background thread, so the songs are started/stopped one after the other, in order
    private static final ExecutorService soundThread = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "music");
        thread.setDaemon(true); // doesn't keep the program alive when the window closes
        return thread;
    });

    // volatile = every thread sees the newest value (the menu writes, the sound thread reads)
    private static volatile int volume = DEFAULT_VOLUME;
    private static volatile boolean muted = false;

    // Only used on the sound thread
    private static Clip clip;
    private static String currentSong;

    static {
        loadSettings();
    }

    private MusicPlayer() {
        // only static methods: MusicPlayer.play(...)
    }

    /** Starts this song on repeat (stops the old one). Already playing it? Then nothing changes. */
    public static void play(String song) {
        soundThread.execute(() -> {
            if (song.equals(currentSong)) {
                return;
            }
            closeClip();
            currentSong = song;
            clip = openClip(song);
            if (clip != null) {
                applyVolume();
                clip.loop(Clip.LOOP_CONTINUOUSLY);
            }
        });
    }

    public static int getVolume() {
        return volume;
    }

    public static boolean isMuted() {
        return muted;
    }

    /** @param newVolume 0 (silent) to 100 (loudest) */
    public static void setVolume(int newVolume) {
        volume = Math.max(0, Math.min(100, newVolume));
        soundThread.execute(MusicPlayer::applyVolume);
    }

    public static void setMuted(boolean newMuted) {
        muted = newMuted;
        soundThread.execute(MusicPlayer::applyVolume);
    }

    /** Writes the volume and mute setting to settings.properties (call it after a change). */
    public static void saveSettings() {
        Properties settings = new Properties();
        settings.setProperty("music.volume", String.valueOf(volume));
        settings.setProperty("music.muted", String.valueOf(muted));
        File file = new File(Resources.dataFolder(), SETTINGS_FILE);
        try (FileOutputStream out = new FileOutputStream(file)) {
            settings.store(out, "Mini Games settings");
        } catch (IOException e) {
            System.out.println("Couldn't save the settings: " + e.getMessage());
        }
    }

    private static void loadSettings() {
        File file = new File(Resources.dataFolder(), SETTINGS_FILE);
        if (!file.isFile()) {
            return; // first start: keep the defaults
        }
        Properties settings = new Properties();
        try (FileInputStream in = new FileInputStream(file)) {
            settings.load(in);
            volume = Math.max(0, Math.min(100, Integer.parseInt(
                    settings.getProperty("music.volume", String.valueOf(DEFAULT_VOLUME)))));
            muted = Boolean.parseBoolean(settings.getProperty("music.muted", "false"));
        } catch (IOException | NumberFormatException e) {
            System.out.println("Couldn't read the settings, using the defaults: " + e.getMessage());
        }
    }

    // Reads the .wav into a Clip (a sound that is fully in memory and can loop). null = no music.
    private static Clip openClip(String song) {
        try (InputStream file = Resources.open(song)) {
            if (file == null) {
                System.out.println("Music not found: " + song);
                return null;
            }
            // BufferedInputStream: AudioSystem needs to peek at the start of the file
            try (AudioInputStream audio = AudioSystem.getAudioInputStream(new BufferedInputStream(file))) {
                Clip newClip = AudioSystem.getClip();
                newClip.open(audio);
                return newClip;
            }
        } catch (IOException | UnsupportedAudioFileException | LineUnavailableException
                | IllegalArgumentException | SecurityException e) {
            System.out.println("Couldn't play the music (" + song + "): " + e.getMessage());
            return null;
        }
    }

    private static void closeClip() {
        if (clip != null) {
            clip.stop();
            clip.close();
            clip = null;
        }
    }

    /**
     * Sets the loudness of the clip. Sound cards use decibels (dB), not percent:
     * 0 dB = full volume, every -6 dB = about half as loud. 20 * log10(percent / 100)
     * turns the slider percent into dB, so the slider sounds even from 0 to 100.
     */
    private static void applyVolume() {
        if (clip == null || !clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            return;
        }
        FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        float decibels;
        if (muted || volume == 0) {
            decibels = gain.getMinimum(); // as quiet as it goes
        } else {
            decibels = (float) (20 * Math.log10(volume / 100.0));
        }
        gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), decibels)));
    }
}
