package io.josemmo.bukkit.plugin.addon.imgui.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import java.util.UUID;

public class ImguiMenuHolder implements InventoryHolder {
    private final UUID playerId;
    private final long token;
    private Inventory inventory;

    public ImguiMenuHolder(UUID playerId, long token) {
        this.playerId = playerId;
        this.token = token;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public long getToken() {
        return token;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
