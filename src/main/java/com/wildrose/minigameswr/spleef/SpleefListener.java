package com.wildrose.minigameswr.spleef;

import com.wildrose.minigameswr.MiniGamesWR;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class SpleefListener implements Listener {

    private final MiniGamesWR plugin;

    public SpleefListener(MiniGamesWR plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        SpleefManager sm = plugin.getSpleefManager();
        if (!sm.isInSpleef(player.getUniqueId())) return;

        SpleefArena arena = sm.getArenaForPlayer(player.getUniqueId());
        if (arena == null || !arena.isMatchRunning()) {
            event.setCancelled(true);
            return;
        }

        // Only allow breaking blocks that are inside the arena region.
        if (!arena.contains(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (plugin.getSpleefManager().isInSpleef(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        SpleefManager sm = plugin.getSpleefManager();
        if (!sm.isInSpleef(player.getUniqueId())) return;

        event.getDrops().clear();
        event.setDroppedExp(0);
        sm.eliminatePlayer(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getSpleefManager().handleDisconnect(event.getPlayer().getUniqueId());
    }
}
