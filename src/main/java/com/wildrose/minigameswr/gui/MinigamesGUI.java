package com.wildrose.minigameswr.gui;

import com.wildrose.minigameswr.MiniGamesWR;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class MinigamesGUI implements Listener {

    private static final String OPENER_KEY = "minigameswr_opener";
    private static final String GUI_KEY = "minigameswr_gui";

    private final MiniGamesWR plugin;
    private final NamespacedKey openerKey;

    public MinigamesGUI(MiniGamesWR plugin) {
        this.plugin = plugin;
        this.openerKey = new NamespacedKey(plugin, OPENER_KEY);
    }

    public static ItemStack createOpenerItem() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Wild Rose Minigames", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Right-click to open!", NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        // Mark as opener item using PDC
        NamespacedKey key = new NamespacedKey("minigameswr", OPENER_KEY);
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isOpenerItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        NamespacedKey key = new NamespacedKey("minigameswr", OPENER_KEY);
        return meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    public void openMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9,
                Component.text("Wild Rose Minigames", NamedTextColor.DARK_PURPLE));

        // Duels item in slot 2
        ItemStack duels = new ItemStack(Material.IRON_SWORD);
        ItemMeta duelsMeta = duels.getItemMeta();
        duelsMeta.displayName(Component.text("Duels (1v1)", NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        duelsMeta.lore(List.of(
                Component.text("Click to join the duel queue!", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        NamespacedKey guiKey = new NamespacedKey(plugin, GUI_KEY);
        duelsMeta.getPersistentDataContainer().set(guiKey, PersistentDataType.STRING, "duels");
        duels.setItemMeta(duelsMeta);
        inv.setItem(2, duels);

        // Spliff item in slot 6
        ItemStack spliff = new ItemStack(Material.SNOW_BLOCK);
        ItemMeta spliffMeta = spliff.getItemMeta();
        spliffMeta.displayName(Component.text("Spliff", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        spliffMeta.lore(List.of(
                Component.text("Click to join the Spliff queue!", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        spliffMeta.getPersistentDataContainer().set(guiKey, PersistentDataType.STRING, "spliff");
        spliff.setItemMeta(spliffMeta);
        inv.setItem(6, spliff);

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        // Check if this is our GUI
        Component title = event.getView().title();
        if (!Component.text("Wild Rose Minigames", NamedTextColor.DARK_PURPLE).equals(title)) return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        NamespacedKey guiKey = new NamespacedKey(plugin, GUI_KEY);
        String action = meta.getPersistentDataContainer().get(guiKey, PersistentDataType.STRING);
        if (action == null) return;

        player.closeInventory();

        switch (action) {
            case "duels" -> plugin.getDuelsManager().joinQueue(player);
            case "spliff" -> plugin.getSpliffManager().joinQueue(player);
        }
    }
}
