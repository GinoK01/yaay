package io.josemmo.bukkit.plugin.addon.imgui.editor;

import io.josemmo.bukkit.plugin.YamipaPlugin;
import io.josemmo.bukkit.plugin.addon.imgui.config.AddonSettings;
import io.josemmo.bukkit.plugin.addon.imgui.display.DisplayMetadataService;
import io.josemmo.bukkit.plugin.addon.imgui.i18n.LocaleService;
import io.josemmo.bukkit.plugin.addon.imgui.util.ImageItemFactory;
import io.josemmo.bukkit.plugin.storage.ImageFile;
import io.josemmo.bukkit.plugin.renderer.FakeImage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.Collections;
import java.util.List;

public final class ImageItemEditor {
    private final YamipaPlugin corePlugin;
    private final LocaleService localeService;
    private final DisplayMetadataService displayMetadataService;

    public ImageItemEditor(
        YamipaPlugin corePlugin,
        LocaleService localeService,
        DisplayMetadataService displayMetadataService
    ) {
        this.corePlugin = corePlugin;
        this.localeService = localeService;
        this.displayMetadataService = displayMetadataService;
    }

    public ImageItemFactory.ImageData read(ItemStack item) {
        return ImageItemFactory.readImageData(item);
    }

    public ItemStack resize(Player player, ItemStack source, int width, int height, AddonSettings settings) {
        ImageItemFactory.ImageData data = read(source);
        if (data == null) {
            throw new IllegalArgumentException("Item is not a Yamipa image");
        }
        if (player == null) {
            throw new IllegalArgumentException("Player is required");
        }
        if (settings == null) {
            throw new IllegalArgumentException("Settings are required");
        }
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("Image size must be at least 1");
        }
        int yamipaMax = FakeImage.getMaxImageDimension(player);
        int maxWidth = Math.max(1, Math.min(settings.getEditorMaxWidth(), yamipaMax));
        int maxHeight = Math.max(1, Math.min(settings.getEditorMaxHeight(), yamipaMax));
        int minWidth = Math.min(Math.max(1, settings.getEditorMinWidth()), maxWidth);
        int minHeight = Math.min(Math.max(1, settings.getEditorMinHeight()), maxHeight);
        if (width < minWidth || width > maxWidth || height < minHeight || height > maxHeight) {
            throw new IllegalArgumentException("Image size is outside the allowed range");
        }

        ImageFile imageFile = corePlugin.getStorage().get(data.getFilename());
        if (imageFile == null) {
            throw new IllegalStateException("Image file no longer exists: " + data.getFilename());
        }

        ItemStack result = new ItemStack(source.getType(), Math.max(1, source.getAmount()));
        ItemMeta meta = result.getItemMeta();
        if (meta == null) {
            throw new IllegalStateException("Could not create item meta for " + data.getFilename());
        }

        String nameTemplate = localeService.trRaw(
            player,
            "item.name-format",
            settings.getClaimItemNameFormat(),
            Collections.<String, String>emptyMap()
        );
        List<String> loreTemplate = localeService.trRawList(
            player,
            "item.lore",
            settings.getClaimItemLore(),
            Collections.<String, String>emptyMap()
        );
        DisplayMetadataService.DisplayMetadata displayMetadata = displayMetadataService.resolve(
            player,
            settings,
            data.getFilename(),
            nameTemplate,
            loreTemplate
        );
        ImageItemFactory.applyDisplayMetadata(
            meta,
            data.getFilename(),
            result.getAmount(),
            width,
            height,
            settings,
            displayMetadata
        );
        ImageItemFactory.writeImageData(meta, data.getFilename(), width, height, data.getFlags());
        result.setItemMeta(meta);
        return result;
    }
}
