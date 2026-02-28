package com.wildrose.minigameswr;

import com.wildrose.minigameswr.command.MgCommand;
import com.wildrose.minigameswr.generator.VoidChunkGenerator;
import com.wildrose.minigameswr.listener.WorldLoadListener;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * MiniGamesWR – Wild Rose MC mini-games plugin.
 *
 * Features provided by this class:
 *  - Void chunk generator: registered as the default world generator so that
 *    bukkit.yml can point the minigames world at this plugin.
 *  - Spawn safety platform: placed via {@link WorldLoadListener} the first
 *    time (and on every load) of the configured void world.
 *  - /mg command: admin utilities (platform regen, etc.).
 */
public class MiniGamesWR extends JavaPlugin {

    private static MiniGamesWR instance;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        if (getConfig().getBoolean("void-generator.spawn-platform.enabled", true)) {
            getServer().getPluginManager().registerEvents(new WorldLoadListener(this), this);
        }

        MgCommand mgCommand = new MgCommand(this);
        getCommand("mg").setExecutor(mgCommand);
        getCommand("mg").setTabCompleter(mgCommand);

        getLogger().info("MiniGamesWR enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("MiniGamesWR disabled.");
    }

    /**
     * Returns the void chunk generator when the void-generator feature is
     * enabled.  This method is called by Bukkit/Paper when bukkit.yml lists
     * this plugin as the generator for a world.
     *
     * <p>To activate, add the following to {@code bukkit.yml}:
     * <pre>
     * worlds:
     *   world:
     *     generator: MiniGamesWR
     * </pre>
     */
    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        if (getConfig().getBoolean("void-generator.enabled", true)) {
            return new VoidChunkGenerator();
        }
        return null;
    }

    public static MiniGamesWR getInstance() {
        return instance;
    }
}
