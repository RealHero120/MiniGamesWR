package com.wildrose.minigameswr;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

/**
 * Listens for world load events and places the configured spawn platform in
 * the void world so players always have a safe landing spot at spawn.
 */
public class SpawnPlatformListener implements Listener {

    private final MiniGamesWR plugin;

    public SpawnPlatformListener(MiniGamesWR plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        tryPlacePlatform(event.getWorld());
    }

    /**
     * Places an NxN spawn platform in {@code world} if it matches the configured
     * void-generator world and platform placement is enabled.
     *
     * <p>Centering rule (integer math, works for both odd and even sizes):
     * <pre>
     *   startX = centerX - (size - 1) / 2
     *   endX   = startX + size - 1
     * </pre>
     * Examples:
     * <ul>
     *   <li>size=4: startX = center-1, blocks at center-1, center, center+1, center+2 → 4 blocks</li>
     *   <li>size=5: startX = center-2, blocks at center-2 … center+2 → 5 blocks</li>
     * </ul>
     */
    public void tryPlacePlatform(World world) {
        FileConfiguration cfg = plugin.getConfig();
        if (!cfg.getBoolean("void-generator.enabled", false)) return;

        String voidWorldName = cfg.getString("void-generator.world-name", "world");
        if (!world.getName().equals(voidWorldName)) return;

        if (!cfg.getBoolean("void-generator.spawn-platform.enabled", true)) return;

        int size = cfg.getInt("void-generator.spawn-platform.size", 5);
        if (size <= 0) {
            plugin.getLogger().warning("void-generator.spawn-platform.size must be > 0; skipping platform placement.");
            return;
        }

        String materialName = cfg.getString("void-generator.spawn-platform.material", "STONE");
        Material material;
        try {
            material = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid platform material '" + materialName + "', defaulting to STONE.");
            material = Material.STONE;
        }

        // Hub spawn coordinates determine the platform centre.
        int centerX = (int) Math.floor(cfg.getDouble("hub.x", 0.5));
        int centerZ = (int) Math.floor(cfg.getDouble("hub.z", 0.5));
        // Place the platform one block below the player's spawn Y so they land on it.
        int platformY = (int) Math.floor(cfg.getDouble("hub.y", 64.0)) - 1;

        // startX/Z calculated with integer division so the grid is exactly size×size.
        int startX = centerX - (size - 1) / 2;
        int startZ = centerZ - (size - 1) / 2;

        int placed = 0;
        for (int dx = 0; dx < size; dx++) {
            for (int dz = 0; dz < size; dz++) {
                Block block = world.getBlockAt(startX + dx, platformY, startZ + dz);
                // Only fill air variants — do not overwrite existing blocks (idempotent).
                Material type = block.getType();
                if (type == Material.AIR || type == Material.CAVE_AIR || type == Material.VOID_AIR) {
                    block.setType(material);
                    placed++;
                }
            }
        }

        if (placed > 0) {
            plugin.getLogger().info("Placed " + placed + " block(s) for " + size + "×" + size
                    + " spawn platform at y=" + platformY + " in world '" + voidWorldName + "'.");
        } else {
            plugin.getLogger().info("Spawn platform in world '" + voidWorldName + "' is already in place.");
        }
    }
}
