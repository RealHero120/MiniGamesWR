package com.wildrose.minigameswr.config;

import com.wildrose.minigameswr.MiniGamesWR;
import com.wildrose.minigameswr.spleef.SpleefArena;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private final MiniGamesWR plugin;

    public ConfigManager(MiniGamesWR plugin) {
        this.plugin = plugin;
    }

    private FileConfiguration cfg() {
        return plugin.getConfig();
    }

    // ---- Hub ----

    public Location getHubSpawn() {
        return readLocation("hub");
    }

    public void setHubSpawn(Location loc) {
        writeLocation("hub", loc);
        plugin.saveConfig();
    }

    // ---- Duels ----

    public Location getDuelSpawnA() {
        return readLocation("duels.spawnA", "duels.world");
    }

    public Location getDuelSpawnB() {
        return readLocation("duels.spawnB", "duels.world");
    }

    public void setDuelSpawn(String which, Location loc) {
        String key = "duels.spawn" + which.toUpperCase();
        writeLocationWithSharedWorld(key, "duels.world", loc);
        plugin.saveConfig();
    }

    public ItemStack getDuelsKitArmorPiece(String slot, String defaultMaterial) {
        String name = cfg().getString("duels.kit." + slot, defaultMaterial);
        try {
            return new ItemStack(Material.valueOf(name.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return new ItemStack(Material.valueOf(defaultMaterial));
        }
    }

    public Map<Integer, ItemStack> getDuelsKitItems() {
        Map<Integer, ItemStack> items = new LinkedHashMap<>();
        ConfigurationSection section = cfg().getConfigurationSection("duels.kit.items");
        if (section == null) {
            items.put(0, new ItemStack(Material.IRON_SWORD));
            items.put(1, new ItemStack(Material.COOKED_BEEF, 16));
            return items;
        }
        for (String key : section.getKeys(false)) {
            try {
                int slot = Integer.parseInt(key);
                String materialName = section.getString(key + ".material", "AIR");
                int amount = section.getInt(key + ".amount", 1);
                items.put(slot, new ItemStack(Material.valueOf(materialName.toUpperCase()), amount));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid duels kit item at slot '" + key + "': " + e.getMessage());
            }
        }
        return items;
    }

    // ---- Spliff ----

    public String getSpliffWorld() {
        return cfg().getString("spliff.world", "world");
    }

    public List<Location> getSpliffSpawnpoints() {
        List<Location> result = new ArrayList<>();
        List<?> list = cfg().getList("spliff.spawnpoints");
        if (list == null) return result;
        World world = Bukkit.getWorld(getSpliffWorld());
        for (Object obj : list) {
            if (obj instanceof java.util.Map<?, ?> map) {
                try {
                    double x = toDouble(map.get("x"));
                    double y = toDouble(map.get("y"));
                    double z = toDouble(map.get("z"));
                    float yaw = (float) toDouble(map.get("yaw"));
                    float pitch = (float) toDouble(map.get("pitch"));
                    result.add(new Location(world, x, y, z, yaw, pitch));
                } catch (Exception ignored) {}
            }
        }
        return result;
    }

    public void addSpliffSpawnpoint(Location loc) {
        List<java.util.Map<String, Object>> list = new ArrayList<>();
        // Preserve existing
        for (Location existing : getSpliffSpawnpoints()) {
            list.add(serializeLoc(existing));
        }
        list.add(serializeLoc(loc));
        cfg().set("spliff.spawnpoints", list);
        cfg().set("spliff.world", loc.getWorld().getName());
        plugin.saveConfig();
    }

    public org.bukkit.Material getSpliffBlock() {
        String name = cfg().getString("spliff.block", "SNOW_BLOCK");
        try {
            return org.bukkit.Material.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return org.bukkit.Material.SNOW_BLOCK;
        }
    }

    public double getSpliffLoseY() {
        return cfg().getDouble("spliff.lose-y", 0.0);
    }

    public Location getSpliffPos1() {
        String worldName = getSpliffWorld();
        World world = Bukkit.getWorld(worldName);
        if (!cfg().contains("spliff.pos1")) return null;
        double x = cfg().getDouble("spliff.pos1.x");
        double y = cfg().getDouble("spliff.pos1.y");
        double z = cfg().getDouble("spliff.pos1.z");
        return new Location(world, x, y, z);
    }

    public Location getSpliffPos2() {
        String worldName = getSpliffWorld();
        World world = Bukkit.getWorld(worldName);
        if (!cfg().contains("spliff.pos2")) return null;
        double x = cfg().getDouble("spliff.pos2.x");
        double y = cfg().getDouble("spliff.pos2.y");
        double z = cfg().getDouble("spliff.pos2.z");
        return new Location(world, x, y, z);
    }

    public void setSpliffPos(int posNum, Location loc) {
        String key = "spliff.pos" + posNum;
        cfg().set(key + ".x", loc.getBlockX());
        cfg().set(key + ".y", loc.getBlockY());
        cfg().set(key + ".z", loc.getBlockZ());
        cfg().set("spliff.world", loc.getWorld().getName());
        plugin.saveConfig();
    }

    // ---- Spleef arenas ----

    /**
     * Load all Spleef arenas stored under {@code spleef.arenas.*} in config.yml.
     * Returns a list of arenas with their configuration populated; snapshots must
     * be loaded separately by the caller.
     */
    public List<SpleefArena> loadSpleefArenas() {
        List<SpleefArena> result = new ArrayList<>();
        ConfigurationSection arenasSection = cfg().getConfigurationSection("spleef.arenas");
        if (arenasSection == null) return result;
        for (String name : arenasSection.getKeys(false)) {
            SpleefArena arena = new SpleefArena(name);
            String world = arenasSection.getString(name + ".world");
            arena.setWorldName(world);

            // pos1 / pos2
            if (arenasSection.contains(name + ".pos1")) {
                double x = arenasSection.getDouble(name + ".pos1.x");
                double y = arenasSection.getDouble(name + ".pos1.y");
                double z = arenasSection.getDouble(name + ".pos1.z");
                arena.setPos1(new Location(Bukkit.getWorld(world != null ? world : "world"), x, y, z));
            }
            if (arenasSection.contains(name + ".pos2")) {
                double x = arenasSection.getDouble(name + ".pos2.x");
                double y = arenasSection.getDouble(name + ".pos2.y");
                double z = arenasSection.getDouble(name + ".pos2.z");
                arena.setPos2(new Location(Bukkit.getWorld(world != null ? world : "world"), x, y, z));
            }

            // Spawn points
            List<?> spawns = arenasSection.getList(name + ".spawnpoints");
            if (spawns != null) {
                World w = Bukkit.getWorld(world != null ? world : "world");
                for (Object obj : spawns) {
                    if (obj instanceof Map<?, ?> map) {
                        try {
                            double x = toDouble(map.get("x"));
                            double y = toDouble(map.get("y"));
                            double z = toDouble(map.get("z"));
                            float yaw = (float) toDouble(map.get("yaw"));
                            float pitch = (float) toDouble(map.get("pitch"));
                            arena.addSpawnpoint(new Location(w, x, y, z, yaw, pitch));
                        } catch (Exception ignored) {}
                    }
                }
            }

            // Optional lobby
            if (arenasSection.contains(name + ".lobby")) {
                String lobbyWorld = arenasSection.getString(name + ".lobby.world", world != null ? world : "world");
                double x = arenasSection.getDouble(name + ".lobby.x");
                double y = arenasSection.getDouble(name + ".lobby.y");
                double z = arenasSection.getDouble(name + ".lobby.z");
                float yaw = (float) arenasSection.getDouble(name + ".lobby.yaw");
                float pitch = (float) arenasSection.getDouble(name + ".lobby.pitch");
                arena.setLobby(new Location(Bukkit.getWorld(lobbyWorld), x, y, z, yaw, pitch));
            }

            result.add(arena);
        }
        return result;
    }

    /**
     * Persist a Spleef arena's current configuration to config.yml (auto-saves).
     */
    public void saveSpleefArena(SpleefArena arena) {
        String base = "spleef.arenas." + arena.getName();
        if (arena.getWorldName() != null) cfg().set(base + ".world", arena.getWorldName());

        if (arena.getPos1() != null) {
            cfg().set(base + ".pos1.x", arena.getPos1().getBlockX());
            cfg().set(base + ".pos1.y", arena.getPos1().getBlockY());
            cfg().set(base + ".pos1.z", arena.getPos1().getBlockZ());
        }
        if (arena.getPos2() != null) {
            cfg().set(base + ".pos2.x", arena.getPos2().getBlockX());
            cfg().set(base + ".pos2.y", arena.getPos2().getBlockY());
            cfg().set(base + ".pos2.z", arena.getPos2().getBlockZ());
        }

        List<Map<String, Object>> spawns = new ArrayList<>();
        for (Location loc : arena.getSpawnpoints()) {
            spawns.add(serializeLoc(loc));
        }
        cfg().set(base + ".spawnpoints", spawns);

        if (arena.getLobby() != null) {
            Location l = arena.getLobby();
            cfg().set(base + ".lobby.world", l.getWorld() != null ? l.getWorld().getName() : arena.getWorldName());
            cfg().set(base + ".lobby.x", l.getX());
            cfg().set(base + ".lobby.y", l.getY());
            cfg().set(base + ".lobby.z", l.getZ());
            cfg().set(base + ".lobby.yaw", (double) l.getYaw());
            cfg().set(base + ".lobby.pitch", (double) l.getPitch());
        }

        plugin.saveConfig();
    }

    // ---- Helpers ----

    private Location readLocation(String section) {
        String worldName = cfg().getString(section + ".world", "world");
        World world = Bukkit.getWorld(worldName);
        double x = cfg().getDouble(section + ".x", 0.5);
        double y = cfg().getDouble(section + ".y", 64);
        double z = cfg().getDouble(section + ".z", 0.5);
        float yaw = (float) cfg().getDouble(section + ".yaw", 0);
        float pitch = (float) cfg().getDouble(section + ".pitch", 0);
        return new Location(world, x, y, z, yaw, pitch);
    }

    private Location readLocation(String section, String worldKey) {
        String worldName = cfg().getString(worldKey, "world");
        World world = Bukkit.getWorld(worldName);
        double x = cfg().getDouble(section + ".x", 0.5);
        double y = cfg().getDouble(section + ".y", 64);
        double z = cfg().getDouble(section + ".z", 0.5);
        float yaw = (float) cfg().getDouble(section + ".yaw", 0);
        float pitch = (float) cfg().getDouble(section + ".pitch", 0);
        return new Location(world, x, y, z, yaw, pitch);
    }

    private void writeLocation(String section, Location loc) {
        cfg().set(section + ".world", loc.getWorld().getName());
        cfg().set(section + ".x", loc.getX());
        cfg().set(section + ".y", loc.getY());
        cfg().set(section + ".z", loc.getZ());
        cfg().set(section + ".yaw", (double) loc.getYaw());
        cfg().set(section + ".pitch", (double) loc.getPitch());
    }

    private void writeLocationWithSharedWorld(String section, String worldKey, Location loc) {
        cfg().set(worldKey, loc.getWorld().getName());
        cfg().set(section + ".x", loc.getX());
        cfg().set(section + ".y", loc.getY());
        cfg().set(section + ".z", loc.getZ());
        cfg().set(section + ".yaw", (double) loc.getYaw());
        cfg().set(section + ".pitch", (double) loc.getPitch());
    }

    private java.util.Map<String, Object> serializeLoc(Location loc) {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("x", loc.getX());
        map.put("y", loc.getY());
        map.put("z", loc.getZ());
        map.put("yaw", (double) loc.getYaw());
        map.put("pitch", (double) loc.getPitch());
        return map;
    }

    private double toDouble(Object o) {
        if (o == null) return 0;
        if (o instanceof Number n) return n.doubleValue();
        return Double.parseDouble(o.toString());
    }
}
