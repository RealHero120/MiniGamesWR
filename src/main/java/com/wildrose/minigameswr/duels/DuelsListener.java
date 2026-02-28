package com.wildrose.minigameswr.duels;

import com.wildrose.minigameswr.MiniGamesWR;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class DuelsListener implements Listener {

    private final MiniGamesWR plugin;

    public DuelsListener(MiniGamesWR plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!plugin.getDuelsManager().isInDuel(player.getUniqueId())) return;

        // Keep death drops suppressed during duel
        event.getDrops().clear();
        event.setDroppedExp(0);

        plugin.getDuelsManager().handleDeath(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getDuelsManager().handleDisconnect(player.getUniqueId());
    }
}
