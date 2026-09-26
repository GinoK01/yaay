package io.josemmo.bukkit.plugin.addon.imgui.util;

import io.josemmo.bukkit.plugin.YamipaPlugin;
import io.josemmo.bukkit.plugin.addon.imgui.config.AddonSettings;
import io.josemmo.bukkit.plugin.addon.imgui.display.DisplayMetadataService;
import io.josemmo.bukkit.plugin.renderer.FakeImage;
import io.josemmo.bukkit.plugin.storage.ImageFile;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ImageItemFactory {
    private ImageItemFactory() {
    }

    public static ItemStack createClaimItem(
        Player player,
        ImageFile imageFile,
        AddonSettings settings,
        DisplayMetadataService.DisplayMetadata displayMetadata
    ) {
        if (imageFile.getSize() == null) {
            throw new IllegalArgumentException("Invalid image file");
        }

        ResolvedSize resolvedSize = resolveSize(player, imageFile, settings, displayMetadata);
        int width = resolvedSize.getWidth();
        int height = resolvedSize.getHeight();

        int amount = getOverrideOrDefault(
            displayMetadata == null ? null : displayMetadata.getAmountOverride(),
            settings.getClaimItemAmount(),
            1,
            64
        );
        int flags = Math.max(0, displayMetadata != null && displayMetadata.getFlagsOverride() != null
            ? displayMetadata.getFlagsOverride()
            : settings.getClaimItemFlags());

        ItemStack item = new ItemStack(
            displayMetadata != null && displayMetadata.getMaterialOverride() != null
                ? displayMetadata.getMaterialOverride()
                : settings.getClaimItemMaterial(),
            amount
        );
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        applyDisplayMetadata(meta, imageFile.getFilename(), amount, width, height, settings, displayMetadata);
        writeImageData(meta, imageFile.getFilename(), width, height, flags);

        item.setItemMeta(meta);
        return item;
    }

    public static ImageData readImageData(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        PersistentDataContainer data = meta.getPersistentDataContainer();
        String filename = data.get(key("filename"), PersistentDataType.STRING);
        Integer width = data.get(key("width"), PersistentDataType.INTEGER);
        Integer height = data.get(key("height"), PersistentDataType.INTEGER);
        Integer flags = data.get(key("flags"), PersistentDataType.INTEGER);
        if (filename == null || filename.trim().isEmpty() || width == null || height == null || flags == null) {
            return null;
        }
        if (width < 1 || height < 0) {
            return null;
        }
        return new ImageData(filename, width, height, flags);
    }

    public static void writeImageData(ItemMeta meta, String filename, int width, int height, int flags) {
        if (meta == null) {
            return;
        }
        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(key("filename"), PersistentDataType.STRING, filename);
        data.set(key("width"), PersistentDataType.INTEGER, width);
        data.set(key("height"), PersistentDataType.INTEGER, height);
        data.set(key("flags"), PersistentDataType.INTEGER, flags);
    }

    private static NamespacedKey key(String name) {
        return new NamespacedKey(YamipaPlugin.getInstance(), name);
    }

    public static void applyDisplayMetadata(
        ItemMeta meta,
        String filename,
        int amount,
        int width,
        int height,
        AddonSettings settings,
        DisplayMetadataService.DisplayMetadata displayMetadata
    ) {
        Map<String, String> placeholders = new HashMap<String, String>();
        String basename = Texts.getBasename(filename);
        placeholders.put("amount", String.valueOf(amount));
        placeholders.put("filename", basename);
        placeholders.put("basename", basename);
        placeholders.put("filepath", Texts.getPathWithoutExtension(filename));
        placeholders.put("width", String.valueOf(width));
        placeholders.put("height", String.valueOf(height));

        String nameTemplate = settings.getClaimItemNameFormat();
        List<String> loreTemplates = settings.getClaimItemLore();
        boolean hasCustomLore = false;
        if (displayMetadata != null) {
            nameTemplate = displayMetadata.getNameTemplate();
            loreTemplates = displayMetadata.getLoreTemplates();
            hasCustomLore = displayMetadata.hasCustomLore();
        }
        nameTemplate = appendSizeSuffixIfNeeded(nameTemplate, loreTemplates, width, height);

        meta.setDisplayName(Texts.applyPlaceholders(nameTemplate, placeholders));
        if (settings.isClaimItemClearLore() && !hasCustomLore) {
            meta.setLore(null);
            return;
        }

        List<String> lore = new ArrayList<String>(loreTemplates);
        for (int i = 0; i < lore.size(); i++) {
            lore.set(i, Texts.applyPlaceholders(lore.get(i), placeholders));
        }
        meta.setLore(lore);
    }

    public static ResolvedSize resolveSize(
        Player player,
        ImageFile imageFile,
        AddonSettings settings,
        DisplayMetadataService.DisplayMetadata displayMetadata
    ) {
        Dimension size = imageFile == null ? null : imageFile.getSize();
        int width = getOverrideOrDefault(
            displayMetadata == null ? null : displayMetadata.getWidthOverride(),
            settings.getClaimItemWidth(),
            1,
            30
        );
        int height = getOverrideOrDefault(
            displayMetadata == null ? null : displayMetadata.getHeightOverride(),
            settings.getClaimItemHeight(),
            0,
            30
        );
        boolean autoHeight = displayMetadata != null && displayMetadata.getAutoHeightOverride() != null
            ? displayMetadata.getAutoHeightOverride()
            : settings.isClaimItemAutoHeight();
        if (autoHeight && height <= 0 && size != null && size.width > 0 && player != null) {
            height = FakeImage.getProportionalHeight(size, player, width);
        }
        if (height <= 0) {
            height = 1;
        }
        return new ResolvedSize(width, height);
    }

    public static String appendSizeSuffixIfNeeded(String nameTemplate, List<String> loreTemplates, int width, int height) {
        if (usesDimensionPlaceholder(nameTemplate, loreTemplates)) {
            return nameTemplate == null ? "" : nameTemplate;
        }

        String suffix = "&7(" + width + "x" + height + ")";
        if (nameTemplate == null || nameTemplate.trim().isEmpty()) {
            return suffix;
        }
        return nameTemplate + " " + suffix;
    }

    public static boolean usesDimensionPlaceholder(String nameTemplate, List<String> loreTemplates) {
        if (containsDimensionPlaceholder(nameTemplate)) {
            return true;
        }
        if (loreTemplates == null) {
            return false;
        }
        for (int i = 0; i < loreTemplates.size(); i++) {
            if (containsDimensionPlaceholder(loreTemplates.get(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsDimensionPlaceholder(String value) {
        return value != null && (value.contains("{width}") || value.contains("{height}"));
    }

    private static int getOverrideOrDefault(Integer overrideValue, int defaultValue, int min, int max) {
        int value = overrideValue == null ? defaultValue : overrideValue;
        return Math.max(min, Math.min(max, value));
    }

    public static final class ImageData {
        private final String filename;
        private final int width;
        private final int height;
        private final int flags;

        public ImageData(String filename, int width, int height, int flags) {
            this.filename = filename;
            this.width = width;
            this.height = height;
            this.flags = flags;
        }

        public String getFilename() {
            return filename;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public int getFlags() {
            return flags;
        }
    }

    public static final class ResolvedSize {
        private final int width;
        private final int height;

        public ResolvedSize(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }
}
