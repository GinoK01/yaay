package io.josemmo.bukkit.plugin.addon.imgui.editor;

import io.josemmo.bukkit.plugin.addon.imgui.util.InventoryUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.Map;

public final class ItemDelivery {
    private ItemDelivery() {
    }

    public static boolean giveOrDrop(Player player, ItemStack item) {
        if (player == null || item == null) {
            return false;
        }
        ItemStack copy = item.clone();
        if (!InventoryUtil.hasSpaceFor(player.getInventory(), copy)) {
            drop(player, copy);
            return false;
        }
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(copy);
        if (leftovers.isEmpty()) {
            return true;
        }
        for (ItemStack leftover : leftovers.values()) {
            drop(player, leftover);
        }
        return false;
    }

    public static void drop(Player player, ItemStack item) {
        if (player == null || item == null || player.getWorld() == null) {
            return;
        }
        Location location = player.getLocation();
        player.getWorld().dropItemNaturally(location, item.clone());
    }
}
