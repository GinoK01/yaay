package io.josemmo.bukkit.plugin.addon.imgui.display;

import io.josemmo.bukkit.plugin.addon.imgui.config.AddonSettings;
import io.josemmo.bukkit.plugin.addon.imgui.i18n.LocaleService;
import io.josemmo.bukkit.plugin.renderer.FakeImage;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DisplayMetadataService {
    private final File configFile;
    private final LocaleService localeService;
    private volatile Map<String, DisplayOverride> overrides = Collections.emptyMap();

    public DisplayMetadataService(File dataFolder, LocaleService localeService) {
        this.configFile = new File(dataFolder, "display.yml");
        this.localeService = localeService;
    }

    public synchronized void reload() {
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        List<Map<?, ?>> entries = config.getMapList("entries");
        Map<String, DisplayOverride> parsedOverrides = new HashMap<String, DisplayOverride>();

        for (Map<?, ?> row : entries) {
            String path = asString(row.get("path"));
            if (path == null || path.trim().isEmpty()) {
                continue;
            }

            Map<String, String> localizedNames = parseLocalizedNames(row.get("name"));
            Map<String, List<String>> localizedLore = parseLocalizedLore(row.get("lore"));

            Map<?, ?> itemSection = asMap(row.get("item"));
            Material material = parseOptionalMaterial(getOverrideValue(row, itemSection, "material"));
            Integer amount = parseOptionalInt(getOverrideValue(row, itemSection, "amount"), 1, 64);
            Integer width = parseOptionalInt(getOverrideValue(row, itemSection, "width"), 1, 30);
            Integer height = parseOptionalInt(getOverrideValue(row, itemSection, "height"), 0, 30);
            Boolean autoHeight = parseOptionalBoolean(getOverrideValue(row, itemSection, "auto-height"));
            Integer flags = parseOptionalFlags(getOverrideValue(row, itemSection, "flags"));

            parsedOverrides.put(normalizePath(path), new DisplayOverride(
                localizedNames,
                localizedLore,
                material,
                amount,
                width,
                height,
                autoHeight,
                flags
            ));
        }

        overrides = parsedOverrides;
    }

    public DisplayMetadata resolve(
        Player player,
        AddonSettings settings,
        String filename,
        String defaultName,
        List<String> defaultLore
    ) {
        DisplayOverride override = overrides.get(normalizePath(filename));
        if (override == null) {
            return DisplayMetadata.defaultValue(defaultName, defaultLore);
        }

        String lang = normalizeLang(localeService.resolveLanguage(player));
        String fallback = normalizeLang(settings.getFallbackLanguage());

        String customName = chooseLocalizedString(override.localizedNames, lang, fallback);
        List<String> customLore = chooseLocalizedList(override.localizedLore, lang, fallback);

        String resolvedName = customName == null ? defaultName : customName;
        List<String> resolvedLore = customLore == null ? defaultLore : customLore;
        return new DisplayMetadata(
            resolvedName,
            copyList(resolvedLore),
            customName != null,
            customLore != null,
            override.material,
            override.amount,
            override.width,
            override.height,
            override.autoHeight,
            override.flags
        );
    }

    private Object getOverrideValue(Map<?, ?> row, Map<?, ?> itemSection, String key) {
        if (itemSection != null && itemSection.containsKey(key)) {
            return itemSection.get(key);
        }
        return row.get(key);
    }

    private Map<?, ?> asMap(Object value) {
        if (value instanceof Map<?, ?>) {
            return (Map<?, ?>) value;
        }
        return null;
    }

    private Integer parseOptionalInt(Object value, int min, int max) {
        if (value instanceof Number) {
            return clamp(((Number) value).intValue(), min, max);
        }

        if (value instanceof String) {
            try {
                return clamp(Integer.parseInt(((String) value).trim()), min, max);
            } catch (NumberFormatException __) {
                return null;
            }
        }

        return null;
    }

    private Boolean parseOptionalBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            String normalized = ((String) value).trim().toLowerCase(Locale.ROOT);
            if ("true".equals(normalized)) {
                return true;
            }
            if ("false".equals(normalized)) {
                return false;
            }
        }
        return null;
    }

    private Material parseOptionalMaterial(Object value) {
        String materialName = asString(value);
        if (materialName == null) {
            return null;
        }

        Material material = Material.matchMaterial(materialName);
        return material;
    }

    private Integer parseOptionalFlags(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number) {
            return Math.max(0, ((Number) value).intValue());
        }

        if (value instanceof String) {
            return parseFlagsSpec((String) value);
        }

        if (value instanceof List<?>) {
            return parseFlagsTokens((List<?>) value);
        }

        return null;
    }

    private Integer parseFlagsSpec(String spec) {
        if (spec == null || spec.trim().isEmpty()) {
            return null;
        }

        String normalized = spec.trim();
        try {
            return Math.max(0, Integer.parseInt(normalized));
        } catch (NumberFormatException __) {
        }

        String[] split = normalized.split(",");
        List<String> tokens = new ArrayList<String>();
        for (String token : split) {
            tokens.add(token);
        }
        return parseFlagsTokens(tokens);
    }

    private Integer parseFlagsTokens(List<?> rawTokens) {
        if (rawTokens == null || rawTokens.isEmpty()) {
            return null;
        }

        List<String> tokens = new ArrayList<String>();
        boolean usesRelativeOps = false;
        for (Object rawToken : rawTokens) {
            if (rawToken == null) {
                continue;
            }

            String token = String.valueOf(rawToken).trim();
            if (token.isEmpty()) {
                continue;
            }

            if (token.startsWith("+") || token.startsWith("-")) {
                usesRelativeOps = true;
            }
            tokens.add(token);
        }

        if (tokens.isEmpty()) {
            return null;
        }

        int flags = usesRelativeOps ? FakeImage.DEFAULT_GIVE_FLAGS : 0;
        for (String token : tokens) {
            char op = '+';
            String name = token;
            if (token.startsWith("+") || token.startsWith("-")) {
                op = token.charAt(0);
                name = token.substring(1).trim();
            }

            int flagValue = parseFlagName(name.toUpperCase(Locale.ROOT));
            if (flagValue == 0) {
                continue;
            }

            if (op == '-') {
                flags &= ~flagValue;
            } else {
                flags |= flagValue;
            }
        }

        return flags;
    }

    private int parseFlagName(String value) {
        if ("ANIM".equals(value)) {
            return FakeImage.FLAG_ANIMATABLE;
        }
        if ("REMO".equals(value)) {
            return FakeImage.FLAG_REMOVABLE;
        }
        if ("DROP".equals(value)) {
            return FakeImage.FLAG_DROPPABLE;
        }
        if ("GLOW".equals(value)) {
            return FakeImage.FLAG_GLOWING;
        }
        return 0;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private Map<String, String> parseLocalizedNames(Object rawValue) {
        if (rawValue == null) {
            return Collections.emptyMap();
        }

        Map<String, String> localized = new HashMap<String, String>();
        if (rawValue instanceof String) {
            localized.put("default", (String) rawValue);
            return localized;
        }

        if (rawValue instanceof Map<?, ?>) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) rawValue).entrySet()) {
                String key = asString(entry.getKey());
                String value = asString(entry.getValue());
                if (key == null || value == null || value.trim().isEmpty()) {
                    continue;
                }
                localized.put(normalizeLang(key), value);
            }
        }

        return localized;
    }

    private Map<String, List<String>> parseLocalizedLore(Object rawValue) {
        if (rawValue == null) {
            return Collections.emptyMap();
        }

        Map<String, List<String>> localized = new HashMap<String, List<String>>();
        if (rawValue instanceof String) {
            List<String> lore = new ArrayList<String>();
            lore.add((String) rawValue);
            localized.put("default", lore);
            return localized;
        }

        if (rawValue instanceof List<?>) {
            List<String> lore = asStringList((List<?>) rawValue);
            localized.put("default", lore);
            return localized;
        }

        if (rawValue instanceof Map<?, ?>) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) rawValue).entrySet()) {
                String key = asString(entry.getKey());
                if (key == null) {
                    continue;
                }

                Object value = entry.getValue();
                List<String> lore;
                if (value instanceof String) {
                    lore = new ArrayList<String>();
                    lore.add((String) value);
                } else if (value instanceof List<?>) {
                    lore = asStringList((List<?>) value);
                } else {
                    continue;
                }

                localized.put(normalizeLang(key), lore);
            }
        }

        return localized;
    }

    private String chooseLocalizedString(Map<String, String> values, String lang, String fallback) {
        if (values.isEmpty()) {
            return null;
        }

        String value = values.get(lang);
        if (value != null) {
            return value;
        }

        String baseLang = baseLanguage(lang);
        value = values.get(baseLang);
        if (value != null) {
            return value;
        }

        value = values.get(fallback);
        if (value != null) {
            return value;
        }

        String baseFallback = baseLanguage(fallback);
        value = values.get(baseFallback);
        if (value != null) {
            return value;
        }

        return values.get("default");
    }

    private List<String> chooseLocalizedList(Map<String, List<String>> values, String lang, String fallback) {
        if (values.isEmpty()) {
            return null;
        }

        List<String> value = values.get(lang);
        if (value != null) {
            return copyList(value);
        }

        String baseLang = baseLanguage(lang);
        value = values.get(baseLang);
        if (value != null) {
            return copyList(value);
        }

        value = values.get(fallback);
        if (value != null) {
            return copyList(value);
        }

        String baseFallback = baseLanguage(fallback);
        value = values.get(baseFallback);
        if (value != null) {
            return copyList(value);
        }

        value = values.get("default");
        return copyList(value);
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        String str = String.valueOf(value).trim();
        return str.isEmpty() ? null : str;
    }

    private List<String> asStringList(List<?> values) {
        List<String> result = new ArrayList<String>();
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            result.add(String.valueOf(value));
        }
        return result;
    }

    private String normalizePath(String path) {
        return path == null ? "" : path.replace('\\', '/');
    }

    private String normalizeLang(String lang) {
        if (lang == null || lang.trim().isEmpty()) {
            return "default";
        }
        return lang.toLowerCase(Locale.ROOT);
    }

    private String baseLanguage(String lang) {
        int split = lang.indexOf('_');
        if (split <= 0) {
            return lang;
        }
        return lang.substring(0, split);
    }

    private List<String> copyList(List<String> value) {
        if (value == null) {
            return Collections.emptyList();
        }
        return new ArrayList<String>(value);
    }

    private static class DisplayOverride {
        final Map<String, String> localizedNames;
        final Map<String, List<String>> localizedLore;
        final Material material;
        final Integer amount;
        final Integer width;
        final Integer height;
        final Boolean autoHeight;
        final Integer flags;

        DisplayOverride(
            Map<String, String> localizedNames,
            Map<String, List<String>> localizedLore,
            Material material,
            Integer amount,
            Integer width,
            Integer height,
            Boolean autoHeight,
            Integer flags
        ) {
            this.localizedNames = localizedNames;
            this.localizedLore = localizedLore;
            this.material = material;
            this.amount = amount;
            this.width = width;
            this.height = height;
            this.autoHeight = autoHeight;
            this.flags = flags;
        }
    }

    public static class DisplayMetadata {
        private final String nameTemplate;
        private final List<String> loreTemplates;
        private final boolean customName;
        private final boolean customLore;
        private final Material materialOverride;
        private final Integer amountOverride;
        private final Integer widthOverride;
        private final Integer heightOverride;
        private final Boolean autoHeightOverride;
        private final Integer flagsOverride;

        DisplayMetadata(
            String nameTemplate,
            List<String> loreTemplates,
            boolean customName,
            boolean customLore,
            Material materialOverride,
            Integer amountOverride,
            Integer widthOverride,
            Integer heightOverride,
            Boolean autoHeightOverride,
            Integer flagsOverride
        ) {
            this.nameTemplate = nameTemplate;
            this.loreTemplates = loreTemplates == null ? Collections.<String>emptyList() : loreTemplates;
            this.customName = customName;
            this.customLore = customLore;
            this.materialOverride = materialOverride;
            this.amountOverride = amountOverride;
            this.widthOverride = widthOverride;
            this.heightOverride = heightOverride;
            this.autoHeightOverride = autoHeightOverride;
            this.flagsOverride = flagsOverride;
        }

        static DisplayMetadata defaultValue(String nameTemplate, List<String> loreTemplates) {
            return new DisplayMetadata(nameTemplate, loreTemplates, false, false, null, null, null, null, null, null);
        }

        public String getNameTemplate() {
            return nameTemplate;
        }

        public List<String> getLoreTemplates() {
            return loreTemplates;
        }

        public boolean hasCustomName() {
            return customName;
        }

        public boolean hasCustomLore() {
            return customLore;
        }

        public Material getMaterialOverride() {
            return materialOverride;
        }

        public Integer getAmountOverride() {
            return amountOverride;
        }

        public Integer getWidthOverride() {
            return widthOverride;
        }

        public Integer getHeightOverride() {
            return heightOverride;
        }

        public Boolean getAutoHeightOverride() {
            return autoHeightOverride;
        }

        public Integer getFlagsOverride() {
            return flagsOverride;
        }
    }
}
