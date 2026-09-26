package io.josemmo.bukkit.plugin.addon.imgui.editor;

public final class EditorStateMachine {
    private EditorState state = EditorState.EDITING;

    public EditorState state() {
        return state;
    }

    public synchronized boolean tryFinish(EditorState target) {
        if (state != EditorState.EDITING || target == null || !target.isTerminal()) {
            return false;
        }
        state = target;
        return true;
    }
}
