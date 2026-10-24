=====================================================================
          ONLINE LEADERBOARD (Supabase) - HOW TO USE DB_Main
=====================================================================

The leaderboards are in Supabase (a PostgreSQL database on the
internet). We talk to it with plain JDBC, exactly like with SQLite,
only the URL is different. Everything is in:

    src/common/DB_Main.java        (package common)

Tables (already made in Supabase):
    f1_scores   (id, username UNIQUE, score INT)
    tank_scores (id, username UNIQUE, score INT)

Every player is in a table only ONCE, with their BEST score.


---------------------------------------------------------------------
1. SETUP (one time)
---------------------------------------------------------------------
a) The driver: lib/postgresql-42.7.14.jar is already in the repo.
   - VS Code: nothing to do (.vscode/settings.json adds lib/*.jar).
     Ctrl+Shift+B compiles and runs with it too.
   - IntelliJ: File -> Project Structure -> Libraries -> + -> Java
     -> pick lib/postgresql-42.7.14.jar.
   - Terminal (Linux/Mac, on Windows use ; instead of :):
       javac -d out -cp "lib/*" src/common/*.java src/f1/*.java src/battletanks/*.java
       java -cp "out:lib/*" common.Main

b) The password file:
   - Copy  config.example.properties  and name the copy
     config.properties  (same folder, next to src).
   - Fill in the 3 lines (ask me for the password, or take it from
     Supabase -> Connect -> "Session pooler").
   - Use the SESSION POOLER address, not "Direct connection". The
     direct one often doesn't work on home/school internet.
   - config.properties is in .gitignore: NEVER commit it, the
     password would be on GitHub.

   The game must be started FROM the project folder (the one with
   src and config.properties), otherwise the file isn't found.
   (Same rule as the pictures in resources.)


---------------------------------------------------------------------
2. THE METHODS
---------------------------------------------------------------------
    DB_Main.F1     = "f1_scores"     always use these constants,
    DB_Main.TANK   = "tank_scores"   never type the table name

    boolean DB_Main.saveScore(table, username, score)
        Saves the score. If the name is already there, it only changes
        if the new score is HIGHER.
        true = saved, false = didn't work (no internet...).

    List<Object[]> DB_Main.getTopScores(table)
        The top 10, best first. Every row = { username, score }.
        No internet -> an EMPTY list (not null, no crash).

    List<Object[]> DB_Main.loadTopScores(table)
        Same as getTopScores, but it THROWS (SQLException / IOException)
        when it doesn't work. The Scores screen uses it to show
        "Couldn't load scores" instead of "No scores yet".

    boolean DB_Main.addPoints(table, username, points)
        ADDS the points to the player's score (new name -> new row).
        Battle Tanks uses it: every win = +1, so score = number of wins.
        true = saved, false = didn't work (no internet...).

IMPORTANT: all of them can take up to ~5-10 seconds without internet.
Never call them directly on the Swing thread (in a button click,
in paintComponent, in the game loop...) or the window freezes.
Use a SwingWorker like in the examples below.


---------------------------------------------------------------------
3. EXAMPLE: SAVE A TANK SCORE (at the end of a tank game)
---------------------------------------------------------------------
    import common.DB_Main;
    import javax.swing.SwingWorker;

    String name = "Belle";   // copy the values into final variables first
    int points = 3;

    new SwingWorker<Boolean, Void>() {
        @Override
        protected Boolean doInBackground() {      // background thread: slow work
            return DB_Main.saveScore(DB_Main.TANK, name, points);
        }

        @Override
        protected void done() {                   // back on the Swing thread
            try {
                if (get()) {
                    System.out.println("Saved!");
                } else {
                    System.out.println("Couldn't save (no internet?)");
                }
            } catch (Exception e) {
                System.out.println("Couldn't save: " + e);
            }
        }
    }.execute();

(The F1 game already does this itself on its game over screen, in
src/f1/ScoreSubmitter.java, saveInBackground().
The tank game saves through src/common/TankScoreRepository.java:
winner +1 win, loser +0, so both names show up on the leaderboard.)


---------------------------------------------------------------------
4. EXAMPLE: TOP 10 IN A JTABLE (score screen)
---------------------------------------------------------------------
    import common.DB_Main;
    import java.util.List;
    import javax.swing.*;
    import javax.swing.table.DefaultTableModel;

    // 1) an empty table with 2 columns, shown right away
    DefaultTableModel model = new DefaultTableModel(new String[] {"Name", "Score"}, 0);
    JTable table = new JTable(model);
    panel.add(new JScrollPane(table));   // JScrollPane = shows the column titles

    // 2) load the rows in the background, then put them in the table
    new SwingWorker<List<Object[]>, Void>() {
        @Override
        protected List<Object[]> doInBackground() {
            return DB_Main.getTopScores(DB_Main.F1);   // or DB_Main.TANK
        }

        @Override
        protected void done() {
            try {
                List<Object[]> rows = get();
                if (rows.isEmpty()) {
                    // no internet (or nobody played yet): show a message instead
                }
                for (Object[] row : rows) {
                    model.addRow(row);         // row[0] = name, row[1] = score
                }
            } catch (Exception e) {
                System.out.println("Couldn't load the leaderboard: " + e);
            }
        }
    }.execute();

To refresh: model.setRowCount(0); then run the SwingWorker again.


---------------------------------------------------------------------
5. PROBLEMS
---------------------------------------------------------------------
"config.properties (No such file or directory)"
    -> The file is missing, or the game wasn't started from the
       project folder.
"No suitable driver found"
    -> The .jar isn't on the classpath (see 1a), or db.url doesn't
       start with  jdbc:postgresql://
"The connection attempt failed" / timeout
    -> No internet, or the Direct connection address was used
       instead of the Session pooler.
"password authentication failed"
    -> Wrong db.user / db.password. The pooler user looks like
       postgres.abcdefghijklmnop (with the project ref after the dot).

The errors are printed in the console, the game keeps running.
