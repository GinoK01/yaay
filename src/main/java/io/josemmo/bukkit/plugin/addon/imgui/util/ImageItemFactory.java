package io.josemmo.bukkit.plugin.addon.imgui.util;

import io.josemmo.bukkit.plugin.YamipaPlugin;
import io.josemmo.bukkit.plugin.addon.imgui.config.AddonSettings;
import io.josemmo.bukkit.plugin.addon.imgui.display.DisplayMetadataService;
import io.josemmo.bukkit.plugin.renderer.FakeImage;
import io.josemmo.bukkit.plugin.storage.ImageFile;
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
        Dimension size = imageFile.getSize();
        if (size == null) {
            throw new IllegalArgumentException("Invalid image file");
        }

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
        if (autoHeight && height <= 0) {
            height = FakeImage.getProportionalHeight(size, player, width);
        }
        if (height <= 0) {
            height = 1;
        }

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

        PersistentDataContainer data = meta.getPersistentDataContainer();
        NamespacedKey filenameKey = new NamespacedKey(YamipaPlugin.getInstance(), "filename");
        NamespacedKey widthKey = new NamespacedKey(YamipaPlugin.getInstance(), "width");
        NamespacedKey heightKey = new NamespacedKey(YamipaPlugin.getInstance(), "height");
        NamespacedKey flagsKey = new NamespacedKey(YamipaPlugin.getInstance(), "flags");
        data.set(filenameKey, PersistentDataType.STRING, imageFile.getFilename());
        data.set(widthKey, PersistentDataType.INTEGER, width);
        data.set(heightKey, PersistentDataType.INTEGER, height);
        data.set(flagsKey, PersistentDataType.INTEGER, flags);

        item.setItemMeta(meta);
        return item;
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

    private static int getOverrideOrDefault(Integer overrideValue, int defaultValue, int min, int max) {
        int value = overrideValue == null ? defaultValue : overrideValue;
        return Math.max(min, Math.min(max, value));
    }
}
