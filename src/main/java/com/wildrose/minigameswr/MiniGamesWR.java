package com.wildrose.minigameswr;

import com.wildrose.minigameswr.commands.MgCommand;
import com.wildrose.minigameswr.config.ConfigManager;
import com.wildrose.minigameswr.duels.DuelsListener;
import com.wildrose.minigameswr.duels.DuelsManager;
import com.wildrose.minigameswr.gui.MinigamesGUI;
import com.wildrose.minigameswr.hub.HubListener;
import com.wildrose.minigameswr.spliff.SpliffListener;
import com.wildrose.minigameswr.spliff.SpliffManager;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

public final class MiniGamesWR extends JavaPlugin {

    private ConfigManager configManager;
    private DuelsManager duelsManager;
    private SpliffManager spliffManager;
    private MinigamesGUI minigamesGUI;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        configManager = new ConfigManager(this);
        duelsManager = new DuelsManager(this);
        spliffManager = new SpliffManager(this);
        minigamesGUI = new MinigamesGUI(this);

        SpawnPlatformListener spawnPlatformListener = new SpawnPlatformListener(this);
        getServer().getPluginManager().registerEvents(new HubListener(this), this);
        getServer().getPluginManager().registerEvents(new DuelsListener(this), this);
        getServer().getPluginManager().registerEvents(new SpliffListener(this), this);
        getServer().getPluginManager().registerEvents(minigamesGUI, this);
        getServer().getPluginManager().registerEvents(spawnPlatformListener, this);

        // Place spawn platform in any worlds already loaded at enable time.
        String voidWorldName = getConfig().getString("void-generator.world-name", "world");
        World voidWorld = getServer().getWorld(voidWorldName);
        if (voidWorld != null) {
            spawnPlatformListener.tryPlacePlatform(voidWorld);
        }

        PluginCommand mgCmd = getCommand("mg");
        if (mgCmd == null) {
            getLogger().severe("Command 'mg' is not registered in plugin.yml — executor and tab-completer not set!");
        } else {
            MgCommand mgCommand = new MgCommand(this);
            mgCmd.setExecutor(mgCommand);
            mgCmd.setTabCompleter(mgCommand);
        }

        getLogger().info("MiniGamesWR enabled!");
    }

    @Override
    public void onDisable() {
        if (duelsManager != null) duelsManager.shutdown();
        if (spliffManager != null) spliffManager.shutdown();
        getLogger().info("MiniGamesWR disabled.");
    }

    /**
     * Returns {@link VoidChunkGenerator} only when both conditions are true:
     * <ol>
     *   <li>{@code void-generator.enabled} is {@code true} in config.yml</li>
     *   <li>{@code worldName} matches {@code void-generator.world-name}</li>
     * </ol>
     * Otherwise returns {@code null} so Bukkit uses normal terrain generation.
     * This method is called by the server before {@link #onEnable}, so it reads
     * directly from the plugin's config (loaded lazily on first access).
     */
    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        if (!getConfig().getBoolean("void-generator.enabled", false)) {
            return null;
        }
        String voidWorldName = getConfig().getString("void-generator.world-name", "world");
        if (!worldName.equals(voidWorldName)) {
            getLogger().info("VoidChunkGenerator not applied to world '" + worldName
                    + "' (configured for '" + voidWorldName + "').");
            return null;
        }
        getLogger().info("Applying VoidChunkGenerator to world '" + worldName + "'.");
        return new VoidChunkGenerator();
    }

    public ConfigManager getConfigManager() { return configManager; }
    public DuelsManager getDuelsManager() { return duelsManager; }
    public SpliffManager getSpliffManager() { return spliffManager; }
    public MinigamesGUI getMinigamesGUI() { return minigamesGUI; }
}
