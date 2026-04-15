package io.josemmo.bukkit.plugin.addon.imgui.security;

import io.josemmo.bukkit.plugin.YamipaPlugin;
import io.josemmo.bukkit.plugin.addon.imgui.config.AddonSettings;
import io.josemmo.bukkit.plugin.addon.imgui.display.DisplayMetadataService;
import io.josemmo.bukkit.plugin.addon.imgui.gui.GuiService;
import io.josemmo.bukkit.plugin.addon.imgui.i18n.LocaleService;
import io.josemmo.bukkit.plugin.addon.imgui.util.ImageItemFactory;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import java.util.Collections;
import java.util.List;

public class DroppedImageItemMetadataListener implements Listener {
    private final GuiService guiService;
    private final LocaleService localeService;
    private final DisplayMetadataService displayMetadataService;
    private final NamespacedKey filenameKey;
    private final NamespacedKey widthKey;
    private final NamespacedKey heightKey;

    public DroppedImageItemMetadataListener(
        GuiService guiService,
        LocaleService localeService,
        DisplayMetadataService displayMetadataService
    ) {
        this.guiService = guiService;
        this.localeService = localeService;
        this.displayMetadataService = displayMetadataService;
        YamipaPlugin corePlugin = YamipaPlugin.getInstance();
        this.filenameKey = new NamespacedKey(corePlugin, "filename");
        this.widthKey = new NamespacedKey(corePlugin, "width");
        this.heightKey = new NamespacedKey(corePlugin, "height");
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        AddonSettings settings = guiService.getSettings();
        if (settings == null) {
            return;
        }

        ItemStack itemStack = event.getItem().getItemStack();
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null || !isLegacyYamipaLore(itemMeta.getLore())) {
            return;
        }

        PersistentDataContainer data = itemMeta.getPersistentDataContainer();
        String filename = data.get(filenameKey, PersistentDataType.STRING);
        Integer width = data.get(widthKey, PersistentDataType.INTEGER);
        Integer height = data.get(heightKey, PersistentDataType.INTEGER);
        if (filename == null || width == null || height == null) {
            return;
        }

        Player player = (Player) event.getEntity();
        String itemNameTemplate = localeService.trRaw(
            player,
            "item.name-format",
            settings.getClaimItemNameFormat(),
            Collections.<String, String>emptyMap()
        );
        List<String> itemLoreTemplate = localeService.trRawList(
            player,
            "item.lore",
            settings.getClaimItemLore(),
            Collections.<String, String>emptyMap()
        );
        DisplayMetadataService.DisplayMetadata displayMetadata = displayMetadataService.resolve(
            player,
            settings,
            filename,
            itemNameTemplate,
            itemLoreTemplate
        );

        ImageItemFactory.applyDisplayMetadata(
            itemMeta,
            filename,
            itemStack.getAmount(),
            width,
            height,
            settings,
            displayMetadata
        );
        itemStack.setItemMeta(itemMeta);
        event.getItem().setItemStack(itemStack);
    }

    private boolean isLegacyYamipaLore(List<String> lore) {
        return lore != null && lore.size() == 1 && "Yamipa image".equals(lore.get(0));
    }
}
