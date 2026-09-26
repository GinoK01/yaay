package io.josemmo.bukkit.plugin.addon.imgui.editor;

import org.bukkit.inventory.ItemStack;

public final class EditorSession {
    private final EditorStateMachine machine = new EditorStateMachine();
    private ItemStack held;
    private String filename = "";
    private int originalWidth;
    private int originalHeight;
    private int width;
    private int height;

    public EditorState state() {
        return machine.state();
    }

    public synchronized boolean hasItem() {
        return held != null && machine.state() == EditorState.EDITING;
    }

    public synchronized ItemStack view() {
        return held == null ? null : held.clone();
    }

    public synchronized String filename() {
        return filename;
    }

    public synchronized int originalWidth() {
        return originalWidth;
    }

    public synchronized int originalHeight() {
        return originalHeight;
    }

    public synchronized int width() {
        return width;
    }

    public synchronized int height() {
        return height;
    }

    public synchronized boolean hold(
        ItemStack item,
        String filename,
        int originalWidth,
        int originalHeight,
        int width,
        int height
    ) {
        if (item == null || machine.state() != EditorState.EDITING) {
            return false;
        }
        this.held = item.clone();
        this.filename = filename == null ? "" : filename;
        this.originalWidth = originalWidth;
        this.originalHeight = originalHeight;
        this.width = width;
        this.height = height;
        return true;
    }

    public synchronized ItemStack release() {
        if (held == null || machine.state() != EditorState.EDITING) {
            return null;
        }
        ItemStack item = held;
        held = null;
        return item.clone();
    }

    public synchronized void clampDraft(int minWidth, int maxWidth, int minHeight, int maxHeight) {
        if (held == null || machine.state() != EditorState.EDITING) {
            return;
        }
        width = Math.max(minWidth, Math.min(maxWidth, width));
        height = Math.max(minHeight, Math.min(maxHeight, height));
    }

    public synchronized boolean adjustWidth(int delta, int min, int max) {
        return adjust(true, delta, min, max);
    }

    public synchronized boolean adjustHeight(int delta, int min, int max) {
        return adjust(false, delta, min, max);
    }

    public synchronized ItemStack finish(EditorState target) {
        if (!machine.tryFinish(target)) {
            return null;
        }
        ItemStack item = held;
        held = null;
        return item == null ? null : item.clone();
    }

    private boolean adjust(boolean horizontal, int delta, int min, int max) {
        if (held == null || machine.state() != EditorState.EDITING) {
            return false;
        }
        int current = horizontal ? width : height;
        int next = current + delta;
        if (next < min || next > max || next == current) {
            return false;
        }
        if (horizontal) {
            width = next;
        } else {
            height = next;
        }
        return true;
    }
}
