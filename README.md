# MiniGamesWR

A [Paper](https://papermc.io/) plugin for **Minecraft 1.21.1** powering the Wild Rose MC minigames server.  
Adds two minigames: **Duels (1v1)** and **Spliff**.

---

## Requirements

| Component | Version |
|-----------|---------|
| Minecraft / Paper | 1.21.1 |
| Java | 21+ |
| Server type | Paper (not Spigot / Vanilla) |

---

## Installation (Windows VPS)

1. **Build the jar** (requires Java 21 and internet access to the Paper Maven repository):
   ```bat
   gradlew.bat build
   ```
   Output: `build\libs\MiniGamesWR-1.0.0.jar`

2. **Copy the jar** to the minigames server plugins folder:
   ```
   C:\Minecraft\minigames\plugins\MiniGamesWR-1.0.0.jar
   ```

3. **Restart** the minigames server.

4. A `plugins\MiniGamesWR\config.yml` will be generated automatically.

---

## Quick-Start Setup

After installation, log in with an OP account and run the following commands **in-game** to configure each location.

### Hub Spawn
Stand where you want players to spawn when they join, then:
```
/mg sethub
```

### Duels Arena
Stand at duel spawn A, then:
```
/mg duel setspawn a
```
Stand at duel spawn B, then:
```
/mg duel setspawn b
```

### Spliff Arena
Add spawn points (repeat for each spawn, up to 8+):
```
/mg spliff addspawn
```

Set the two corners of the rectangular arena platform:
```
/mg spliff setpos1
/mg spliff setpos2
```

Reload config after any manual edits:
```
/mg reload
```

---

## How to Play

### Opening the Minigames Menu
On join, every player receives a **Nether Star** (★) in hotbar slot 0 titled *Wild Rose Minigames*.  
Right-click it to open the menu.

### Duels (1v1)
- Click **Duels (1v1)** in the menu.
- First player waits in queue; second player starts the match.
- Both players are teleported to the arena, given the **Classic kit** (iron armour + iron sword + 16 steak), and a 5-second countdown begins.
- Last player standing wins (opponent dies or disconnects).
- Winner is announced; both players return to hub 3 seconds later.

### Spliff
- Click **Spliff** in the menu.
- Match begins when ≥ 2 players are queued.
- Each player is teleported to a configured spawn point.
- After a 5-second countdown, players may **break** only the configured spliff block (default: `SNOW_BLOCK`) — placing blocks is forbidden.
- A player is eliminated when they fall below the configured Y level (default: `0`).
- Last player alive wins.
- Arena is **batch-reset** (refilled) after each match without freezing the server.
- All players return to hub 3 seconds after match ends.

---

## Configuration (`config.yml`)

```yaml
hub:
  world: world
  x: 0.5
  y: 64.0
  z: 0.5
  yaw: 0.0
  pitch: 0.0

duels:
  world: world
  spawnA: { x: 10.5, y: 64.0, z: 0.5, yaw: 90.0, pitch: 0.0 }
  spawnB: { x: -10.5, y: 64.0, z: 0.5, yaw: 270.0, pitch: 0.0 }

spliff:
  world: world
  block: SNOW_BLOCK     # Material players may break
  lose-y: 0             # Players below this Y are eliminated
  pos1: { x: -20, y: 63, z: -20 }
  pos2: { x: 20,  y: 63, z: 20  }
  spawnpoints: []       # Populated by /mg spliff addspawn
```

---

## Admin Commands

Permission: `minigameswr.admin` (default: OP)

| Command | Description |
|---------|-------------|
| `/mg sethub` | Set hub spawn to current position |
| `/mg duel setspawn <a\|b>` | Set duel arena spawn A or B |
| `/mg spliff addspawn` | Add a Spliff spawn point |
| `/mg spliff setpos1` | Set Spliff arena corner 1 |
| `/mg spliff setpos2` | Set Spliff arena corner 2 |
| `/mg reload` | Reload `config.yml` |

---

## Project Structure

```
src/main/java/com/wildrose/minigameswr/
├── MiniGamesWR.java          Main plugin class
├── config/ConfigManager.java Config read/write helpers
├── gui/MinigamesGUI.java     Inventory GUI (menu + click handling)
├── hub/HubListener.java      Join teleport, GUI item management
├── duels/
│   ├── DuelsManager.java     Queue, countdown, kit, win/lose logic
│   └── DuelsListener.java    Death + quit events
└── spliff/
    ├── SpliffManager.java    Queue, countdown, void check, arena reset
    └── SpliffListener.java   Block break/place + death + quit events
src/main/resources/
├── plugin.yml
└── config.yml
```