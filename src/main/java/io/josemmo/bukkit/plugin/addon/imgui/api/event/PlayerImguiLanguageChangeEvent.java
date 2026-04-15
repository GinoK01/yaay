package io.josemmo.bukkit.plugin.addon.imgui.api.event;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import java.util.UUID;

public class PlayerImguiLanguageChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerId;
    private final String previousLanguage;
    private final String newLanguage;
    private final LanguageChangeSource source;

    public PlayerImguiLanguageChangeEvent(
        UUID playerId,
        String previousLanguage,
        String newLanguage,
        LanguageChangeSource source
    ) {
        this.playerId = playerId;
        this.previousLanguage = previousLanguage;
        this.newLanguage = newLanguage;
        this.source = source;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(playerId);
    }

    public String getPreviousLanguage() {
        return previousLanguage;
    }

    public String getNewLanguage() {
        return newLanguage;
    }

    public LanguageChangeSource getSource() {
        return source;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
