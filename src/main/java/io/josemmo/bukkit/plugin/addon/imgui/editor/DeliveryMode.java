package io.josemmo.bukkit.plugin.addon.imgui.editor;

public enum DeliveryMode {
    INVENTORY,
    DROP;

    public static DeliveryMode forSpace(boolean hasSpace) {
        return hasSpace ? INVENTORY : DROP;
    }
}
