package com.wildrose.minigameswr.hub;

import com.wildrose.minigameswr.MiniGamesWR;
import com.wildrose.minigameswr.gui.MinigamesGUI;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

public class HubListener implements Listener {

    private final MiniGamesWR plugin;

    public HubListener(MiniGamesWR plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Teleport to hub
        Location hub = plugin.getConfigManager().getHubSpawn();
        if (hub != null && hub.getWorld() != null) {
            player.teleport(hub);
        }
        // Give GUI opener item in slot 0
        giveOpenerItem(player);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || !MinigamesGUI.isOpenerItem(item)) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR
                || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            plugin.getMinigamesGUI().openMenu(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        // Remove the opener nether star from death drops so it never spawns as a ground item.
        event.getDrops().removeIf(MinigamesGUI::isOpenerItem);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (MinigamesGUI.isOpenerItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        if ((current != null && MinigamesGUI.isOpenerItem(current))
                || (cursor != null && MinigamesGUI.isOpenerItem(cursor))) {
            // Only cancel if moving INTO another inventory or swapping
            if (event.getView().getTopInventory() != event.getView().getBottomInventory()) {
                event.setCancelled(true);
            } else if (event.getClickedInventory() != null
                    && !event.getClickedInventory().equals(event.getView().getTopInventory())) {
                // Cancel shift-clicks that would move the item
                if (event.isShiftClick()) {
                    event.setCancelled(true);
                }
            }
        }
    }

    public static void giveOpenerItem(Player player) {
        player.getInventory().setItem(0, MinigamesGUI.createOpenerItem());
    }
}
