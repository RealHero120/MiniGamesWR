package com.wildrose.minigameswr.commands;

import com.wildrose.minigameswr.MiniGamesWR;
import com.wildrose.minigameswr.spleef.SpleefArena;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MgCommand implements CommandExecutor, TabCompleter {

    private final MiniGamesWR plugin;

    public MgCommand(MiniGamesWR plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("minigameswr.admin")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "sethub" -> cmdSetHub(sender);
            case "duel" -> cmdDuel(sender, args);
            case "spliff" -> cmdSpliff(sender, args);
            case "spleef" -> cmdSpleef(sender, args);
            case "reload" -> cmdReload(sender);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void cmdSetHub(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command must be run by a player.", NamedTextColor.RED));
            return;
        }
        plugin.getConfigManager().setHubSpawn(player.getLocation());
        player.sendMessage(Component.text("Hub spawn set to your current location.", NamedTextColor.GREEN));
    }

    private void cmdDuel(CommandSender sender, String[] args) {
        // /mg duel setspawn a|b
        if (args.length < 3 || !args[1].equalsIgnoreCase("setspawn")) {
            sender.sendMessage(Component.text("Usage: /mg duel setspawn <a|b>", NamedTextColor.YELLOW));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command must be run by a player.", NamedTextColor.RED));
            return;
        }
        String which = args[2].toLowerCase();
        if (!which.equals("a") && !which.equals("b")) {
            sender.sendMessage(Component.text("Usage: /mg duel setspawn <a|b>", NamedTextColor.YELLOW));
            return;
        }
        plugin.getConfigManager().setDuelSpawn(which, player.getLocation());
        player.sendMessage(Component.text("Duel spawn " + which.toUpperCase() + " set to your current location.", NamedTextColor.GREEN));
    }

    private void cmdSpliff(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /mg spliff <addspawn|setpos1|setpos2>", NamedTextColor.YELLOW));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command must be run by a player.", NamedTextColor.RED));
            return;
        }

        switch (args[1].toLowerCase()) {
            case "addspawn" -> {
                plugin.getConfigManager().addSpliffSpawnpoint(player.getLocation());
                player.sendMessage(Component.text("Spliff spawnpoint added at your current location.", NamedTextColor.GREEN));
            }
            case "setpos1" -> {
                plugin.getConfigManager().setSpliffPos(1, player.getLocation());
                player.sendMessage(Component.text("Spliff arena pos1 set to your current block position.", NamedTextColor.GREEN));
            }
            case "setpos2" -> {
                plugin.getConfigManager().setSpliffPos(2, player.getLocation());
                player.sendMessage(Component.text("Spliff arena pos2 set to your current block position.", NamedTextColor.GREEN));
            }
            default -> sender.sendMessage(Component.text("Usage: /mg spliff <addspawn|setpos1|setpos2>", NamedTextColor.YELLOW));
        }
    }

    /**
     * Handle all {@code /mg spleef arena ...} subcommands.
     * Usage:
     * <pre>
     *   /mg spleef arena create  &lt;name&gt;
     *   /mg spleef arena pos1    &lt;name&gt;
     *   /mg spleef arena pos2    &lt;name&gt;
     *   /mg spleef arena setspawn &lt;name&gt; &lt;1|2|...&gt;
     *   /mg spleef arena setlobby &lt;name&gt;
     *   /mg spleef arena save    &lt;name&gt;
     * </pre>
     */
    private void cmdSpleef(CommandSender sender, String[] args) {
        // /mg spleef arena <sub> [name] [extra...]
        if (args.length < 4 || !args[1].equalsIgnoreCase("arena")) {
            sender.sendMessage(Component.text("Usage: /mg spleef arena <create|pos1|pos2|setspawn|setlobby|save> <name> [index]", NamedTextColor.YELLOW));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command must be run by a player.", NamedTextColor.RED));
            return;
        }

        String sub = args[2].toLowerCase();
        String arenaName = args[3];

        switch (sub) {
            case "create" -> {
                if (!plugin.getSpleefManager().createArena(arenaName)) {
                    player.sendMessage(Component.text("Arena '" + arenaName + "' already exists!", NamedTextColor.RED));
                } else {
                    player.sendMessage(Component.text("Spleef arena '" + arenaName + "' created. Now set pos1, pos2, spawns, then save.", NamedTextColor.GREEN));
                }
            }
            case "pos1" -> {
                SpleefArena arena = requireArena(player, arenaName);
                if (arena == null) return;
                arena.setPos1(player.getLocation().getBlock().getLocation());
                arena.setWorldName(player.getWorld().getName());
                plugin.getConfigManager().saveSpleefArena(arena);
                player.sendMessage(Component.text("Spleef arena '" + arenaName + "' pos1 set.", NamedTextColor.GREEN));
            }
            case "pos2" -> {
                SpleefArena arena = requireArena(player, arenaName);
                if (arena == null) return;
                arena.setPos2(player.getLocation().getBlock().getLocation());
                arena.setWorldName(player.getWorld().getName());
                plugin.getConfigManager().saveSpleefArena(arena);
                player.sendMessage(Component.text("Spleef arena '" + arenaName + "' pos2 set.", NamedTextColor.GREEN));
            }
            case "setspawn" -> {
                // /mg spleef arena setspawn <name> <index>
                if (args.length < 5) {
                    player.sendMessage(Component.text("Usage: /mg spleef arena setspawn <name> <1|2|...>", NamedTextColor.YELLOW));
                    return;
                }
                SpleefArena arena = requireArena(player, arenaName);
                if (arena == null) return;
                try {
                    int idx = Integer.parseInt(args[4]) - 1; // 1-based → 0-based
                    if (idx < 0) throw new NumberFormatException();
                    arena.setSpawnpoint(idx, player.getLocation());
                    plugin.getConfigManager().saveSpleefArena(arena);
                    player.sendMessage(Component.text("Spleef arena '" + arenaName + "' spawn " + (idx + 1) + " set.", NamedTextColor.GREEN));
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.text("Invalid spawn index. Use 1, 2, 3 …", NamedTextColor.RED));
                }
            }
            case "setlobby" -> {
                SpleefArena arena = requireArena(player, arenaName);
                if (arena == null) return;
                arena.setLobby(player.getLocation());
                plugin.getConfigManager().saveSpleefArena(arena);
                player.sendMessage(Component.text("Spleef arena '" + arenaName + "' lobby return set.", NamedTextColor.GREEN));
            }
            case "save" -> {
                SpleefArena arena = requireArena(player, arenaName);
                if (arena == null) return;
                if (arena.getPos1() == null || arena.getPos2() == null) {
                    player.sendMessage(Component.text("Set pos1 and pos2 first!", NamedTextColor.RED));
                    return;
                }
                player.sendMessage(Component.text("Capturing snapshot for arena '" + arenaName + "'…", NamedTextColor.GRAY));
                if (plugin.getSpleefManager().saveSnapshot(arena)) {
                    player.sendMessage(Component.text("Snapshot saved! Arena '" + arenaName + "' is ready.", NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("Failed to save snapshot. Check console for errors.", NamedTextColor.RED));
                }
            }
            default -> sender.sendMessage(Component.text("Usage: /mg spleef arena <create|pos1|pos2|setspawn|setlobby|save> <name> [index]", NamedTextColor.YELLOW));
        }
    }

    /** Looks up an arena and sends an error if it does not exist. */
    private SpleefArena requireArena(Player player, String name) {
        SpleefArena arena = plugin.getSpleefManager().getArena(name);
        if (arena == null) {
            player.sendMessage(Component.text("Arena '" + name + "' does not exist. Create it first with /mg spleef arena create " + name, NamedTextColor.RED));
        }
        return arena;
    }

    private void cmdReload(CommandSender sender) {
        plugin.reloadConfig();
        sender.sendMessage(Component.text("MiniGamesWR config reloaded.", NamedTextColor.GREEN));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("--- MiniGamesWR Admin Commands ---", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/mg sethub", NamedTextColor.YELLOW)
                .append(Component.text(" - Set hub spawn to your position", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/mg duel setspawn <a|b>", NamedTextColor.YELLOW)
                .append(Component.text(" - Set duel spawn A or B", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/mg spliff addspawn", NamedTextColor.YELLOW)
                .append(Component.text(" - Add a Spliff spawnpoint", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/mg spliff setpos1", NamedTextColor.YELLOW)
                .append(Component.text(" - Set Spliff arena corner 1", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/mg spliff setpos2", NamedTextColor.YELLOW)
                .append(Component.text(" - Set Spliff arena corner 2", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/mg spleef arena create <name>", NamedTextColor.YELLOW)
                .append(Component.text(" - Create a new Spleef arena", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/mg spleef arena pos1 <name>", NamedTextColor.YELLOW)
                .append(Component.text(" - Set arena corner 1", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/mg spleef arena pos2 <name>", NamedTextColor.YELLOW)
                .append(Component.text(" - Set arena corner 2", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/mg spleef arena setspawn <name> <1|2|…>", NamedTextColor.YELLOW)
                .append(Component.text(" - Set a spawn point", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/mg spleef arena setlobby <name>", NamedTextColor.YELLOW)
                .append(Component.text(" - Set per-arena lobby return", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/mg spleef arena save <name>", NamedTextColor.YELLOW)
                .append(Component.text(" - Capture arena snapshot", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/mg reload", NamedTextColor.YELLOW)
                .append(Component.text(" - Reload config", NamedTextColor.GRAY)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("minigameswr.admin")) return List.of();

        if (args.length == 1) {
            return filterStartsWith(args[0], "sethub", "duel", "spliff", "spleef", "reload");
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("duel")) {
                return filterStartsWith(args[1], "setspawn");
            }
            if (args[0].equalsIgnoreCase("spliff")) {
                return filterStartsWith(args[1], "addspawn", "setpos1", "setpos2");
            }
            if (args[0].equalsIgnoreCase("spleef")) {
                return filterStartsWith(args[1], "arena");
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("duel") && args[1].equalsIgnoreCase("setspawn")) {
            return filterStartsWith(args[2], "a", "b");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("spleef") && args[1].equalsIgnoreCase("arena")) {
            return filterStartsWith(args[2], "create", "pos1", "pos2", "setspawn", "setlobby", "save");
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("spleef") && args[1].equalsIgnoreCase("arena")) {
            // Suggest existing arena names
            List<String> names = new ArrayList<>();
            plugin.getSpleefManager().getArenas().forEach(a -> names.add(a.getName()));
            return filterStartsWith(args[3], names.toArray(new String[0]));
        }
        if (args.length == 5 && args[0].equalsIgnoreCase("spleef") && args[1].equalsIgnoreCase("arena")
                && args[2].equalsIgnoreCase("setspawn")) {
            return filterStartsWith(args[4], "1", "2", "3", "4");
        }
        return List.of();
    }

    private List<String> filterStartsWith(String input, String... options) {
        List<String> result = new ArrayList<>();
        for (String o : options) {
            if (o.toLowerCase().startsWith(input.toLowerCase())) result.add(o);
        }
        return result;
    }
}
