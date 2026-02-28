package com.wildrose.minigameswr.duels;

import com.wildrose.minigameswr.MiniGamesWR;
import com.wildrose.minigameswr.hub.HubListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.*;

public class DuelsManager {

    private final MiniGamesWR plugin;

    // UUID of the player waiting in queue (null if nobody)
    private UUID waitingPlayer = null;

    // Active matches: maps each participant UUID -> their opponent UUID
    private final Map<UUID, UUID> activeMatches = new HashMap<>();

    // Countdown tasks
    private final Map<UUID, BukkitTask> countdownTasks = new HashMap<>();

    public DuelsManager(MiniGamesWR plugin) {
        this.plugin = plugin;
    }

    public void joinQueue(Player player) {
        UUID uuid = player.getUniqueId();

        if (activeMatches.containsKey(uuid)) {
            player.sendMessage(Component.text("You are already in a duel!", NamedTextColor.RED));
            return;
        }
        if (plugin.getSpliffManager().isInSpliff(uuid)) {
            player.sendMessage(Component.text("You are already in a Spliff match!", NamedTextColor.RED));
            return;
        }
        if (waitingPlayer != null && waitingPlayer.equals(uuid)) {
            player.sendMessage(Component.text("You are already in the duel queue!", NamedTextColor.YELLOW));
            return;
        }

        if (waitingPlayer == null) {
            // Put player in queue
            waitingPlayer = uuid;
            player.sendMessage(Component.text("You joined the duel queue. Waiting for an opponent...", NamedTextColor.YELLOW));
        } else {
            // Start match
            Player playerA = plugin.getServer().getPlayer(waitingPlayer);
            waitingPlayer = null;
            if (playerA == null) {
                // Previous waiter left, this player becomes the new waiter
                waitingPlayer = uuid;
                player.sendMessage(Component.text("You joined the duel queue. Waiting for an opponent...", NamedTextColor.YELLOW));
                return;
            }
            startMatch(playerA, player);
        }
    }

    public void leaveQueue(UUID uuid) {
        if (waitingPlayer != null && waitingPlayer.equals(uuid)) {
            waitingPlayer = null;
        }
    }

    private void startMatch(Player pA, Player pB) {
        Location spawnA = plugin.getConfigManager().getDuelSpawnA();
        Location spawnB = plugin.getConfigManager().getDuelSpawnB();

        if (spawnA == null || spawnA.getWorld() == null
                || spawnB == null || spawnB.getWorld() == null) {
            pA.sendMessage(Component.text("Duel arena is not configured! Contact an admin.", NamedTextColor.RED));
            pB.sendMessage(Component.text("Duel arena is not configured! Contact an admin.", NamedTextColor.RED));
            return;
        }

        activeMatches.put(pA.getUniqueId(), pB.getUniqueId());
        activeMatches.put(pB.getUniqueId(), pA.getUniqueId());

        // Teleport
        pA.teleport(spawnA);
        pB.teleport(spawnB);

        // Clear and give kit
        giveKit(pA);
        giveKit(pB);

        // Announce
        broadcast(pA, pB, Component.text("Duel starting between ", NamedTextColor.GOLD)
                .append(Component.text(pA.getName(), NamedTextColor.WHITE))
                .append(Component.text(" vs ", NamedTextColor.GOLD))
                .append(Component.text(pB.getName(), NamedTextColor.WHITE))
                .append(Component.text("!", NamedTextColor.GOLD)));

        startCountdown(pA, pB);
    }

