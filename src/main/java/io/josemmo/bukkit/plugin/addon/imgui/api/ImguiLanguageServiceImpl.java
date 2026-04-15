package io.josemmo.bukkit.plugin.addon.imgui.api;

import io.josemmo.bukkit.plugin.addon.imgui.i18n.LocaleService;
import org.bukkit.command.CommandSender;
import java.util.List;
import java.util.UUID;

public class ImguiLanguageServiceImpl implements ImguiLanguageService {
    private final LocaleService localeService;

    public ImguiLanguageServiceImpl(LocaleService localeService) {
        this.localeService = localeService;
    }

    @Override
    public List<String> getAvailableLanguages() {
        return localeService.getAvailableLanguages();
    }

    @Override
    public String getLanguageLabel(CommandSender sender, String languageCode) {
        return localeService.getLanguageLabel(sender, languageCode);
    }

    @Override
    public String resolveLanguage(CommandSender sender) {
        return localeService.resolveLanguage(sender);
    }

    @Override
    public String resolveLanguage(UUID playerId) {
        return localeService.resolveLanguage(playerId);
    }

    @Override
    public String getStoredLanguage(UUID playerId) {
        return localeService.getPlayerPreferredLanguage(playerId);
    }

    @Override
    public String setPlayerLanguage(UUID playerId, String languageCode) {
        return localeService.setPlayerLanguage(playerId, languageCode);
    }

    @Override
    public boolean clearPlayerLanguage(UUID playerId) {
        return localeService.clearPlayerLanguage(playerId);
    }
}
