Requirements
Create a small Java game with two different game modes.
1. Game Mode: F1 Racing Car
- In this mode, the player controls a car on a 2D track. The car is 3 blocks in size, with 4 blocks in front of it.
- The player's objective is to avoid obstacles. Obstacles appear randomly on the track, and the player must steer the car to avoid collisions.
- The graphical user interface must be implemented using Java Swing.
2. Game Mode: Tank Battle
- Two players compete against each other in this mode.
- Player 1 moves using the W, A, S, D keys and shoots using the Shift key closest to them (Left Shift).
- Player 2 moves using the arrow keys and shoots using the Shift key closest to them (Right Shift).
- When the game starts, various block elements are placed on the playing field.
- Both players can place additional blocks on the playing field.
- The placed blocks act as obstacles.
- Players can also use the obstacles as cover.
- The objective of the game is to defeat the opponent's tank.
- Players must pay attention to the opponent's movements and the obstacles on the playing field.
3. Leaderboards and Score Management
- Each game mode must have its own separate leaderboard.
- Players' scores must be saved in a database.
- The leaderboard must display each player's name and their achieved score.
- At the end of each game, the result must be saved automatically.
- There must also be a global leaderboard that displays scores achieved by other players.
4. Optional Features: Online Leaderboard and Multiplayer
- Scores may optionally be saved online using Supabase.
- Supabase may also be used to implement multiplayer functionality.
- The game must check whether an internet connection is available.
- If an internet connection is available, the global leaderboard will be accessible. (Pure AI.)
- If there is no internet connection, the game's core features must continue to work.
- Offline scores can also be stored in a local database.
- Airtable or Supabase can be used to implement the global leaderboard.
