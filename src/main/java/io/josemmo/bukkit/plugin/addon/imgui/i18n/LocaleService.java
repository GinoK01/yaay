package io.josemmo.bukkit.plugin.addon.imgui.i18n;

import io.josemmo.bukkit.plugin.addon.imgui.ImguiAddonPlugin;
import io.josemmo.bukkit.plugin.addon.imgui.api.event.LanguageChangeSource;
import io.josemmo.bukkit.plugin.addon.imgui.api.event.PlayerImguiLanguageChangeEvent;
import io.josemmo.bukkit.plugin.addon.imgui.config.AddonSettings;
import io.josemmo.bukkit.plugin.addon.imgui.util.Texts;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class LocaleService {
    private final ImguiAddonPlugin plugin;
    private final LanguagePreferenceStore preferenceStore;
    private final Map<String, FileConfiguration> localeFiles = new HashMap<String, FileConfiguration>();
    private volatile AddonSettings settings;

    public LocaleService(ImguiAddonPlugin plugin) {
        this.plugin = plugin;
        this.preferenceStore = new LanguagePreferenceStore(plugin);
    }

    public synchronized void reload(AddonSettings newSettings) {
        this.settings = newSettings;
        localeFiles.clear();

        File localesDir = new File(plugin.getDataFolder(), "locales");
        if (!localesDir.exists()) {
            localesDir.mkdirs();
        }

        File[] files = localesDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.toLowerCase(Locale.ROOT).endsWith(".yml");
            }
        });

        if (files == null || files.length == 0) {
            return;
        }

        for (File file : files) {
            String name = file.getName();
            String lang = name.substring(0, name.length() - 4).toLowerCase(Locale.ROOT);
            localeFiles.put(lang, YamlConfiguration.loadConfiguration(file));
        }
    }

    public String tr(CommandSender sender, String key) {
        return tr(sender, key, Collections.<String, String>emptyMap());
    }

    public String tr(CommandSender sender, String key, Map<String, String> placeholders) {
        AddonSettings localSettings = settings;
        if (localSettings == null) {
            return key;
        }

        String lang = resolveLanguage(sender);

        String message = getLocalizedString(lang, "messages." + key);
        if (message == null) {
            message = getLocalizedString(normalizeLanguageCode(localSettings.getFallbackLanguage()), "messages." + key);
        }
        if (message == null) {
            message = key;
        }

        String prefix = Texts.applyPlaceholders(localSettings.getPrefix(), placeholders);
        String body = Texts.applyPlaceholders(message, placeholders);
        return prefix + body;
    }

    public String trRaw(CommandSender sender, String key, String defaultValue, Map<String, String> placeholders) {
        AddonSettings localSettings = settings;
        if (localSettings == null) {
            return Texts.applyPlaceholders(defaultValue, placeholders);
        }

        String lang = resolveLanguage(sender);
        String value = getLocalizedString(lang, key);
        if (value == null) {
            value = getLocalizedString(normalizeLanguageCode(localSettings.getFallbackLanguage()), key);
        }
        if (value == null) {
            value = defaultValue;
        }
        return Texts.applyPlaceholders(value, placeholders);
    }

    public List<String> trRawList(CommandSender sender, String key, List<String> defaultValue, Map<String, String> placeholders) {
        AddonSettings localSettings = settings;
        List<String> values = null;
        if (localSettings != null) {
            String lang = resolveLanguage(sender);
            values = getLocalizedList(lang, key);
            if (values == null) {
                values = getLocalizedList(normalizeLanguageCode(localSettings.getFallbackLanguage()), key);
            }
        }

        if (values == null) {
            values = defaultValue == null ? Collections.<String>emptyList() : new ArrayList<String>(defaultValue);
        }

        List<String> output = new ArrayList<String>(values.size());
        for (String value : values) {
            output.add(Texts.applyPlaceholders(value, placeholders));
        }
        return output;
    }

    public List<String> getAvailableLanguages() {
        List<String> values = new ArrayList<String>(localeFiles.keySet());
        Collections.sort(values);
        return values;
    }

    public String cyclePlayerLanguage(Player player) {
        if (player == null) {
            return "en";
        }

        List<String> languages = getAvailableLanguages();
        if (languages.isEmpty()) {
            return resolveLanguage(player.getUniqueId());
        }

        String current = resolveLanguage(player.getUniqueId());
        int index = languages.indexOf(current);
        int nextIndex = index < 0 ? 0 : (index + 1) % languages.size();
        String next = languages.get(nextIndex);
        return setPlayerLanguage(player.getUniqueId(), next, LanguageChangeSource.GUI_BUTTON);
    }

    public String getLanguageLabel(CommandSender sender, String languageCode) {
        String normalized = normalizeLanguageCode(languageCode);
        if (normalized == null) {
            return "UNKNOWN";
        }
        return trRaw(sender, "languages." + normalized, normalized.toUpperCase(Locale.ROOT), Collections.<String, String>emptyMap());
    }

    public String getPlayerPreferredLanguage(Player player) {
        return player == null ? null : getPlayerPreferredLanguage(player.getUniqueId());
    }

    public String getPlayerPreferredLanguage(UUID playerId) {
        if (playerId == null) {
            return null;
        }

        String value = preferenceStore.getLanguage(playerId);
        return findBestAvailableLanguage(value);
    }

    public String setPlayerLanguage(UUID playerId, String requestedLanguage) {
        return setPlayerLanguage(playerId, requestedLanguage, LanguageChangeSource.API_CALL);
    }

    public String setPlayerLanguage(UUID playerId, String requestedLanguage, LanguageChangeSource source) {
        AddonSettings localSettings = settings;
        if (localSettings == null || playerId == null) {
            return "en";
        }

        String previousLanguage = resolveLanguage(playerId);
        String targetLanguage = normalizeRequestedLanguage(requestedLanguage, localSettings);
        if (!preferenceStore.setLanguage(playerId, targetLanguage)) {
            return previousLanguage;
        }

        String nextLanguage = resolveLanguage(playerId);
        publishLanguageChange(playerId, previousLanguage, nextLanguage, source);
        return nextLanguage;
    }

    public boolean clearPlayerLanguage(UUID playerId) {
        return preferenceStore.clearLanguage(playerId);
    }

    public String resolveLanguage(CommandSender sender) {
        AddonSettings localSettings = settings;
        if (localSettings == null) {
            return "en";
        }

        String fallback = normalizeLanguageCode(localSettings.getFallbackLanguage());
        if (sender instanceof Player) {
            return resolveLanguage(((Player) sender).getUniqueId());
        }

        String resolvedFallback = findBestAvailableLanguage(fallback);
        if (resolvedFallback != null) {
            return resolvedFallback;
        }

        List<String> availableLanguages = getAvailableLanguages();
        if (!availableLanguages.isEmpty()) {
            return availableLanguages.get(0);
        }
        return fallback == null ? "en" : fallback;
    }

    public String resolveLanguage(UUID playerId) {
        AddonSettings localSettings = settings;
        if (localSettings == null) {
            return "en";
        }

        String fallback = normalizeLanguageCode(localSettings.getFallbackLanguage());
        String preferredLanguage = getPlayerPreferredLanguage(playerId);
        if (preferredLanguage != null) {
            return preferredLanguage;
        }

        if (playerId != null && localSettings.isUsePlayerLocale()) {
            String localeLanguage = fromPlayerLocale(playerId, fallback);
            String resolvedLocaleLanguage = findBestAvailableLanguage(localeLanguage);
            if (resolvedLocaleLanguage != null) {
                return resolvedLocaleLanguage;
            }
        }

        String resolvedFallback = findBestAvailableLanguage(fallback);
        if (resolvedFallback != null) {
            return resolvedFallback;
        }

        List<String> availableLanguages = getAvailableLanguages();
        if (!availableLanguages.isEmpty()) {
            return availableLanguages.get(0);
        }
        return fallback == null ? "en" : fallback;
    }

    private String normalizeRequestedLanguage(String requestedLanguage, AddonSettings localSettings) {
        String requested = findBestAvailableLanguage(requestedLanguage);
        if (requested != null) {
            return requested;
        }

        String fallback = findBestAvailableLanguage(localSettings.getFallbackLanguage());
        if (fallback != null) {
            return fallback;
        }

        List<String> availableLanguages = getAvailableLanguages();
        if (!availableLanguages.isEmpty()) {
            return availableLanguages.get(0);
        }

        String normalized = normalizeLanguageCode(requestedLanguage);
        return normalized == null ? "en" : normalized;
    }

    private void publishLanguageChange(
        UUID playerId,
        String previousLanguage,
        String newLanguage,
        LanguageChangeSource source
    ) {
        if (playerId == null || previousLanguage == null || newLanguage == null || previousLanguage.equals(newLanguage)) {
            return;
        }

        LanguageChangeSource effectiveSource = source == null ? LanguageChangeSource.API_CALL : source;
        plugin.getServer().getPluginManager().callEvent(
            new PlayerImguiLanguageChangeEvent(playerId, previousLanguage, newLanguage, effectiveSource)
        );
    }

    private String fromPlayerLocale(UUID playerId, String fallback) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            return fallback;
        }

        String locale = player.getLocale();
        if (locale == null || locale.trim().isEmpty()) {
            return fallback;
        }
        String normalized = locale.toLowerCase(Locale.ROOT).replace('-', '_');
        int split = normalized.indexOf('_');
        if (split > 0) {
            normalized = normalized.substring(0, split);
        }
        return normalized;
    }

    private String getLocalizedString(String lang, String key) {
        FileConfiguration config = localeFiles.get(lang);
        if (config == null) {
            return null;
        }
        return config.getString(key);
    }

    private List<String> getLocalizedList(String lang, String key) {
        FileConfiguration config = localeFiles.get(lang);
        if (config == null || !config.contains(key)) {
            return null;
        }

        if (config.isList(key)) {
            return config.getStringList(key);
        }

        if (config.isString(key)) {
            List<String> values = new ArrayList<String>();
            values.add(config.getString(key));
            return values;
        }

        return null;
    }

    private String normalizeLanguageCode(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private String findBestAvailableLanguage(String requestedLanguage) {
        String normalized = normalizeLanguageCode(requestedLanguage);
        if (normalized == null) {
            return null;
        }

        if (localeFiles.containsKey(normalized)) {
            return normalized;
        }

        int split = normalized.indexOf('_');
        if (split > 0) {
            String base = normalized.substring(0, split);
            if (localeFiles.containsKey(base)) {
                return base;
            }
        }

        return null;
    }
}
