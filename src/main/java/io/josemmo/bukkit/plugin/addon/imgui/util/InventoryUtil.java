package io.josemmo.bukkit.plugin.addon.imgui.util;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class InventoryUtil {
    private InventoryUtil() {
    }

    public static boolean hasSpaceFor(Inventory inventory, ItemStack item) {
        if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) {
            return true;
        }

        int remaining = item.getAmount();
        ItemStack[] contents = inventory.getStorageContents();
        for (ItemStack current : contents) {
            if (current == null || current.getType() == Material.AIR) {
                continue;
            }
            if (!current.isSimilar(item)) {
                continue;
            }
            int free = current.getMaxStackSize() - current.getAmount();
            if (free > 0) {
                remaining -= free;
                if (remaining <= 0) {
                    return true;
                }
            }
        }

        for (ItemStack current : contents) {
            if (current == null || current.getType() == Material.AIR) {
                remaining -= item.getMaxStackSize();
                if (remaining <= 0) {
                    return true;
                }
            }
        }

        return remaining <= 0;
    }
}
