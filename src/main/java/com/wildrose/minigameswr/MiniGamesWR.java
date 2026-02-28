package com.wildrose.minigameswr;

import com.wildrose.minigameswr.commands.MgCommand;
import com.wildrose.minigameswr.config.ConfigManager;
import com.wildrose.minigameswr.duels.DuelsListener;
import com.wildrose.minigameswr.duels.DuelsManager;
import com.wildrose.minigameswr.gui.MinigamesGUI;
import com.wildrose.minigameswr.hub.HubListener;
import com.wildrose.minigameswr.spliff.SpliffListener;
import com.wildrose.minigameswr.spliff.SpliffManager;
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

        getServer().getPluginManager().registerEvents(new HubListener(this), this);
        getServer().getPluginManager().registerEvents(new DuelsListener(this), this);
        getServer().getPluginManager().registerEvents(new SpliffListener(this), this);
        getServer().getPluginManager().registerEvents(minigamesGUI, this);

        MgCommand mgCommand = new MgCommand(this);
        getCommand("mg").setExecutor(mgCommand);
        getCommand("mg").setTabCompleter(mgCommand);

        getLogger().info("MiniGamesWR enabled!");
    }

    @Override
    public void onDisable() {
        if (duelsManager != null) duelsManager.shutdown();
        if (spliffManager != null) spliffManager.shutdown();
        getLogger().info("MiniGamesWR disabled.");
    }

    public ConfigManager getConfigManager() { return configManager; }
    public DuelsManager getDuelsManager() { return duelsManager; }
    public SpliffManager getSpliffManager() { return spliffManager; }
    public MinigamesGUI getMinigamesGUI() { return minigamesGUI; }
}
