package com.wildrose.minigameswr.spliff;

import com.wildrose.minigameswr.MiniGamesWR;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class SpliffListener implements Listener {

    private final MiniGamesWR plugin;

    public SpliffListener(MiniGamesWR plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        SpliffManager sm = plugin.getSpliffManager();
        if (!sm.isInSpliff(player.getUniqueId())) return;

        if (!sm.canBreakBlock(player, event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (plugin.getSpliffManager().isInSpliff(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        SpliffManager sm = plugin.getSpliffManager();
        if (!sm.isInSpliff(player.getUniqueId())) return;

        event.getDrops().clear();
        event.setDroppedExp(0);
        sm.eliminatePlayer(player.getUniqueId(), true);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getSpliffManager().handleDisconnect(event.getPlayer().getUniqueId());
    }
}
