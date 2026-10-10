# Mini-Games Project – Planning Notes

Two small 2D games in Java Swing, launched from one main window.
Status: planning phase, no code yet.

## Who does what
- **Art (you):** art style, sprites, cars, tanks, walls, backgrounds.
- **Engineering (me):** game logic, structure, database.
- Until the art is ready I'll use placeholder shapes (colored boxes). Each object just has an image path, so your art gets dropped in later without changing any logic.

## Build order
1. Game logic / object model first (cars, tanks, bullets, walls, collisions )
2. Rendering with placeholders
3. Database last (it's small)
    4.Art , the phase of overwriting the placeholders 

---

## Game 1 – Top-down traffic racer (single player)
Teacher-approved: 2D only, top-down view, no 3D.

**Idea:** you drive through traffic as fast as you dare and score points for risky driving (inspired by traffic runs in BeamNG / Assetto Corsa).

**How it works**
- The car stays near the bottom of the screen; the road scrolls underneath to create the feeling of speed.
- Left/right to change lanes, player controls their own speed (throttle/brake).
- Traffic is **not AI** – cars just spawn in random lanes on a pattern and move down the screen
    -The player using W accelerates the car, s is for brake 

**Scoring**
- Points over time, multiplied by speed (e.g. 1.5x at 100 km/h, 2x at 200 km/h, capped).
- Bonus for **near misses**: a slightly bigger invisible box around the player's car. Traffic touches the big box but not the car = near miss. Touches the car = crash.
- Bonus for overtakes.

**Difficulty**
- One game timer drives everything (no separate timers).
- Phases: traffic slowly gets denser (e.g. every ~30s), and after ~2 min roadblocks / potholes start appearing.
- Difficulty **caps** at a maximum so it never becomes a wall of cars.
- Spawning always leaves **at least one open lane**, so the game is always beatable.
- Since the player controls speed, they can slow down to get through dense traffic.

**Stretch goal (only if we have time):** unlockable cars, from slow ones up to a Koenigsegg Jesko Absolut.

---

## Game 2 – Tank battle (2 players, same keyboard)

**Controls**
- Player 1: WASD to move, Left Shift to shoot
- Player 2: Arrow keys to move, Right Shift to shoot

**Build phase (inspired by "Make Way")**
- At the start of a round each player gets a random set of predefined objects (walls etc.).
- They place them anywhere in the arena. Overlapping is not allowed.

**Combat**
- Bullets **bounce off walls**, so you can bank shots around cover to hit someone hiding.
- Each player has **3 lives**.
- After a life is lost, a new phase starts and another object gets placed, so the arena keeps changing.

**Destructible walls**
- A wall looks like one solid rectangle, but internally it's **3 small blocks** (like 3 Minecraft blocks side by side).
- Each block has its own health and shrinks a bit when hit, then disappears – so the hole opens exactly where the bullet hit.
- Any bullet damages any wall (exept the main walls whehre the tanks live), including your own.
- This stops players from walling themselves in forever (turtling).
- Rejected: a "shoot through walls" ability – too overpowered.

---

## Database (small, done last)
- Player name, high scores, leaderboard placement.
- Possibly the catalog of placeable objects (name, image path, size, solid/destructible) so new objects can be added as data instead of code. Behavior stays in code.
- Which database we use: still checking the list the teacher allowed.
