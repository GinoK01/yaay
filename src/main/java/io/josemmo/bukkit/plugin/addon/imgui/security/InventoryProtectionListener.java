package io.josemmo.bukkit.plugin.addon.imgui.security;

import io.josemmo.bukkit.plugin.addon.imgui.ImguiAddonPlugin;
import io.josemmo.bukkit.plugin.addon.imgui.config.AddonSettings;
import io.josemmo.bukkit.plugin.addon.imgui.gui.GuiService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.InventoryView;

public class InventoryProtectionListener implements Listener {
    private final ImguiAddonPlugin plugin;
    private final GuiService guiService;

    public InventoryProtectionListener(ImguiAddonPlugin plugin, GuiService guiService) {
        this.plugin = plugin;
        this.guiService = guiService;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();

        InventoryView view = event.getView();
        if (!guiService.isMenuInventory(view.getTopInventory())) {
            return;
        }
        if (!guiService.belongsToPlayer(view.getTopInventory(), player.getUniqueId())) {
            return;
        }

        AddonSettings settings = guiService.getSettings();
        boolean clickedTop = event.getRawSlot() >= 0 && event.getRawSlot() < view.getTopInventory().getSize();

        if (settings.isBlockCreativeActions() && event.getClick() == ClickType.CREATIVE) {
            event.setCancelled(true);
        }
        if (settings.isBlockShiftClick() && event.isShiftClick()) {
            event.setCancelled(true);
        }
        if (settings.isBlockNumberKey() && event.getClick() == ClickType.NUMBER_KEY) {
            event.setCancelled(true);
        }
        if (settings.isBlockCollectToCursor() && event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
        }

        if (settings.isStrictInventoryLock() || clickedTop) {
            event.setCancelled(true);
        }

        if (clickedTop) {
            guiService.handleTopClick(player, event.getRawSlot(), event.getClick());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();

        InventoryView view = event.getView();
        if (!guiService.isMenuInventory(view.getTopInventory())) {
            return;
        }
        if (!guiService.belongsToPlayer(view.getTopInventory(), player.getUniqueId())) {
            return;
        }

        AddonSettings settings = guiService.getSettings();
        if (settings.isStrictInventoryLock()) {
            event.setCancelled(true);
            return;
        }

        if (settings.isBlockDrag()) {
            int topSize = view.getTopInventory().getSize();
            for (Integer rawSlot : event.getRawSlots()) {
                if (rawSlot != null && rawSlot >= 0 && rawSlot < topSize) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getPlayer();

        if (!guiService.isMenuInventory(event.getView().getTopInventory())) {
            return;
        }
        if (!guiService.belongsToPlayer(event.getView().getTopInventory(), player.getUniqueId())) {
            return;
        }

        guiService.onInventoryClosed(player, event.getView().getTopInventory());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        guiService.onPlayerDisconnected(event.getPlayer());
    }
}
