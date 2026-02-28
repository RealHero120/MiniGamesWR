package com.wildrose.minigameswr.spliff;

import com.wildrose.minigameswr.MiniGamesWR;
import com.wildrose.minigameswr.hub.HubListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.*;

public class SpliffManager {

    private final MiniGamesWR plugin;

    // Players in queue (not yet in match)
    private final List<UUID> queue = new ArrayList<>();

    // Players currently in a match
    private final Set<UUID> inMatch = new HashSet<>();

    // Players eliminated this match (for tracking)
    private final Set<UUID> eliminated = new HashSet<>();

    // All players who started the current match
    private final List<UUID> matchPlayers = new ArrayList<>();

    private BukkitTask countdownTask = null;
    private BukkitTask matchCheckTask = null;
    private boolean matchRunning = false;

    public SpliffManager(MiniGamesWR plugin) {
        this.plugin = plugin;
    }

    public void joinQueue(Player player) {
        UUID uuid = player.getUniqueId();

        if (inMatch.contains(uuid) || matchPlayers.contains(uuid)) {
            player.sendMessage(Component.text("You are already in a Spliff match!", NamedTextColor.RED));
            return;
        }
        if (plugin.getDuelsManager().isInDuel(uuid) || plugin.getDuelsManager().isInQueue(uuid)) {
            player.sendMessage(Component.text("You are already in a Duels match or queue!", NamedTextColor.RED));
            return;
        }
        if (queue.contains(uuid)) {
            player.sendMessage(Component.text("You are already in the Spliff queue!", NamedTextColor.YELLOW));
            return;
        }
        if (matchRunning) {
            player.sendMessage(Component.text("A Spliff match is already in progress. Try again later!", NamedTextColor.YELLOW));
            return;
        }

        queue.add(uuid);
        player.sendMessage(Component.text("You joined the Spliff queue! Players: " + queue.size(), NamedTextColor.GREEN));

        // Notify all queued players
        broadcastToQueue(Component.text(player.getName() + " joined the queue. (" + queue.size() + " player(s))", NamedTextColor.YELLOW));

        if (queue.size() >= 2) {
            startMatch();
        }
    }

    public void leaveQueue(UUID uuid) {
        queue.remove(uuid);
    }

    private void startMatch() {
        List<Location> spawns = plugin.getConfigManager().getSpliffSpawnpoints();
        if (spawns.isEmpty()) {
            broadcastToQueue(Component.text("Spliff arena is not configured! Contact an admin.", NamedTextColor.RED));
            queue.clear();
            return;
        }

        matchRunning = true;
        matchPlayers.clear();
        matchPlayers.addAll(queue);
        queue.clear();
        inMatch.clear();
        inMatch.addAll(matchPlayers);
        eliminated.clear();

        // Teleport each player to a spawn
        for (int i = 0; i < matchPlayers.size(); i++) {
            UUID uuid = matchPlayers.get(i);
            Player p = plugin.getServer().getPlayer(uuid);
            if (p == null) {
                inMatch.remove(uuid);
                eliminated.add(uuid);
                continue;
            }
            Location spawn = spawns.get(i % spawns.size());
            p.teleport(spawn);
            clearInventory(p);
        }

        // Announce
        plugin.getServer().broadcast(Component.text("[Spliff] Match starting with " + matchPlayers.size() + " players!", NamedTextColor.GREEN));

        startCountdown();
    }

