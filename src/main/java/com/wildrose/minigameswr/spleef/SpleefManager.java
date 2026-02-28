package com.wildrose.minigameswr.spleef;

import com.wildrose.minigameswr.MiniGamesWR;
import com.wildrose.minigameswr.hub.HubListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.*;

/**
 * Central manager for all Spleef arenas.
 * Handles queue, countdown, match lifecycle, and arena reset for each named arena.
 */
public class SpleefManager {

    private static final int RESTORE_BATCH_SIZE = 500;

    private final MiniGamesWR plugin;
    /** All configured arenas, keyed by lower-case arena name. */
    private final Map<String, SpleefArena> arenas = new LinkedHashMap<>();
    /** Per-arena active scheduler tasks (countdown / match-check). */
    private final Map<String, BukkitTask> countdownTasks = new HashMap<>();
    private final Map<String, BukkitTask> matchCheckTasks = new HashMap<>();

    public SpleefManager(MiniGamesWR plugin) {
        this.plugin = plugin;
        loadArenas();
    }

    // ---- Arena lifecycle ----

    /** Called on plugin enable to read arenas from config and load their snapshots. */
    private void loadArenas() {
        List<SpleefArena> loaded = plugin.getConfigManager().loadSpleefArenas();
        for (SpleefArena arena : loaded) {
            File snapFile = snapshotFile(arena.getName());
            arena.getSnapshot().load(snapFile, plugin.getLogger());
            arenas.put(arena.getName().toLowerCase(), arena);
        }
        plugin.getLogger().info("[Spleef] Loaded " + arenas.size() + " arena(s).");
    }

    /**
     * Create a brand-new empty arena and persist it immediately.
     * @return {@code false} if an arena with that name already exists.
     */
    public boolean createArena(String name) {
        if (arenas.containsKey(name.toLowerCase())) return false;
        SpleefArena arena = new SpleefArena(name);
        arenas.put(name.toLowerCase(), arena);
        plugin.getConfigManager().saveSpleefArena(arena);
        return true;
    }

    /** Returns the arena with the given name, or {@code null}. */
    public SpleefArena getArena(String name) {
        return arenas.get(name.toLowerCase());
    }

    /** Returns an unmodifiable view of all loaded arenas. */
    public Collection<SpleefArena> getArenas() {
        return Collections.unmodifiableCollection(arenas.values());
    }

    /**
     * Capture the current block state of the arena cuboid and save it to disk.
     * @return {@code false} if pos1/pos2 or world are not yet set.
     */
    public boolean saveSnapshot(SpleefArena arena) {
        if (arena.getPos1() == null || arena.getPos2() == null || arena.getWorldName() == null) return false;
        World world = Bukkit.getWorld(arena.getWorldName());
        if (world == null) return false;
        arena.getSnapshot().capture(world, arena.getPos1(), arena.getPos2());
        try {
            arena.getSnapshot().save(snapshotFile(arena.getName()));
        } catch (IOException e) {
            plugin.getLogger().severe("[Spleef] Could not save snapshot for arena '" + arena.getName() + "': " + e.getMessage());
            return false;
        }
        return true;
    }

    // ---- Queue ----

    /**
     * Add a player to the queue for the first available, configured arena.
     * If no configured arena is free the player is informed.
     */
    public void joinQueue(Player player) {
        UUID uuid = player.getUniqueId();

        // Already in any Spleef activity?
        for (SpleefArena arena : arenas.values()) {
            if (arena.getInMatch().contains(uuid) || arena.getMatchPlayers().contains(uuid) || arena.getQueue().contains(uuid)) {
                player.sendMessage(Component.text("You are already in a Spleef match or queue!", NamedTextColor.RED));
                return;
            }
        }
        // Also check Duels/Spliff
        if (plugin.getDuelsManager().isInDuel(uuid) || plugin.getDuelsManager().isInQueue(uuid)) {
            player.sendMessage(Component.text("You are already in a Duels match or queue!", NamedTextColor.RED));
            return;
        }
        if (plugin.getSpliffManager().isInSpliff(uuid)) {
            player.sendMessage(Component.text("You are already in a Spliff match or queue!", NamedTextColor.RED));
            return;
        }

        // Find first arena that is free and configured
        SpleefArena target = null;
        for (SpleefArena arena : arenas.values()) {
            if (!arena.isConfigured()) continue;
            if (arena.isMatchRunning() || arena.isResetting()) continue;
            target = arena;
            break;
        }
        if (target == null) {
            player.sendMessage(Component.text("No Spleef arena is available right now. Try again later!", NamedTextColor.YELLOW));
            return;
        }

        joinArenaQueue(player, target);
    }

