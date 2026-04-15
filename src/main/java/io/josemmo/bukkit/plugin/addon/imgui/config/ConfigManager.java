package io.josemmo.bukkit.plugin.addon.imgui.config;

import io.josemmo.bukkit.plugin.addon.imgui.ImguiAddonPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ConfigManager {
    private final ImguiAddonPlugin plugin;

    public ConfigManager(ImguiAddonPlugin plugin) {
        this.plugin = plugin;
    }

    public void ensureDefaults() {
        saveIfNotExists("config.yml");
        saveIfNotExists("gui.yml");
        saveIfNotExists("limits.yml");
        saveIfNotExists("display.yml");
        saveIfNotExists("locales/en.yml");
        saveIfNotExists("locales/es.yml");
    }

    private void saveIfNotExists(String path) {
        File file = new File(plugin.getDataFolder(), path);
        if (!file.exists()) {
            plugin.saveResource(path, false);
        }
    }

    public AddonSettings loadSettings() {
        FileConfiguration config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "config.yml"));
        FileConfiguration gui = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "gui.yml"));
        FileConfiguration limits = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "limits.yml"));

        boolean enabled = config.getBoolean("enabled", true);

        boolean usePlayerLocale = config.getBoolean("language.use-player-locale", false);
        long languageChangeCooldownMs = Math.max(0L, config.getLong("language.change-cooldown-ms", 0L));
        String fallbackLanguage = config.getString("language.fallback-language", "en").toLowerCase();
        String prefix = config.getString("language.prefix", "&8[&bImgui&8] &7");

        String usePermission = config.getString("command.use-permission", "yamipa.imgui.use");
        String reloadPermission = config.getString("command.reload-permission", "yamipa.imgui.reload");
        boolean reloadSubcommandEnabled = config.getBoolean("command.reload-subcommand-enabled", true);

        boolean requirePublicOrOwn = config.getBoolean("visibility.require-public-or-own", true);
        String ownPathPattern = config.getString("visibility.own-path-pattern", "^private/#player#/.+");
        String publicPathPattern = config.getString("visibility.public-path-pattern", "^public/.+");

        Material claimItemMaterial = parseMaterial(config.getString("item.material", "ITEM_FRAME"), Material.ITEM_FRAME);
        int claimItemAmount = clamp(config.getInt("item.amount", 1), 1, 64);
        int claimItemWidth = clamp(config.getInt("item.width", 3), 1, 30);
        int claimItemHeight = Math.max(0, config.getInt("item.height", 0));
        boolean claimItemAutoHeight = config.getBoolean("item.auto-height", true);
        int claimItemFlags = Math.max(0, config.getInt("item.flags", 7));
        String claimItemNameFormat = config.getString("item.name-format", "&b{filename}");
        boolean claimItemClearLore = config.getBoolean("item.clear-lore", true);
        List<String> claimItemLore = nonNullStringList(config.getStringList("item.lore"));

        boolean asyncOpen = config.getBoolean("performance.async-open", true);
        long cacheTtlMs = Math.max(0L, config.getLong("performance.cache-ttl-ms", 5000L));

        boolean closeMenusOnReload = config.getBoolean("reload.close-open-menus", false);
        boolean autoReloadEnabled = config.getBoolean("reload.auto-reload-enabled", true);
        long autoReloadDebounceMs = Math.max(0L, config.getLong("reload.auto-reload-debounce-ms", 800L));

        String menuTitle = gui.getString("title", "&0Image Claim Menu");
        int menuRows = clamp(gui.getInt("rows", 6), 1, 6);
        List<Integer> contentSlots = sanitizeSlots(gui.getIntegerList("content-slots"), menuRows * 9);
        if (contentSlots.isEmpty()) {
            contentSlots = defaultContentSlots(menuRows * 9);
        }

        boolean fillerEnabled = gui.getBoolean("filler.enabled", true);
        Material fillerMaterial = parseMaterial(gui.getString("filler.material", "GRAY_STAINED_GLASS_PANE"), Material.GRAY_STAINED_GLASS_PANE);
        String fillerName = gui.getString("filler.name", " ");
        List<String> fillerLore = nonNullStringList(gui.getStringList("filler.lore"));

        Material imageIconMaterial = parseMaterial(gui.getString("image-icon.material", "PAPER"), Material.PAPER);
        String imageIconNameFormat = gui.getString("image-icon.name-format", "&f{filename}");
        List<String> imageIconLore = nonNullStringList(gui.getStringList("image-icon.lore"));

        int previousSlot = clamp(gui.getInt("navigation.previous.slot", menuRows * 9 - 9), 0, menuRows * 9 - 1);
        Material previousMaterial = parseMaterial(gui.getString("navigation.previous.material", "ARROW"), Material.ARROW);
        String previousName = gui.getString("navigation.previous.name", "&ePrevious page");
        List<String> previousLore = nonNullStringList(gui.getStringList("navigation.previous.lore"));

        int nextSlot = clamp(gui.getInt("navigation.next.slot", menuRows * 9 - 1), 0, menuRows * 9 - 1);
        Material nextMaterial = parseMaterial(gui.getString("navigation.next.material", "ARROW"), Material.ARROW);
        String nextName = gui.getString("navigation.next.name", "&eNext page");
        List<String> nextLore = nonNullStringList(gui.getStringList("navigation.next.lore"));

        int refreshSlot = clamp(gui.getInt("navigation.refresh.slot", menuRows * 9 - 5), 0, menuRows * 9 - 1);
        Material refreshMaterial = parseMaterial(gui.getString("navigation.refresh.material", "SUNFLOWER"), Material.SUNFLOWER);
        String refreshName = gui.getString("navigation.refresh.name", "&bRefresh");
        List<String> refreshLore = nonNullStringList(gui.getStringList("navigation.refresh.lore"));

        boolean languageMenuEnabled = gui.getBoolean("navigation.language.enabled", true);
        int languageSlot = clamp(gui.getInt("navigation.language.slot", menuRows * 9 - 6), 0, menuRows * 9 - 1);
        Material languageMaterial = parseMaterial(gui.getString("navigation.language.material", "GLOBE_BANNER_PATTERN"), Material.BOOK);
        String languageName = gui.getString("navigation.language.name", "&aLanguage");
        List<String> languageLore = nonNullStringList(gui.getStringList("navigation.language.lore"));

        int infoSlot = clamp(gui.getInt("navigation.info.slot", menuRows * 9 - 4), 0, menuRows * 9 - 1);
        Material infoMaterial = parseMaterial(gui.getString("navigation.info.material", "BOOK"), Material.BOOK);
        String infoName = gui.getString("navigation.info.name", "&fInfo");
        List<String> infoLore = nonNullStringList(gui.getStringList("navigation.info.lore"));

        int closeSlot = clamp(gui.getInt("navigation.close.slot", menuRows * 9 - 1), 0, menuRows * 9 - 1);
        Material closeMaterial = parseMaterial(gui.getString("navigation.close.material", "BARRIER"), Material.BARRIER);
        String closeName = gui.getString("navigation.close.name", "&cClose");
        List<String> closeLore = nonNullStringList(gui.getStringList("navigation.close.lore"));

        int emptySlot = clamp(gui.getInt("empty-state.slot", menuRows * 9 / 2), 0, menuRows * 9 - 1);
        Material emptyMaterial = parseMaterial(gui.getString("empty-state.material", "CHEST_MINECART"), Material.CHEST_MINECART);
        String emptyName = gui.getString("empty-state.name", "&7No available images");
        List<String> emptyLore = nonNullStringList(gui.getStringList("empty-state.lore"));

        boolean hourlyLimitEnabled = limits.getBoolean("hourly-limit.enabled", true);
        int hourlyLimitMaxItems = Math.max(1, limits.getInt("hourly-limit.max-items-per-hour", 64));

        boolean claimCooldownEnabled = limits.getBoolean("claim-rate-limit.enabled", true);
        long claimCooldownMs = Math.max(0L, limits.getLong("claim-rate-limit.cooldown-ms", 200L));

        boolean strictInventoryLock = limits.getBoolean("anti-exploit.strict-inventory-lock", true);
        boolean blockShiftClick = limits.getBoolean("anti-exploit.block-shift-click", true);
        boolean blockNumberKey = limits.getBoolean("anti-exploit.block-number-key", true);
        boolean blockCollectToCursor = limits.getBoolean("anti-exploit.block-collect-to-cursor", true);
        boolean blockDrag = limits.getBoolean("anti-exploit.block-drag", true);
        boolean blockCreativeActions = limits.getBoolean("anti-exploit.block-creative-actions", true);
        boolean claimLockEnabled = limits.getBoolean("anti-exploit.claim-lock-enabled", true);

        return new AddonSettings(
            enabled,
            usePlayerLocale,
            languageChangeCooldownMs,
            fallbackLanguage,
            prefix,
            usePermission,
            reloadPermission,
            reloadSubcommandEnabled,
            requirePublicOrOwn,
            ownPathPattern,
            publicPathPattern,
            claimItemMaterial,
            claimItemAmount,
            claimItemWidth,
            claimItemHeight,
            claimItemAutoHeight,
            claimItemFlags,
            claimItemNameFormat,
            claimItemClearLore,
            claimItemLore,
            asyncOpen,
            cacheTtlMs,
            closeMenusOnReload,
            autoReloadEnabled,
            autoReloadDebounceMs,
            menuTitle,
            menuRows,
            contentSlots,
            fillerEnabled,
            fillerMaterial,
            fillerName,
            fillerLore,
            imageIconMaterial,
            imageIconNameFormat,
            imageIconLore,
            previousSlot,
            previousMaterial,
            previousName,
            previousLore,
            nextSlot,
            nextMaterial,
            nextName,
            nextLore,
            refreshSlot,
            refreshMaterial,
            refreshName,
            refreshLore,
            languageMenuEnabled,
            languageSlot,
            languageMaterial,
            languageName,
            languageLore,
            infoSlot,
            infoMaterial,
            infoName,
            infoLore,
            closeSlot,
            closeMaterial,
            closeName,
            closeLore,
            emptySlot,
            emptyMaterial,
            emptyName,
            emptyLore,
            hourlyLimitEnabled,
            hourlyLimitMaxItems,
            claimCooldownEnabled,
            claimCooldownMs,
            strictInventoryLock,
            blockShiftClick,
            blockNumberKey,
            blockCollectToCursor,
            blockDrag,
            blockCreativeActions,
            claimLockEnabled
        );
    }

    private Material parseMaterial(String value, Material fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        Material material = Material.matchMaterial(value.trim());
        return material == null ? fallback : material;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private List<String> nonNullStringList(List<String> values) {
        if (values == null) {
            return Collections.emptyList();
        }
        return new ArrayList<String>(values);
    }

    private List<Integer> sanitizeSlots(List<Integer> slots, int inventorySize) {
        if (slots == null || slots.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> sanitized = new ArrayList<Integer>();
        for (Integer slot : slots) {
            if (slot == null) {
                continue;
            }
            if (slot >= 0 && slot < inventorySize && !sanitized.contains(slot)) {
                sanitized.add(slot);
            }
        }
        return sanitized;
    }

    private List<Integer> defaultContentSlots(int inventorySize) {
        int upperBound = Math.max(0, inventorySize - 9);
        List<Integer> values = new ArrayList<Integer>();
        for (int i = 0; i < upperBound; i++) {
            values.add(i);
        }
        return values;
    }
}
