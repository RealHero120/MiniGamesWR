package com.wildrose.minigameswr.listener;

import com.wildrose.minigameswr.MiniGamesWR;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

/**
 * Listens for the target void world to load and places a small spawn-safety
 * platform so that players do not fall into the void immediately on join.
 *
 * <p>The platform is idempotent: blocks that are already non-air are left
 * untouched, so the command {@code /mg platform regen} is safe to run at
 * any time without overwriting arena structures.
 */
public class WorldLoadListener implements Listener {

    private final MiniGamesWR plugin;

    public WorldLoadListener(MiniGamesWR plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        World world = event.getWorld();
        String targetWorld = plugin.getConfig().getString("void-generator.world-name", "world");
        if (!world.getName().equals(targetWorld)) {
            return;
        }
        // Defer one tick so the world is fully ready before we set blocks.
        plugin.getServer().getScheduler().runTask(plugin, () -> placePlatform(world));
    }

    /**
     * Places (or refreshes) the spawn-safety platform in the given world
     * according to the current plugin configuration.
     *
     * @param world the world in which to place the platform
     */
    public void placePlatform(World world) {
        int size = plugin.getConfig().getInt("void-generator.spawn-platform.size", 3);

        String materialName = plugin.getConfig()
                .getString("void-generator.spawn-platform.material", "GLASS");
        Material material;
        try {
            material = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning(
                    "Invalid platform material '" + materialName + "', defaulting to GLASS.");
            material = Material.GLASS;
        }

        Location spawnLoc = world.getSpawnLocation();

        int configY = plugin.getConfig().getInt("void-generator.spawn-platform.y", -1);
        int platformY = (configY < 0) ? (spawnLoc.getBlockY() - 1) : configY;

        int centerX = spawnLoc.getBlockX();
        int centerZ = spawnLoc.getBlockZ();
        int halfSize = size / 2;

        for (int x = centerX - halfSize; x <= centerX + halfSize; x++) {
            for (int z = centerZ - halfSize; z <= centerZ + halfSize; z++) {
                Block block = world.getBlockAt(x, platformY, z);
                if (block.getType() == Material.AIR) {
                    block.setType(material);
                }
            }
        }

        plugin.getLogger().info(
                "Spawn safety platform placed at y=" + platformY
                + " in world '" + world.getName() + "'.");
    }
}
