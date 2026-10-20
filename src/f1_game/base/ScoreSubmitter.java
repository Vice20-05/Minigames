package f1_game.base;

import Database.DB_Main;
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
 *   canRestart()          may R restart now? (NOT while typing, a name can contain an R)
 *   draw(g, y)            draws the name / "Saving..." / "Saved!" text
 *
 * It goes through these steps (a "state"):
 *
 *   TYPING --Enter--> SAVING --> SAVED   (it worked)
 *                            --> FAILED  (no internet, wrong password...)
 *   TYPING --Esc----> SKIPPED            (the player doesn't want to save)
 */
public class ScoreSubmitter {

    // (Not just "State": SwingWorker already has its own State, Java would mix them up.)
    private enum SaveState { TYPING, SAVING, SAVED, FAILED, SKIPPED }

    private SaveState state = SaveState.SKIPPED; // nothing to do until the first start()
    private int score;                   // the score of the run that just ended
    private String name = "";            // what is typed right now
    private String lastName = "";        // the name used last time (only while the game is open)

    // Right after a crash W/A/S/D are probably still held, and a held key "types" itself
    // again and again (wwwww). So typing only starts once all driving keys are let go.
    private boolean waitForKeyRelease;

    /** The player crashed: get ready to type a name for this score. */
    public void start(double score) {
        this.score = (int) score; // the database wants a whole number
        name = lastName;          // filled in already, so Enter is enough to save again
        state = SaveState.TYPING;
        waitForKeyRelease = true;
    }

    /** May R restart the game now? Not while typing, otherwise a name with an R restarts it. */
    public boolean canRestart() {
        return state != SaveState.TYPING;
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
                if (!name.isEmpty()) { // empty name + Enter = do nothing
                    saveInBackground();
                    return;
                }
            } else if (c == KeyHandler.ESCAPE) {
                state = SaveState.SKIPPED;
                return;
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
                return DB_Main.saveScore(DB_Main.F1, nameToSave, scoreToSave);
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
            drawCentered(g, "[Enter] save    [Esc] skip", y + 35);
            return; // no "Press R" while typing, R is a letter now
        }

        if (state == SaveState.SAVING) {
            g.setColor(Color.LIGHT_GRAY);
            drawCentered(g, "Saving...", y);
        } else if (state == SaveState.SAVED) {
            g.setColor(Color.GREEN);
            drawCentered(g, "Saved!", y);
        } else if (state == SaveState.FAILED) {
            g.setColor(Color.ORANGE);
            drawCentered(g, "Couldn't save (no internet?)", y);
        }
        // SKIPPED: no status text, only "Press R"

        g.setColor(Color.WHITE);
        drawCentered(g, "Press R to restart", y + 35);
    }

    /** Draws text in the horizontal middle of the window (same as in GamePanel). */
    private void drawCentered(Graphics2D g, String text, int y) {
        int textWidth = g.getFontMetrics().stringWidth(text);
        g.drawString(text, (GameConfig.WINDOW_WIDTH - textWidth) / 2, y);
    }
}