    /** Add a player to the queue for a specifically named arena. */
    public void joinArenaQueue(Player player, SpleefArena arena) {
        UUID uuid = player.getUniqueId();
        if (arena.isMatchRunning() || arena.isResetting()) {
            player.sendMessage(Component.text("Arena '" + arena.getName() + "' is busy. Try again later!", NamedTextColor.YELLOW));
            return;
        }
        if (!arena.isConfigured()) {
            player.sendMessage(Component.text("Arena '" + arena.getName() + "' is not fully configured yet.", NamedTextColor.RED));
            return;
        }
        arena.getQueue().add(uuid);
        player.sendMessage(Component.text("You joined Spleef queue for '" + arena.getName() + "'! Players: " + arena.getQueue().size(), NamedTextColor.GREEN));
        broadcastToArenaQueue(arena, Component.text(player.getName() + " joined the queue. (" + arena.getQueue().size() + " player(s))", NamedTextColor.YELLOW));

        if (arena.getQueue().size() >= 2) {
            startMatch(arena);
        }
    }

    public void leaveQueue(UUID uuid) {
        for (SpleefArena arena : arenas.values()) {
            arena.getQueue().remove(uuid);
        }
    }

    // ---- Match lifecycle ----

    private void startMatch(SpleefArena arena) {
        arena.setMatchRunning(true);
        arena.getMatchPlayers().clear();
        arena.getMatchPlayers().addAll(arena.getQueue());
        arena.getQueue().clear();
        arena.getInMatch().clear();
        arena.getInMatch().addAll(arena.getMatchPlayers());
        arena.getEliminated().clear();

        List<Location> spawns = arena.getSpawnpoints();
        for (int i = 0; i < arena.getMatchPlayers().size(); i++) {
            UUID uuid = arena.getMatchPlayers().get(i);
            Player p = plugin.getServer().getPlayer(uuid);
            if (p == null) {
                arena.getInMatch().remove(uuid);
                arena.getEliminated().add(uuid);
                continue;
            }
            p.teleport(spawns.get(i % spawns.size()));
            giveKit(p);
        }

        plugin.getServer().broadcast(Component.text("[Spleef] Match starting in arena '" + arena.getName() + "' with " + arena.getMatchPlayers().size() + " players!", NamedTextColor.GREEN));
        startCountdown(arena);
    }

