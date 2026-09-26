package io.josemmo.bukkit.plugin.addon.imgui.editor;

public enum EditorState {
    EDITING,
    CONFIRMED,
    CANCELLED,
    RECOVERED;

    public boolean isTerminal() {
        return this != EDITING;
    }
}