    private void startCountdown(Player pA, Player pB) {
        final int[] secondsLeft = {5};

        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (secondsLeft[0] > 0) {
                Title title = Title.title(
                        Component.text(String.valueOf(secondsLeft[0]), NamedTextColor.YELLOW),
                        Component.text("Get ready!", NamedTextColor.GRAY),
                        Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(900), Duration.ofMillis(100))
                );
                sendTitle(pA, title);
                sendTitle(pB, title);
                sendActionBar(pA, "Duel starts in " + secondsLeft[0] + "...");
                sendActionBar(pB, "Duel starts in " + secondsLeft[0] + "...");
                secondsLeft[0]--;
            } else {
                // Cancel the task stored
                cancelCountdown(pA.getUniqueId());
                cancelCountdown(pB.getUniqueId());

                Title goTitle = Title.title(
                        Component.text("FIGHT!", NamedTextColor.RED),
                        Component.empty(),
                        Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(1500), Duration.ofMillis(200))
                );
                sendTitle(pA, goTitle);
                sendTitle(pB, goTitle);
            }
        }, 0L, 20L);

        countdownTasks.put(pA.getUniqueId(), task);
        countdownTasks.put(pB.getUniqueId(), task);
    }

    public void handleDeath(Player loser) {
        UUID loserUUID = loser.getUniqueId();
        if (!activeMatches.containsKey(loserUUID)) return;

        UUID winnerUUID = activeMatches.get(loserUUID);
        Player winner = plugin.getServer().getPlayer(winnerUUID);

        endMatch(loserUUID, winnerUUID,
                winner != null ? Component.text(winner.getName(), NamedTextColor.GOLD) : Component.text("Unknown", NamedTextColor.GRAY),
                Component.text(loser.getName(), NamedTextColor.RED));
    }

    public void handleDisconnect(UUID disconnectedUUID) {
        leaveQueue(disconnectedUUID);
        if (!activeMatches.containsKey(disconnectedUUID)) return;

        UUID winnerUUID = activeMatches.get(disconnectedUUID);
        Player winner = plugin.getServer().getPlayer(winnerUUID);

        endMatch(disconnectedUUID, winnerUUID,
                winner != null ? Component.text(winner.getName(), NamedTextColor.GOLD) : Component.text("Unknown", NamedTextColor.GRAY),
                Component.text("(disconnected)", NamedTextColor.GRAY));
    }

    private void endMatch(UUID loserUUID, UUID winnerUUID, Component winnerName, Component loserName) {
        cancelCountdown(loserUUID);
        cancelCountdown(winnerUUID);

        activeMatches.remove(loserUUID);
        activeMatches.remove(winnerUUID);

        Player winner = plugin.getServer().getPlayer(winnerUUID);
        Player loser = plugin.getServer().getPlayer(loserUUID);

        // Announce
        plugin.getServer().broadcast(Component.text("[Duels] ", NamedTextColor.GOLD)
                .append(winnerName)
                .append(Component.text(" won the duel against ", NamedTextColor.YELLOW))
                .append(loserName)
                .append(Component.text("!", NamedTextColor.YELLOW)));

        // Return both to hub after 3 seconds
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            returnToHub(winner);
            returnToHub(loser);
        }, 60L);
    }

    private void returnToHub(Player player) {
        if (player == null) return;
        clearInventory(player);
        Location hub = plugin.getConfigManager().getHubSpawn();
        if (hub != null && hub.getWorld() != null) {
            player.teleport(hub);
        }
        HubListener.giveOpenerItem(player);
    }

    private void giveKit(Player player) {
        clearInventory(player);
        player.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));
        player.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        player.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
        player.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));
        player.getInventory().setItem(0, new ItemStack(Material.IRON_SWORD));
        player.getInventory().setItem(1, new ItemStack(Material.COOKED_BEEF, 16));
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20f);
    }

    private void clearInventory(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
    }

    private void cancelCountdown(UUID uuid) {
        BukkitTask task = countdownTasks.remove(uuid);
        if (task != null) task.cancel();
    }

    private void broadcast(Player a, Player b, Component msg) {
        plugin.getServer().broadcast(msg);
    }

    private void sendTitle(Player player, Title title) {
        if (player != null && player.isOnline()) player.showTitle(title);
    }

    private void sendActionBar(Player player, String message) {
        if (player != null && player.isOnline()) {
            player.sendActionBar(Component.text(message, NamedTextColor.YELLOW));
        }
    }

    public boolean isInDuel(UUID uuid) {
        return activeMatches.containsKey(uuid);
    }

    public boolean isInQueue(UUID uuid) {
        return waitingPlayer != null && waitingPlayer.equals(uuid);
    }

    public void shutdown() {
        countdownTasks.values().forEach(BukkitTask::cancel);
        countdownTasks.clear();
    }
}