    private void startCountdown(SpleefArena arena) {
        final int[] secondsLeft = {5};
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (secondsLeft[0] > 0) {
                Title title = Title.title(
                        Component.text(String.valueOf(secondsLeft[0]), NamedTextColor.GREEN),
                        Component.text("Spleef starting!", NamedTextColor.GRAY),
                        Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(900), Duration.ofMillis(100))
                );
                for (UUID uuid : arena.getInMatch()) {
                    Player p = plugin.getServer().getPlayer(uuid);
                    if (p != null) {
                        p.showTitle(title);
                        p.sendActionBar(Component.text("Spleef starts in " + secondsLeft[0] + "...", NamedTextColor.GREEN));
                    }
                }
                secondsLeft[0]--;
            } else {
                BukkitTask ct = countdownTasks.remove(arena.getName());
                if (ct != null) ct.cancel();
                Title goTitle = Title.title(
                        Component.text("GO!", NamedTextColor.GREEN),
                        Component.empty(),
                        Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(1500), Duration.ofMillis(200))
                );
                for (UUID uuid : arena.getInMatch()) {
                    Player p = plugin.getServer().getPlayer(uuid);
                    if (p != null) p.showTitle(goTitle);
                }
                startMatchChecks(arena);
            }
        }, 0L, 20L);
        countdownTasks.put(arena.getName(), task);
    }

    private void startMatchChecks(SpleefArena arena) {
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!arena.isMatchRunning()) {
                BukkitTask t = matchCheckTasks.remove(arena.getName());
                if (t != null) t.cancel();
                return;
            }
            // Elimination Y: one block below the arena floor so players who just break the
            // last block under themselves have a moment to react before falling too far.
            double arenaFloorY = arena.getPos1() != null
                    ? Math.min(arena.getPos1().getBlockY(), arena.getPos2().getBlockY()) - 1
                    : 0;
            List<UUID> toEliminate = new ArrayList<>();
            for (UUID uuid : new ArrayList<>(arena.getInMatch())) {
                Player p = plugin.getServer().getPlayer(uuid);
                if (p == null || !p.isOnline()) {
                    toEliminate.add(uuid);
                    continue;
                }
                if (p.getLocation().getY() < arenaFloorY) {
                    toEliminate.add(uuid);
                    p.sendMessage(Component.text("You fell out of the arena!", NamedTextColor.RED));
                }
            }
            for (UUID uuid : toEliminate) {
                eliminatePlayer(arena, uuid);
            }
            checkMatchEnd(arena);
        }, 20L, 10L);
        matchCheckTasks.put(arena.getName(), task);
    }

    public void eliminatePlayer(SpleefArena arena, UUID uuid) {
        if (!arena.getInMatch().contains(uuid)) return;
        arena.getInMatch().remove(uuid);
        arena.getEliminated().add(uuid);

        Player p = plugin.getServer().getPlayer(uuid);
        if (p != null) {
            p.sendMessage(Component.text("You have been eliminated from Spleef!", NamedTextColor.RED));
        }

        plugin.getServer().broadcast(Component.text("[Spleef] ", NamedTextColor.GREEN)
                .append(Component.text(p != null ? p.getName() : "A player", NamedTextColor.WHITE))
                .append(Component.text(" has been eliminated! (" + arena.getInMatch().size() + " remaining)", NamedTextColor.YELLOW)));

        checkMatchEnd(arena);
    }

    /** Convenience overload: finds which arena the player is in and eliminates them. */
    public void eliminatePlayer(UUID uuid) {
        for (SpleefArena arena : arenas.values()) {
            if (arena.getInMatch().contains(uuid)) {
                eliminatePlayer(arena, uuid);
                return;
            }
        }
    }

    private void checkMatchEnd(SpleefArena arena) {
        if (!arena.isMatchRunning()) return;
        arena.getInMatch().removeIf(uuid -> {
            Player p = plugin.getServer().getPlayer(uuid);
            return p == null || !p.isOnline();
        });
        if (arena.getInMatch().size() <= 1) {
            UUID winnerUUID = arena.getInMatch().isEmpty() ? null : arena.getInMatch().iterator().next();
            endMatch(arena, winnerUUID);
        }
    }

    private void endMatch(SpleefArena arena, UUID winnerUUID) {
        if (!arena.isMatchRunning()) return;
        arena.setMatchRunning(false);

        BukkitTask ct = countdownTasks.remove(arena.getName());
        if (ct != null) ct.cancel();
        BukkitTask mt = matchCheckTasks.remove(arena.getName());
        if (mt != null) mt.cancel();

        Player winner = winnerUUID != null ? plugin.getServer().getPlayer(winnerUUID) : null;
        if (winner != null) {
            plugin.getServer().broadcast(Component.text("[Spleef] ", NamedTextColor.GREEN)
                    .append(Component.text(winner.getName(), NamedTextColor.GOLD))
                    .append(Component.text(" won Spleef in arena '" + arena.getName() + "'!", NamedTextColor.GREEN)));
        } else {
            plugin.getServer().broadcast(Component.text("[Spleef] The match in arena '" + arena.getName() + "' ended with no winner!", NamedTextColor.YELLOW));
        }

        Set<UUID> toReturn = new HashSet<>(arena.getMatchPlayers());
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (UUID uuid : toReturn) {
                returnToLobby(arena, plugin.getServer().getPlayer(uuid));
            }
            arena.getInMatch().clear();
            arena.getMatchPlayers().clear();
            arena.getEliminated().clear();
            resetArena(arena);
        }, 60L);
    }

    private void resetArena(SpleefArena arena) {
        if (arena.isResetting()) return;
        if (!arena.getSnapshot().isLoaded()) {
            plugin.getLogger().warning("[Spleef] Cannot reset arena '" + arena.getName() + "': snapshot not loaded.");
            return;
        }
        World world = Bukkit.getWorld(arena.getWorldName());
        if (world == null) {
            plugin.getLogger().warning("[Spleef] Cannot reset arena '" + arena.getName() + "': world '" + arena.getWorldName() + "' not loaded.");
            return;
        }
        arena.setResetting(true);
        plugin.getLogger().info("[Spleef] Resetting arena '" + arena.getName() + "'...");
        arena.getSnapshot().restore(plugin, world, RESTORE_BATCH_SIZE, () -> {
            arena.setResetting(false);
            plugin.getLogger().info("[Spleef] Arena '" + arena.getName() + "' reset complete.");
        });
    }

    private void returnToLobby(SpleefArena arena, Player player) {
        if (player == null) return;
        clearInventory(player);
        Location returnLoc = arena.getLobby();
        if (returnLoc == null) returnLoc = plugin.getConfigManager().getHubSpawn();
        if (returnLoc != null && returnLoc.getWorld() != null) {
            player.teleport(returnLoc);
        }
        HubListener.giveOpenerItem(player);
    }

    private void broadcastToArenaQueue(SpleefArena arena, Component msg) {
        for (UUID uuid : arena.getQueue()) {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null) p.sendMessage(msg);
        }
    }

    // ---- Queries ----

    /** Returns {@code true} if the player is queued, in-match, or eliminated in any arena. */
    public boolean isInSpleef(UUID uuid) {
        for (SpleefArena arena : arenas.values()) {
            if (arena.getInMatch().contains(uuid)
                    || arena.getMatchPlayers().contains(uuid)
                    || arena.getQueue().contains(uuid)) return true;
        }
        return false;
    }

    /**
     * Returns the arena whose match this player is actively participating in,
     * or {@code null} if not found.
     */
    public SpleefArena getArenaForPlayer(UUID uuid) {
        for (SpleefArena arena : arenas.values()) {
            if (arena.getInMatch().contains(uuid) || arena.getMatchPlayers().contains(uuid)) return arena;
        }
        return null;
    }

    public void handleDisconnect(UUID uuid) {
        leaveQueue(uuid);
        eliminatePlayer(uuid);
    }

    public void shutdown() {
        countdownTasks.values().forEach(BukkitTask::cancel);
        matchCheckTasks.values().forEach(BukkitTask::cancel);
        countdownTasks.clear();
        matchCheckTasks.clear();
    }

    // ---- Helpers ----

    /**
     * Clear the player's inventory and give the Spleef kit:
     * an Iron Shovel with Efficiency III in hotbar slot 0.
     * The opener nether star is intentionally excluded — it will be
     * restored by {@link com.wildrose.minigameswr.hub.HubListener#giveOpenerItem}
     * when the player returns to the hub after the match.
     */
    private void giveKit(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        ItemStack shovel = new ItemStack(Material.IRON_SHOVEL);
        shovel.addUnsafeEnchantment(Enchantment.EFFICIENCY, 3);
        player.getInventory().setItem(0, shovel);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20f);
    }

    private void clearInventory(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
    }

    private File snapshotFile(String arenaName) {
        return new File(plugin.getDataFolder(), "snapshots" + File.separator + arenaName.toLowerCase() + ".snp");
    }
}