    private void startCountdown() {
        final int[] secondsLeft = {5};

        countdownTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (secondsLeft[0] > 0) {
                Title title = Title.title(
                        Component.text(String.valueOf(secondsLeft[0]), NamedTextColor.GREEN),
                        Component.text("Spliff starting!", NamedTextColor.GRAY),
                        Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(900), Duration.ofMillis(100))
                );
                for (UUID uuid : inMatch) {
                    Player p = plugin.getServer().getPlayer(uuid);
                    if (p != null) {
                        p.showTitle(title);
                        p.sendActionBar(Component.text("Spliff starts in " + secondsLeft[0] + "...", NamedTextColor.GREEN));
                    }
                }
                secondsLeft[0]--;
            } else {
                if (countdownTask != null) {
                    countdownTask.cancel();
                    countdownTask = null;
                }
                Title goTitle = Title.title(
                        Component.text("GO!", NamedTextColor.GREEN),
                        Component.empty(),
                        Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(1500), Duration.ofMillis(200))
                );
                for (UUID uuid : inMatch) {
                    Player p = plugin.getServer().getPlayer(uuid);
                    if (p != null) p.showTitle(goTitle);
                }
                startMatchChecks();
            }
        }, 0L, 20L);
    }

    private void startMatchChecks() {
        double loseY = plugin.getConfigManager().getSpliffLoseY();

        matchCheckTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!matchRunning) {
                if (matchCheckTask != null) matchCheckTask.cancel();
                return;
            }

            List<UUID> toEliminate = new ArrayList<>();
            for (UUID uuid : new ArrayList<>(inMatch)) {
                Player p = plugin.getServer().getPlayer(uuid);
                if (p == null || !p.isOnline()) {
                    toEliminate.add(uuid);
                    continue;
                }
                if (p.getLocation().getY() < loseY) {
                    toEliminate.add(uuid);
                    p.sendMessage(Component.text("You fell out of the arena!", NamedTextColor.RED));
                }
            }

            for (UUID uuid : toEliminate) {
                eliminatePlayer(uuid, false);
            }

            checkMatchEnd();
        }, 20L, 10L);
    }

    public void eliminatePlayer(UUID uuid, boolean fromDeath) {
        if (!inMatch.contains(uuid)) return;
        inMatch.remove(uuid);
        eliminated.add(uuid);

        Player p = plugin.getServer().getPlayer(uuid);
        if (p != null) {
            p.sendMessage(Component.text("You have been eliminated from Spliff!", NamedTextColor.RED));
        }

        plugin.getServer().broadcast(Component.text("[Spliff] ", NamedTextColor.GREEN)
                .append(Component.text(p != null ? p.getName() : "A player", NamedTextColor.WHITE))
                .append(Component.text(" has been eliminated! (" + inMatch.size() + " remaining)", NamedTextColor.YELLOW)));

        checkMatchEnd();
    }

    private void checkMatchEnd() {
        if (!matchRunning) return;

        // Remove offline players from inMatch
        inMatch.removeIf(uuid -> {
            Player p = plugin.getServer().getPlayer(uuid);
            return p == null || !p.isOnline();
        });

        if (inMatch.size() <= 1) {
            UUID winnerUUID = inMatch.isEmpty() ? null : inMatch.iterator().next();
            endMatch(winnerUUID);
        }
    }

    private void endMatch(UUID winnerUUID) {
        if (!matchRunning) return;
        matchRunning = false;

        if (countdownTask != null) { countdownTask.cancel(); countdownTask = null; }
        if (matchCheckTask != null) { matchCheckTask.cancel(); matchCheckTask = null; }

        Player winner = winnerUUID != null ? plugin.getServer().getPlayer(winnerUUID) : null;

        if (winner != null) {
            plugin.getServer().broadcast(Component.text("[Spliff] ", NamedTextColor.GREEN)
                    .append(Component.text(winner.getName(), NamedTextColor.GOLD))
                    .append(Component.text(" won Spliff!", NamedTextColor.GREEN)));
        } else {
            plugin.getServer().broadcast(Component.text("[Spliff] The match ended with no winner!", NamedTextColor.YELLOW));
        }

        // Collect all players to return (winner + remaining)
        Set<UUID> toReturn = new HashSet<>(matchPlayers);

        // Return to hub after 3 seconds
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (UUID uuid : toReturn) {
                Player p = plugin.getServer().getPlayer(uuid);
                returnToHub(p);
            }
            // Reset arena
            resetArena();
            inMatch.clear();
            matchPlayers.clear();
            eliminated.clear();
        }, 60L);
    }

    private void resetArena() {
        Location pos1 = plugin.getConfigManager().getSpliffPos1();
        Location pos2 = plugin.getConfigManager().getSpliffPos2();
        Material block = plugin.getConfigManager().getSpliffBlock();

        if (pos1 == null || pos2 == null || pos1.getWorld() == null) return;

        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        // Build list of all blocks to set
        List<int[]> blocks = new ArrayList<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    blocks.add(new int[]{x, y, z});
                }
            }
        }

        // Batch fill: 500 blocks per tick
        final int BATCH_SIZE = 500;
        final int[] index = {0};
        final org.bukkit.World world = pos1.getWorld();

        final BukkitTask[] resetTaskHolder = {null};
        resetTaskHolder[0] = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            int end = Math.min(index[0] + BATCH_SIZE, blocks.size());
            for (int i = index[0]; i < end; i++) {
                int[] coord = blocks.get(i);
                Block b = world.getBlockAt(coord[0], coord[1], coord[2]);
                b.setType(block, false);
            }
            index[0] = end;
            if (index[0] >= blocks.size()) {
                resetTaskHolder[0].cancel();
                plugin.getLogger().info("[Spliff] Arena reset complete.");
            }
        }, 2L, 1L);
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

    private void clearInventory(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
    }

    private void broadcastToQueue(Component msg) {
        for (UUID uuid : queue) {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null) p.sendMessage(msg);
        }
    }

    public boolean isInSpliff(UUID uuid) {
        return inMatch.contains(uuid) || matchPlayers.contains(uuid) || queue.contains(uuid);
    }

    public boolean isMatchRunning() {
        return matchRunning;
    }

    public void handleDisconnect(UUID uuid) {
        leaveQueue(uuid);
        eliminatePlayer(uuid, false);
    }

    public void shutdown() {
        if (countdownTask != null) countdownTask.cancel();
        if (matchCheckTask != null) matchCheckTask.cancel();
    }

    public boolean canBreakBlock(Player player, Block block) {
        if (!inMatch.contains(player.getUniqueId())) return false;
        Material spliffBlock = plugin.getConfigManager().getSpliffBlock();
        return block.getType() == spliffBlock;
    }
}
