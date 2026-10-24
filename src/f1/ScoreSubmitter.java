package f1;

import common.DB_Main;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.concurrent.ExecutionException;
import javax.swing.SwingWorker;

/**
 * ScoreSubmitter = the "save your score online" part of the game over screen.
 *
 * After a crash the player types a name, presses Enter, and the score is sent to the
 * online leaderboard (DB_Main). This class does ONLY that, so GamePanel stays about the game.
 *
 * How GamePanel uses it:
 *   start(score)          when the player crashes
 *   update(typed, keys)   every frame while the game over screen is shown
 *   canRestart()          may R restart now? (only while the name is still empty, see below)
 *   draw(g, y)            draws the name / "Saving..." / "Saved!" text and the controls
 *
 * The controls on the game over screen:
 *   name + Enter  -> save the score to the F1 table in Supabase
 *   R             -> restart right away, nothing is saved
 *   Esc           -> back to the main menu, nothing is saved (GamePanel handles Esc)
 *
 * R is also a letter. So R restarts only while the name is EMPTY; once you started typing,
 * R is just a letter of the name (Backspace everything to restart with R again).
 *
 * It goes through these steps (a "state"):
 *
 *   TYPING --Enter--> SAVING --> SAVED   (it worked)
 *                            --> FAILED  (no internet, wrong password...)
 */
public class ScoreSubmitter {

    // (Not just "State": SwingWorker already has its own State, Java would mix them up.)
    private enum SaveState { TYPING, SAVING, SAVED, FAILED }

    private SaveState state = SaveState.SAVED; // nothing to do until the first start()
    private volatile boolean savedOffline; // true = the last save went to scores.db (no internet)
    private int score;                   // the score of the run that just ended
    private String name = "";            // what is typed right now
    private String lastName = "";        // the name used last time (only while the game is open)

    // Right after a crash W/A/S/D are probably still held, and a held key "types" itself
    // again and again (wwwww). So typing only starts once all driving keys are let go.
    private boolean waitForKeyRelease;

    /** The player crashed: get ready to type a name for this score. */
    public void start(double score) {
        this.score = (int) score; // the database wants a whole number
        name = "";                // empty, so R can restart right away (Enter alone saves as lastName)
        state = SaveState.TYPING;
        waitForKeyRelease = true;
    }

    /** May R restart the game now? Yes, unless a name is being typed (then R is a letter). */
    public boolean canRestart() {
        return state != SaveState.TYPING || name.isEmpty();
    }

    /**
     * Called every frame on the game over screen.
     * "typed" = the keys typed since the last frame (from KeyHandler.takeTypedKeys()).
     */
    public void update(String typed, KeyHandler keys) {
        if (state != SaveState.TYPING) {
            return; // already saving / saved / skipped: nothing to type anymore
        }

        if (waitForKeyRelease) {
            if (!keys.isDrivingKeyPressed()) {
                waitForKeyRelease = false; // all let go: from the next frame on typing works
            }
            return;
        }

        // Go through the typed keys one by one, in the order they were pressed
        for (char c : typed.toCharArray()) {
            if (c == KeyHandler.ENTER) {
                if (name.isEmpty()) {
                    name = lastName; // empty + Enter = save again with the last name (if there is one)
                }
                if (!name.isEmpty()) {
                    saveInBackground();
                    return;
                }
            } else if (c == KeyHandler.ESCAPE) {
                return; // GamePanel already went back to the menu
            } else if (c == KeyHandler.BACKSPACE) {
                if (!name.isEmpty()) {
                    name = name.substring(0, name.length() - 1); // cut the last letter
                }
            } else if (name.length() < GameConfig.NAME_MAX_LENGTH) {
                name += c; // KeyHandler only lets letters and numbers through
            }
        }
    }

    /**
     * Sends the score to the online leaderboard on a BACKGROUND THREAD.
     * Without internet it can take a few seconds before it gives up, and the game runs
     * on the Swing thread: waiting there would freeze the whole game.
     *
     * SwingWorker (same as the picture loading in GamePanel):
     *   doInBackground() -> runs on another thread (the slow internet part)
     *   done()           -> runs on the Swing thread afterwards (safe to change our variables)
     */
    private void saveInBackground() {
        state = SaveState.SAVING;
        lastName = name;

        // Copy them now: the background thread must not read our variables later,
        // they could change in the meantime (e.g. the player restarts and crashes again)
        String nameToSave = name;
        int scoreToSave = score;

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                // DB_Main saves to Supabase when online, otherwise to scores.db on this computer
                boolean saved = DB_Main.saveScore(DB_Main.F1, nameToSave, scoreToSave);
                savedOffline = !DB_Main.isOnline();
                return saved;
            }

            @Override
            protected void done() {
                boolean saved;
                try {
                    saved = get(); // the true/false from doInBackground()
                } catch (InterruptedException | ExecutionException e) {
                    saved = false;
                }
                // Only change it if we are still waiting for THIS save
                // (the player may have restarted and be typing a new name already)
                if (state == SaveState.SAVING) {
                    state = saved ? SaveState.SAVED : SaveState.FAILED;
                }
            }
        }.execute();
    }

    /** Draws the name input or the saving status. y = where the first line goes. */
    public void draw(Graphics2D g, int y) {
        g.setFont(new Font("Arial", Font.PLAIN, 22));

        if (state == SaveState.TYPING) {
            g.setColor(Color.WHITE);
            drawCentered(g, "Enter your name: " + name + "_", y);
            g.setFont(new Font("Arial", Font.PLAIN, 16));
            if (name.isEmpty() && !lastName.isEmpty()) {
                g.setColor(Color.LIGHT_GRAY);
                drawCentered(g, "(Enter = save as " + lastName + ")", y + 25);
            }
            g.setColor(Color.WHITE);
            if (name.isEmpty()) {
                drawCentered(g, "[Enter] save    [R] restart    [Esc] menu", y + 55);
            } else {
                drawCentered(g, "[Enter] save    [Esc] menu", y + 55); // R is a letter now
            }
            return;
        }

        if (state == SaveState.SAVING) {
            g.setColor(Color.LIGHT_GRAY);
            drawCentered(g, "Saving...", y);
        } else if (state == SaveState.SAVED) {
            g.setColor(Color.GREEN);
            drawCentered(g, savedOffline ? "Saved on this computer (offline)" : "Saved!", y);
        } else if (state == SaveState.FAILED) {
            g.setColor(Color.ORANGE);
            drawCentered(g, "Couldn't save score", y);
        }

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 16));
        drawCentered(g, "[R] restart    [Esc] menu", y + 55);
    }

    /** Draws text in the horizontal middle of the window (same as in GamePanel). */
    private void drawCentered(Graphics2D g, String text, int y) {
        int textWidth = g.getFontMetrics().stringWidth(text);
        g.drawString(text, (GameConfig.WINDOW_WIDTH - textWidth) / 2, y);
    }
}
