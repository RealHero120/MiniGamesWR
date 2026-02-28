package com.wildrose.minigameswr.commands;

import com.wildrose.minigameswr.MiniGamesWR;
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
        sender.sendMessage(Component.text("/mg reload", NamedTextColor.YELLOW)
                .append(Component.text(" - Reload config", NamedTextColor.GRAY)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("minigameswr.admin")) return List.of();

        if (args.length == 1) {
            return filterStartsWith(args[0], "sethub", "duel", "spliff", "reload");
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("duel")) {
                return filterStartsWith(args[1], "setspawn");
            }
            if (args[0].equalsIgnoreCase("spliff")) {
                return filterStartsWith(args[1], "addspawn", "setpos1", "setpos2");
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("duel") && args[1].equalsIgnoreCase("setspawn")) {
            return filterStartsWith(args[2], "a", "b");
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
