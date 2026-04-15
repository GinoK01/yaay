package io.josemmo.bukkit.plugin.addon.imgui.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GuiSession {
    private final UUID playerId;
    private final long token;
    private int page;
    private List<String> filenames;
    private final Map<Integer, String> slotToFilename = new HashMap<Integer, String>();

    public GuiSession(UUID playerId, long token, List<String> filenames, int page) {
        this.playerId = playerId;
        this.token = token;
        this.filenames = new ArrayList<String>(filenames);
        this.page = page;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public long getToken() {
        return token;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public List<String> getFilenames() {
        return filenames;
    }

    public void setFilenames(List<String> filenames) {
        this.filenames = new ArrayList<String>(filenames);
    }

    public Map<Integer, String> getSlotToFilename() {
        return slotToFilename;
    }

    public void clearSlotBindings() {
        slotToFilename.clear();
    }

    public int getMaxPages(int itemsPerPage) {
        int pages = (int) Math.ceil((double) Math.max(1, filenames.size()) / (double) Math.max(1, itemsPerPage));
        return Math.max(1, pages);
    }
}
