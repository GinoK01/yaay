package io.josemmo.bukkit.plugin.addon.imgui.api;

import org.bukkit.command.CommandSender;
import java.util.List;
import java.util.UUID;

public interface ImguiLanguageService {
    List<String> getAvailableLanguages();

    String getLanguageLabel(CommandSender sender, String languageCode);

    String resolveLanguage(CommandSender sender);

    String resolveLanguage(UUID playerId);

    String getStoredLanguage(UUID playerId);

    String setPlayerLanguage(UUID playerId, String languageCode);

    boolean clearPlayerLanguage(UUID playerId);
}
