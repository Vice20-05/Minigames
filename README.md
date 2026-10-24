# 🎮 Minigames

A Java Swing game collection with two game modes, **F1 Racing** and **Tank Battle**, plus online leaderboards powered by Supabase.

Built as a university Java project.

---

## 📸 Screenshots

| Main menu | Scores |
|---|---|
| ![Main menu](docs/screenshots/main-menu.png) | ![Scores screen with the F1 and Battle Tanks leaderboards](docs/screenshots/scores.png) |

| F1 Racing | Battle Tanks |
|---|---|
| <img src="docs/screenshots/f1-racing.png" alt="F1 Racing with traffic, a roadblock and a pothole" width="380"> | ![Battle Tanks arena](docs/screenshots/battle-tanks.png) |

---

## 👥 Team

| Member | GitHub | Role |
|---|---|---|
| Vice | [@Vice20-05](https://github.com/Vice20-05) | Project manager, repo owner, F1 Racing, database (Supabase) |
| Colleague | [@bellamusulin-del](https://github.com/bellamusulin-del) | Tank Battle, main menu, leaderboard screen |

---

## 🕹️ Game Modes

### 🏎️ F1 Racing
A top-down scrolling racer. Dodge traffic, survive as long as possible and rack up points.

- Traffic spawns randomly in lanes, and there's always at least one open lane
- Speed multiplier: the faster you drive, the more points you earn
- Near-miss bonuses for close calls without crashing
- Roadblocks and potholes appear after 1,000 points
- Difficulty ramps up over time, then levels off so it stays beatable

### 💥 Tank Battle
A local 2-player tank fight on a destructible battlefield.

- Block elements are placed on the field at the start of the game
- Both players can place extra blocks as obstacles and cover
- Bullets ricochet off walls, so bank shots are possible
- Defeat the other player's tank to win

---

## ⌨️ Controls

**F1 Racing**

| Key | Action |
|---|---|
| Arrow keys / WASD | Steer and control speed |
| Enter (game over) | Save your name and score |
| R (game over) | Restart without saving |
| Esc (game over) | Back to main menu without saving |

**Tank Battle**

| Player | Move | Shoot |
|---|---|---|
| Player 1 | W, A, S, D | Left Shift |
| Player 2 | Arrow keys | Right Shift |

---

## 🏆 Leaderboards

- Each game mode has its **own leaderboard**
- Scores are saved online in **Supabase** (PostgreSQL)
- The Scores screen shows both leaderboards side by side: player name and score, highest first (top 10)
- The game checks the connection at startup. The menu shows **Online** or **Offline**
- If there's no internet connection, the games still work and scores are saved in a local SQLite database (`scores.db` in the `.minigames` folder in your home folder). The Scores screen then shows those local scores with an **Offline** label (offline scores are not uploaded later)
- The built game connects by itself: `build.sh` puts the login of a **limited** database user inside the jar (it can only read and save scores, see `docs/supabase_game_user.sql`)

---

## 🎵 Music

- Each screen has its own song: the menu, F1 Racing and Battle Tanks
- The **Music** volume slider and the **Mute** button are on the main menu and count for all three
- The setting is remembered (`settings.properties` in the `.minigames` folder)
- All songs are CC0 (public domain) from OpenGameArt, see `resources/music/CREDITS.txt

---

## 📁 Project Structure

```
Minigames/
├── src/
│   ├── f1/              # F1 Racing game
│   ├── battletanks/     # Tank Battle game
│   └── common/          # Main menu, scores screen, database
├── resources/
│   ├── textures/
│   │   ├── f1/          # F1 images
│   │   └── battletanks/ # Tank Battle images
│   └── music/           # Menu, F1 and Tank songs (WAV) + CREDITS.txt
├── lib/                 # PostgreSQL + SQLite JDBC drivers
├── docs/
│   ├── screenshots/     # Images used in this README
│   └── supabase_game_user.sql  # the limited database user for builds
├── build.sh             # makes the player zips (Windows, Linux, Mac)
├── HOW_TO_PLAY.txt      # goes into every zip
├── config.example.properties
└── README.md
```

---

## 📦 Play (no setup)

Download the zip for your computer from the GitHub Releases page, unzip it and double-click:

| Computer | Zip | Start |
|---|---|---|
| Windows | `Minigames-windows-x64.zip` | `Minigames.bat` |
| Linux | `Minigames-linux-x64.zip` | `Minigames.sh` |
| Mac (Apple Silicon) | `Minigames-mac-arm64.zip` | `Minigames.app` |
| Mac (Intel) | `Minigames-mac-x64.zip` | `Minigames.app` |

Java 21 is included, nothing to install. The game connects to the online leaderboard by itself. `HOW_TO_PLAY.txt` in the zip explains the first start on a Mac.

---

## ⚙️ Setup (for developers)

### Requirements
- Java 21 or newer
- VS Code with the Java Extension Pack (recommended)

### 1. Clone the repo
```bash
git clone https://github.com/Vice20-05/Minigames.git
cd Minigames
git switch dev
```

### 2. Set up the database connection
Copy `config.example.properties`, rename the copy to `config.properties` (same folder as this README) and fill in the real values:

```properties
db.url=jdbc:postgresql://<host>:5432/postgres
db.user=postgres.<project_ref>
db.password=<password>
```

> ⚠️ **Never commit `config.properties`.** It's already in `.gitignore`. Ask the project manager for the credentials.

### 3. Run it
Open the folder in VS Code and run **Launch Main Menu** (or press `Ctrl+Shift+B`).

The main menu has four options: **F1 Game**, **Battle Tanks**, **Scores** and **Quit**, plus the music volume and mute button.

### 4. Build the player zips
```bash
./build.sh
```
It makes `dist/Minigames.jar` and one zip per computer type with Java 21 included (the Java runtimes are downloaded once into `build/jre`). On Windows, run it in Git Bash. `build/` and `dist/` are in `.gitignore`: upload the zips as a GitHub Release, don't commit them.

If `config.properties` exists, a copy goes inside the jar so players are online without any setup. Anyone can unzip a jar, so for builds you share, `config.properties` must use the **limited game user** from `docs/supabase_game_user.sql`, never the `postgres` admin password (`build.sh` warns you).

---

## 🌿 Branches

| Branch | Purpose |
|---|---|
| `main` | Stable, final version |
| `dev` | Working version, everything gets merged here first |
| feature branches | One per feature, merged into `dev` through pull requests |

---

## ✅ Requirements Checklist

| Requirement | Status |
|---|---|
| F1 Racing game mode (Java Swing) | ✅ Done |
| Random obstacles and collision avoidance | ✅ Done |
| Tank Battle, 2 players (WASD + Left Shift / Arrows + Right Shift) | ✅ Done |
| Placeable blocks as obstacles and cover | ✅ Done |
| Separate leaderboard per game mode | ✅ Done |
| Scores saved in a database (Supabase) | ✅ Done |
| Leaderboard shows name and score | ✅ Done |
| Global online leaderboard | ✅ Done |
| Core game works without internet | ✅ Done |
| Offline local database (SQLite) | ✅ Done |
