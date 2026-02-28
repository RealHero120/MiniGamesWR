package com.wildrose.minigameswr.spleef;

import org.bukkit.Location;

import java.util.*;

/**
 * Holds configuration and live match state for a single Spleef arena.
 * Configuration (positions, spawns, lobby) is persisted via {@link com.wildrose.minigameswr.config.ConfigManager}.
 * The block-state snapshot is persisted via {@link ArenaSnapshot}.
 */
public class SpleefArena {

    private final String name;
    private String worldName;
    private Location pos1;
    private Location pos2;
    private final List<Location> spawnpoints = new ArrayList<>();
    /** Optional per-arena return location; falls back to hub spawn when {@code null}. */
    private Location lobby;
    private final ArenaSnapshot snapshot = new ArenaSnapshot();

    // ---- Live match state ----
    private final List<UUID> queue = new ArrayList<>();
    private final Set<UUID> inMatch = new HashSet<>();
    private final Set<UUID> eliminated = new HashSet<>();
    private final List<UUID> matchPlayers = new ArrayList<>();
    private boolean matchRunning = false;
    /** True while a batch reset is in progress — prevents a second match from starting. */
    private boolean resetting = false;

    public SpleefArena(String name) {
        this.name = name;
    }

    // ---- Configuration ----

    public String getName() { return name; }

    public String getWorldName() { return worldName; }
    public void setWorldName(String worldName) { this.worldName = worldName; }

    public Location getPos1() { return pos1; }
    public void setPos1(Location pos1) { this.pos1 = pos1; }

    public Location getPos2() { return pos2; }
    public void setPos2(Location pos2) { this.pos2 = pos2; }

    public List<Location> getSpawnpoints() { return spawnpoints; }

    public void addSpawnpoint(Location loc) { spawnpoints.add(loc); }

    public void setSpawnpoint(int index, Location loc) {
        while (spawnpoints.size() <= index) spawnpoints.add(loc);
        spawnpoints.set(index, loc);
    }

    public Location getLobby() { return lobby; }
    public void setLobby(Location lobby) { this.lobby = lobby; }

    public ArenaSnapshot getSnapshot() { return snapshot; }

    /**
     * Returns {@code true} when the arena is fully configured and ready to host matches:
     * pos1, pos2, at least two spawn points, and a saved snapshot.
     */
    public boolean isConfigured() {
        return worldName != null
                && pos1 != null
                && pos2 != null
                && spawnpoints.size() >= 2
                && snapshot.isLoaded();
    }

    /**
     * Returns {@code true} if the given location falls within the arena cuboid region.
     * Used for block protection checks.
     */
    public boolean contains(Location loc) {
        if (pos1 == null || pos2 == null || loc.getWorld() == null) return false;
        if (!loc.getWorld().getName().equals(worldName)) return false;
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        int bx = loc.getBlockX(), by = loc.getBlockY(), bz = loc.getBlockZ();
        return bx >= minX && bx <= maxX && by >= minY && by <= maxY && bz >= minZ && bz <= maxZ;
    }

    // ---- Match state ----

    public List<UUID> getQueue() { return queue; }
    public Set<UUID> getInMatch() { return inMatch; }
    public Set<UUID> getEliminated() { return eliminated; }
    public List<UUID> getMatchPlayers() { return matchPlayers; }

    public boolean isMatchRunning() { return matchRunning; }
    public void setMatchRunning(boolean matchRunning) { this.matchRunning = matchRunning; }

    public boolean isResetting() { return resetting; }
    public void setResetting(boolean resetting) { this.resetting = resetting; }
}
