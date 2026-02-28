package com.wildrose.minigameswr.command;

import com.wildrose.minigameswr.MiniGamesWR;
import com.wildrose.minigameswr.listener.WorldLoadListener;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Handles the {@code /mg} command family.
 *
 * <pre>
 * /mg platform regen   – Re-generate the spawn safety platform.
 * /mg voidplatform     – Alias for /mg platform regen.
 * </pre>
 *
 * All sub-commands require the {@code minigameswr.admin} permission.
 */
public class MgCommand implements CommandExecutor, TabCompleter {

    private static final String PERM_ADMIN = "minigameswr.admin";

    private final MiniGamesWR plugin;

    public MgCommand(MiniGamesWR plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§eMiniGamesWR — available sub-commands:");
            sender.sendMessage("§7  /mg platform regen §f– re-create spawn safety platform");
            sender.sendMessage("§7  /mg voidplatform   §f– alias for platform regen");
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("platform")) {
            if (!checkPermission(sender)) return true;
            if (args.length >= 2 && args[1].equalsIgnoreCase("regen")) {
                return regenPlatform(sender);
            }
            sender.sendMessage("§eUsage: /mg platform regen");
            return true;
        }

        if (sub.equals("voidplatform")) {
            if (!checkPermission(sender)) return true;
            return regenPlatform(sender);
        }

        sender.sendMessage("§cUnknown sub-command. Type §e/mg §cfor help.");
        return true;
    }

    private boolean regenPlatform(CommandSender sender) {
        String worldName = plugin.getConfig().getString("void-generator.world-name", "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            sender.sendMessage("§cWorld '§e" + worldName + "§c' is not loaded.");
            return true;
        }
        new WorldLoadListener(plugin).placePlatform(world);
        sender.sendMessage("§aSpawn platform regenerated in world '§e" + worldName + "§a'.");
        return true;
    }

    private boolean checkPermission(CommandSender sender) {
        if (!sender.hasPermission(PERM_ADMIN)) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return false;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        if (!sender.hasPermission(PERM_ADMIN)) return Collections.emptyList();
        if (args.length == 1) {
            return Arrays.asList("platform", "voidplatform");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("platform")) {
            return Collections.singletonList("regen");
        }
        return Collections.emptyList();
    }
}
