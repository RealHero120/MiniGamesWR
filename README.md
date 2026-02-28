# MiniGamesWR

Wild Rose MC mini-games plugin for Paper 1.21 (`mc1.wildrose.com`).

## Features

- **Void world generator** – generates a completely empty (air) world suitable for
  minigame islands and arenas.  No terrain, caves, structures, or mob-spawning.
- **Spawn safety platform** – a small configurable platform is placed at world
  spawn so players do not fall into the void on first join.
- **`/mg` admin command** – re-create the platform at any time.

---

## One-time void-world setup

> **Do this once** before the first server start.  Players will see a fresh
> void world on next connection.

1. **Stop** the minigames server.
2. **Back up** (or delete) the existing world folder, e.g.:
   ```
   C:\Minecraft\minigames\world  →  C:\Minecraft\minigames\world_backup
   ```
3. Edit **`bukkit.yml`** (in `C:\Minecraft\minigames\`) and add:
   ```yaml
   worlds:
     world:
       generator: MiniGamesWR
   ```
4. **Start** the server.  A fresh void world is generated automatically.
5. *(Optional)* Set a world border to prevent limitless void chunk generation:
   ```
   /worldborder center 0 0
   /worldborder set 1000
   ```

---

## Configuration (`plugins/MiniGamesWR/config.yml`)

```yaml
void-generator:
  enabled: true          # master switch for void generation
  world-name: world      # folder name of the world to keep void

  spawn-platform:
    enabled: true        # place safety platform at spawn on world load
    size: 3              # NxN block square (default 3 = 3×3)
    material: GLASS      # any valid Bukkit Material name
    y: -1                # -1 = auto (spawnY - 1); or set an exact Y value
```

All options take effect on the **next server restart** (world load).

---

## Commands

| Command | Permission | Description |
|---------|-----------|-------------|
| `/mg platform regen` | `minigameswr.admin` | Re-place the spawn safety platform |
| `/mg voidplatform`   | `minigameswr.admin` | Alias for the above |

Permissions default to **op**.

---

## Building

Requirements: Java 17+, Maven 3.x.

```bash
mvn clean package
```

The compiled jar is placed in `target/MiniGamesWR-1.0.0.jar`.
Copy it to `C:\Minecraft\minigames\plugins\`.