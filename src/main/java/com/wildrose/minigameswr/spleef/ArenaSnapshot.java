package com.wildrose.minigameswr.spleef;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Captures and restores the block state of a cuboid arena region.
 * Each entry stores absolute block coordinates and a BlockData string.
 * Snapshots are saved to and loaded from a plain-text file so they
 * survive server restarts.
 */
public class ArenaSnapshot {

    private final List<int[]> coords = new ArrayList<>();
    private final List<String> blockDataStrings = new ArrayList<>();
    private boolean loaded = false;

    /** Capture the current block state of the given cuboid from the world. */
    public void capture(World world, Location pos1, Location pos2) {
        coords.clear();
        blockDataStrings.clear();
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    coords.add(new int[]{x, y, z});
                    blockDataStrings.add(block.getBlockData().getAsString());
                }
            }
        }
        loaded = !coords.isEmpty();
    }

    /**
     * Save the snapshot to a file.
     * Format: one block per line — {@code x y z blockDataString}
     */
    public void save(File file) throws IOException {
        file.getParentFile().mkdirs();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (int i = 0; i < coords.size(); i++) {
                int[] c = coords.get(i);
                writer.write(c[0] + " " + c[1] + " " + c[2] + " " + blockDataStrings.get(i));
                writer.newLine();
            }
        }
    }

    /**
     * Load a snapshot from a file previously written by {@link #save}.
     * Silently skips malformed lines and logs a warning if the file is unreadable.
     */
    public void load(File file, Logger log) {
        coords.clear();
        blockDataStrings.clear();
        loaded = false;
        if (!file.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                // Split on the first three spaces; the rest is the BlockData string.
                String[] parts = line.split(" ", 4);
                if (parts.length < 4) continue;
                try {
                    int x = Integer.parseInt(parts[0]);
                    int y = Integer.parseInt(parts[1]);
                    int z = Integer.parseInt(parts[2]);
                    coords.add(new int[]{x, y, z});
                    blockDataStrings.add(parts[3]);
                } catch (NumberFormatException e) {
                    log.warning("[Spleef] Skipping malformed snapshot line: " + line);
                }
            }
            loaded = !coords.isEmpty();
        } catch (IOException e) {
            log.warning("[Spleef] Failed to load snapshot from " + file.getName() + ": " + e.getMessage());
        }
    }

    /** Returns {@code true} if a non-empty snapshot is available. */
    public boolean isLoaded() {
        return loaded;
    }

    /**
     * Restore all blocks asynchronously in batches of {@code batchSize} per tick.
     * Runs on the server main thread via the scheduler.
     * @param onComplete optional callback invoked on the main thread when done
     */
    public BukkitTask restore(Plugin plugin, World world, int batchSize, Runnable onComplete) {
        if (!loaded || coords.isEmpty()) {
            if (onComplete != null) onComplete.run();
            return null;
        }
        final int[] index = {0};
        final BukkitTask[] taskHolder = {null};
        taskHolder[0] = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            int end = Math.min(index[0] + batchSize, coords.size());
            for (int i = index[0]; i < end; i++) {
                int[] c = coords.get(i);
                try {
                    BlockData data = Bukkit.createBlockData(blockDataStrings.get(i));
                    world.getBlockAt(c[0], c[1], c[2]).setBlockData(data, false);
                } catch (IllegalArgumentException ignored) {
                    // Skip blocks whose data string is no longer valid.
                }
            }
            index[0] = end;
            if (index[0] >= coords.size()) {
                taskHolder[0].cancel();
                if (onComplete != null) onComplete.run();
            }
        }, 2L /* initial delay ticks */, 1L /* period: run every tick */);
        return taskHolder[0];
    }
}
