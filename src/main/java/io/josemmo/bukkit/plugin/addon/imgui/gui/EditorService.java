package io.josemmo.bukkit.plugin.addon.imgui.gui;

import io.josemmo.bukkit.plugin.YamipaPlugin;
import io.josemmo.bukkit.plugin.addon.imgui.ImguiAddonPlugin;
import io.josemmo.bukkit.plugin.addon.imgui.config.AddonSettings;
import io.josemmo.bukkit.plugin.addon.imgui.display.DisplayMetadataService;
import io.josemmo.bukkit.plugin.addon.imgui.editor.DeliveryMode;
import io.josemmo.bukkit.plugin.addon.imgui.editor.EditorFinishPlan;
import io.josemmo.bukkit.plugin.addon.imgui.editor.EditorSession;
import io.josemmo.bukkit.plugin.addon.imgui.editor.EditorSessionStore;
import io.josemmo.bukkit.plugin.addon.imgui.editor.EditorState;
import io.josemmo.bukkit.plugin.addon.imgui.editor.ImageItemEditor;
import io.josemmo.bukkit.plugin.addon.imgui.editor.ItemDelivery;
import io.josemmo.bukkit.plugin.addon.imgui.i18n.LocaleService;
import io.josemmo.bukkit.plugin.addon.imgui.util.ImageItemFactory;
import io.josemmo.bukkit.plugin.addon.imgui.util.InventoryUtil;
import io.josemmo.bukkit.plugin.addon.imgui.util.Texts;
import io.josemmo.bukkit.plugin.renderer.FakeImage;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EditorService implements Listener {
    static final int GUI_SIZE = 45;
    static final int SLOT_INPUT = 13;
    static final int SLOT_INFO = 16;
    static final int SLOT_WIDTH_MINUS = 19;
    static final int SLOT_WIDTH_PLUS = 21;
    static final int SLOT_HEIGHT_MINUS = 23;
    static final int SLOT_HEIGHT_PLUS = 25;
    static final int SLOT_CANCEL = 38;
    static final int SLOT_APPLY = 42;

    private final ImguiAddonPlugin plugin;
    private final LocaleService localeService;
    private final EditorSessionStore store;
    private final ImageItemEditor imageItemEditor;
    private final Map<UUID, EditorSession> sessions = new ConcurrentHashMap<UUID, EditorSession>();
    private final Set<UUID> ignoreClose = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());
    private volatile AddonSettings settings;

    public EditorService(
        ImguiAddonPlugin plugin,
        YamipaPlugin corePlugin,
        LocaleService localeService,
        DisplayMetadataService displayMetadataService,
        EditorSessionStore store
    ) {
        this.plugin = plugin;
        this.localeService = localeService;
        this.store = store;
        this.imageItemEditor = new ImageItemEditor(corePlugin, localeService, displayMetadataService);
    }

    public void applySettings(AddonSettings newSettings) {
        this.settings = newSettings;
        if (newSettings != null && newSettings.isCloseMenusOnReload()) {
            closeOpenEditors(EditorFinishPlan.cancel(true));
            return;
        }
        refreshOpenEditors();
    }

    public boolean hasHeldItem(UUID playerId) {
        EditorSession session = sessions.get(playerId);
        return session != null && session.hasItem();
    }

    public void openEditor(Player player) {
        if (player == null || settings == null) {
            return;
        }
        EditorMenuHolder holder = new EditorMenuHolder(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, GUI_SIZE, title(player));
        holder.setInventory(inventory);
        render(player, inventory);
        ignoreClose.add(player.getUniqueId());
        try {
            player.openInventory(inventory);
        } finally {
            ignoreClose.remove(player.getUniqueId());
        }
    }

    public void shutdown() {
        Set<UUID> pending = new HashSet<UUID>(sessions.keySet());
        for (UUID playerId : pending) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                continue;
            }
            ItemStack preview = preview(playerId);
            boolean hasSpace = preview == null || InventoryUtil.hasSpaceFor(player.getInventory(), preview);
            ignoreClose.add(playerId);
            try {
                finishOpen(player, EditorFinishPlan.recover(hasSpace), null, false);
                closeIfOpen(player);
            } finally {
                ignoreClose.remove(playerId);
            }
        }
    }

    public void deliverPending(Player player) {
        if (player == null || hasHeldItem(player.getUniqueId())) {
            return;
        }
        ItemStack stored = store.peek(player.getUniqueId());
        if (stored == null) {
            return;
        }
        boolean storedInInventory = ItemDelivery.giveOrDrop(player, stored);
        if (!storedInInventory) {
            send(player, "editor-dropped", "Inventory is full. The item was dropped.");
        }
        store.delete(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof EditorMenuHolder)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        if (!((EditorMenuHolder) top.getHolder()).getPlayerId().equals(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        int rawSlot = event.getRawSlot();
        boolean topSlot = rawSlot >= 0 && rawSlot < top.getSize();
        if (event.getClick() == ClickType.DOUBLE_CLICK
            || event.getClick() == ClickType.CREATIVE
            || event.getClick() == ClickType.SWAP_OFFHAND
            || event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            return;
        }
        if (!topSlot) {
            if (event.isShiftClick()) {
                event.setCancelled(true);
                insertFromBottom(player, event.getView(), event.getClickedInventory(), event.getSlot());
            }
            return;
        }

        event.setCancelled(true);
        if (event.getClick() == ClickType.NUMBER_KEY
            || event.getClick() == ClickType.DROP
            || event.getClick() == ClickType.CONTROL_DROP) {
            return;
        }
        if (rawSlot == SLOT_INPUT) {
            if (event.isShiftClick()) {
                returnHeld(player, event.getView(), false);
            } else if (event.getClick().isLeftClick() || event.getClick().isRightClick()) {
                handleInputClick(player, event.getView());
            }
            return;
        }
        if (!event.getClick().isLeftClick() && !event.getClick().isRightClick()) {
            return;
        }
        if (rawSlot == SLOT_WIDTH_MINUS) {
            changeSize(player, true, -1);
        } else if (rawSlot == SLOT_WIDTH_PLUS) {
            changeSize(player, true, 1);
        } else if (rawSlot == SLOT_HEIGHT_MINUS) {
            changeSize(player, false, -1);
        } else if (rawSlot == SLOT_HEIGHT_PLUS) {
            changeSize(player, false, 1);
        } else if (rawSlot == SLOT_CANCEL) {
            cancel(player, true);
        } else if (rawSlot == SLOT_APPLY) {
            apply(player);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof EditorMenuHolder)) {
            return;
        }
        int topSize = top.getSize();
        for (Integer rawSlot : event.getRawSlots()) {
            if (rawSlot != null && rawSlot >= 0 && rawSlot < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCreative(InventoryCreativeEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top.getHolder() instanceof EditorMenuHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof EditorMenuHolder)) {
            return;
        }
        Player player = (Player) event.getPlayer();
        if (!((EditorMenuHolder) top.getHolder()).getPlayerId().equals(player.getUniqueId())) {
            return;
        }
        if (ignoreClose.contains(player.getUniqueId())) {
            return;
        }
        cancel(player, false);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onQuit(PlayerQuitEvent event) {
        recoverDisconnect(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onKick(PlayerKickEvent event) {
        recoverDisconnect(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID playerId = player.getUniqueId();
        if (!hasHeldItem(playerId)) {
            return;
        }
        ignoreClose.add(playerId);
        try {
            clearInput(player);
            closeIfOpen(player);
            ItemStack original = take(playerId, EditorState.RECOVERED);
            if (original == null) {
                return;
            }
            Boolean keepInventory = player.getWorld().getGameRuleValue(GameRule.KEEP_INVENTORY);
            if (keepInventory != null && keepInventory.booleanValue()) {
                deliver(player, original, DeliveryMode.forSpace(InventoryUtil.hasSpaceFor(player.getInventory(), original)));
            } else {
                event.getDrops().add(original);
            }
            store.delete(playerId);
        } finally {
            ignoreClose.remove(playerId);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        deliverPending(event.getPlayer());
    }

    private void recoverDisconnect(Player player) {
        if (player == null || !hasHeldItem(player.getUniqueId())) {
            return;
        }
        ItemStack preview = preview(player.getUniqueId());
        boolean hasSpace = preview != null && InventoryUtil.hasSpaceFor(player.getInventory(), preview);
        finishOpen(player, EditorFinishPlan.recover(hasSpace), null, false);
    }

    private void cancel(Player player, boolean closeAfter) {
        ItemStack preview = preview(player.getUniqueId());
        boolean hasSpace = preview == null || InventoryUtil.hasSpaceFor(player.getInventory(), preview);
        finishOpen(player, EditorFinishPlan.cancel(hasSpace), null, closeAfter);
    }

    private void apply(Player player) {
        EditorSession session = sessions.get(player.getUniqueId());
        if (session == null || !session.hasItem()) {
            send(player, "editor-no-image", "Place an image in the editor first.");
            return;
        }
        AddonSettings localSettings = settings;
        if (localSettings == null) {
            return;
        }
        session.clampDraft(minWidth(player), maxWidth(player), minHeight(player), maxHeight(player));
        ItemStack source = session.view();
        int width = session.width();
        int height = session.height();
        ItemStack result;
        try {
            result = imageItemEditor.resize(player, source, width, height, localSettings);
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("Failed to resize image for " + player.getName() + ": " + ex.getMessage());
            boolean hasSpace = source != null && InventoryUtil.hasSpaceFor(player.getInventory(), source);
            finishOpen(player, EditorFinishPlan.confirm(false, hasSpace), null, true);
            send(player, "editor-apply-failed", "Could not update the image. The original item was returned.");
            return;
        }
        boolean hasSpace = InventoryUtil.hasSpaceFor(player.getInventory(), result);
        if (!finishOpen(player, EditorFinishPlan.confirm(true, hasSpace), result, true)) {
            return;
        }
        Map<String, String> placeholders = new HashMap<String, String>();
        placeholders.put("width", String.valueOf(width));
        placeholders.put("height", String.valueOf(height));
        send(player, "editor-applied", "Image updated: {width}x{height}", placeholders);
    }

    private boolean finishOpen(Player player, EditorFinishPlan plan, ItemStack replacement, boolean closeAfter) {
        ItemStack original = take(player.getUniqueId(), plan.state());
        if (plan.state() == EditorState.CONFIRMED && original == null) {
            return false;
        }
        clearInput(player);
        ItemStack toGive = plan.action() == EditorFinishPlan.Action.DELIVER_RESULT
            ? (replacement != null ? replacement : original)
            : original;
        if (toGive != null) {
            deliver(player, toGive, plan.delivery());
            store.delete(player.getUniqueId());
            if (plan.state() == EditorState.CANCELLED) {
                send(player, "editor-cancelled", "Edit cancelled. Item returned.");
            }
        }
        if (closeAfter) {
            closeIfOpen(player);
        }
        return plan.state() != EditorState.CONFIRMED || toGive != null;
    }

    private ItemStack take(UUID playerId, EditorState target) {
        EditorSession session = sessions.get(playerId);
        if (session == null) {
            return null;
        }
        ItemStack item = session.finish(target);
        if (item != null || session.state().isTerminal()) {
            sessions.remove(playerId, session);
        }
        return item;
    }

    private ItemStack preview(UUID playerId) {
        EditorSession session = sessions.get(playerId);
        return session == null ? null : session.view();
    }

    private void deliver(Player player, ItemStack item, DeliveryMode mode) {
        if (item == null || player == null) {
            return;
        }
        if (mode == DeliveryMode.DROP) {
            ItemDelivery.drop(player, item);
            send(player, "editor-dropped", "Inventory is full. The item was dropped.");
            return;
        }
        if (!ItemDelivery.giveOrDrop(player, item)) {
            send(player, "editor-dropped", "Inventory is full. The item was dropped.");
        }
    }

    private void handleInputClick(Player player, InventoryView view) {
        ItemStack cursor = view.getCursor();
        if (isAir(cursor)) {
            returnHeld(player, view, true);
            return;
        }
        ImageItemFactory.ImageData data = imageItemEditor.read(cursor);
        if (data == null) {
            send(player, "editor-invalid-item", "That item is not a Yamipa image.");
            return;
        }
        EditorSession session = editingSession(player.getUniqueId());
        if (!acceptItem(player, session, cursor.clone(), data, view, false)) {
            return;
        }
        view.setCursor(new ItemStack(Material.AIR));
        rerender(player);
    }

    private void insertFromBottom(Player player, InventoryView view, Inventory inventory, int slot) {
        if (inventory == null || slot < 0) {
            return;
        }
        ItemStack clicked = inventory.getItem(slot);
        if (isAir(clicked)) {
            return;
        }
        ImageItemFactory.ImageData data = imageItemEditor.read(clicked);
        if (data == null) {
            send(player, "editor-invalid-item", "That item is not a Yamipa image.");
            return;
        }
        EditorSession session = editingSession(player.getUniqueId());
        if (!acceptItem(player, session, clicked.clone(), data, view, true)) {
            return;
        }
        inventory.setItem(slot, null);
        rerender(player);
    }

    private boolean acceptItem(
        Player player,
        EditorSession session,
        ItemStack incoming,
        ImageItemFactory.ImageData data,
        InventoryView view,
        boolean preferCursorForPrevious
    ) {
        ItemStack previous = session.view();
        if (previous != null) {
            if (preferCursorForPrevious && isAir(view.getCursor())) {
                view.setCursor(previous);
            } else {
                deliver(player, previous, DeliveryMode.forSpace(InventoryUtil.hasSpaceFor(player.getInventory(), previous)));
            }
            if (session.release() == null) {
                return false;
            }
            store.delete(player.getUniqueId());
        }
        if (!store.save(player.getUniqueId(), incoming)) {
            return false;
        }
        int width = clamp(data.getWidth(), minWidth(player), maxWidth(player));
        int height = clamp(Math.max(1, data.getHeight()), minHeight(player), maxHeight(player));
        if (!session.hold(incoming, data.getFilename(), data.getWidth(), data.getHeight(), width, height)) {
            store.delete(player.getUniqueId());
            return false;
        }
        sessions.put(player.getUniqueId(), session);
        return true;
    }

    private void returnHeld(Player player, InventoryView view, boolean toCursor) {
        EditorSession session = sessions.get(player.getUniqueId());
        if (session == null || !session.hasItem()) {
            return;
        }
        ItemStack item = session.view();
        if (item == null) {
            return;
        }
        if (toCursor) {
            if (!isAir(view.getCursor())) {
                return;
            }
            view.setCursor(item);
        } else {
            deliver(player, item, DeliveryMode.forSpace(InventoryUtil.hasSpaceFor(player.getInventory(), item)));
        }
        if (session.release() == null) {
            if (toCursor) {
                view.setCursor(new ItemStack(Material.AIR));
            }
            return;
        }
        store.delete(player.getUniqueId());
        rerender(player);
    }

    private void changeSize(Player player, boolean width, int delta) {
        EditorSession session = sessions.get(player.getUniqueId());
        if (session == null || !session.hasItem()) {
            send(player, "editor-no-image", "Place an image in the editor first.");
            return;
        }
        boolean changed = width
            ? session.adjustWidth(delta, minWidth(player), maxWidth(player))
            : session.adjustHeight(delta, minHeight(player), maxHeight(player));
        if (changed) {
            rerender(player);
        }
    }

    private EditorSession editingSession(UUID playerId) {
        EditorSession current = sessions.get(playerId);
        if (current != null && current.state() == EditorState.EDITING) {
            return current;
        }
        EditorSession created = new EditorSession();
        sessions.put(playerId, created);
        return created;
    }

    private void closeOpenEditors(EditorFinishPlan plan) {
        Set<UUID> pending = new HashSet<UUID>(sessions.keySet());
        for (Player player : Bukkit.getOnlinePlayers()) {
            Inventory top = player.getOpenInventory().getTopInventory();
            if (!(top.getHolder() instanceof EditorMenuHolder)) {
                continue;
            }
            pending.add(player.getUniqueId());
        }
        for (UUID playerId : pending) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                continue;
            }
            ItemStack current = preview(playerId);
            boolean hasSpace = current == null || InventoryUtil.hasSpaceFor(player.getInventory(), current);
            EditorFinishPlan resolved = plan.state() == EditorState.CANCELLED
                ? EditorFinishPlan.cancel(hasSpace)
                : EditorFinishPlan.recover(hasSpace);
            ignoreClose.add(playerId);
            try {
                finishOpen(player, resolved, null, false);
                closeIfOpen(player);
            } finally {
                ignoreClose.remove(playerId);
            }
        }
    }

    private void refreshOpenEditors() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Inventory top = player.getOpenInventory().getTopInventory();
            if (top.getHolder() instanceof EditorMenuHolder) {
                render(player, top);
            }
        }
    }

    private void closeIfOpen(Player player) {
        Inventory top = player.getOpenInventory().getTopInventory();
        if (!(top.getHolder() instanceof EditorMenuHolder)) {
            return;
        }
        if (ignoreClose.contains(player.getUniqueId())) {
            ignoreClose.add(player.getUniqueId());
            player.closeInventory();
            return;
        }
        ignoreClose.add(player.getUniqueId());
        try {
            player.closeInventory();
        } finally {
            ignoreClose.remove(player.getUniqueId());
        }
    }

    private void clearInput(Player player) {
        Inventory top = player.getOpenInventory().getTopInventory();
        if (top.getHolder() instanceof EditorMenuHolder && top.getSize() > SLOT_INPUT) {
            top.setItem(SLOT_INPUT, null);
        }
    }

    private void rerender(Player player) {
        Inventory top = player.getOpenInventory().getTopInventory();
        if (!(top.getHolder() instanceof EditorMenuHolder)) {
            return;
        }
        render(player, top);
    }

    private void render(Player player, Inventory inventory) {
        AddonSettings localSettings = settings;
        if (localSettings == null) {
            return;
        }
        ItemStack filler = icon(localSettings.getFillerMaterial(), " ", Collections.<String>emptyList(), Collections.<String, String>emptyMap());
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (slot == SLOT_INPUT) {
                inventory.setItem(slot, null);
                continue;
            }
            inventory.setItem(slot, filler);
        }

        EditorSession session = sessions.get(player.getUniqueId());
        if (session != null) {
            session.clampDraft(minWidth(player), maxWidth(player), minHeight(player), maxHeight(player));
        }
        Map<String, String> placeholders = sizePlaceholders(session);
        if (session != null && session.hasItem()) {
            inventory.setItem(SLOT_INFO, button(
                player,
                Material.BOOK,
                "gui.editor.info.name",
                "&fImage",
                "gui.editor.info.lore",
                Arrays.asList("&7File: &f{filename}", "&7Current: &f{current_width}x{current_height}", "&7New: &f{width}x{height}"),
                placeholders
            ));
            inventory.setItem(SLOT_INPUT, session.view());
        } else {
            inventory.setItem(SLOT_INFO, button(
                player,
                Material.BOOK,
                "gui.editor.info-empty.name",
                "&fImage",
                "gui.editor.info-empty.lore",
                Arrays.asList("&7Place an image item here", "&8It must be a Yamipa image"),
                placeholders
            ));
        }

        inventory.setItem(SLOT_WIDTH_MINUS, button(player, Material.RED_DYE, "gui.editor.width-decrease.name", "&c- Width", "gui.editor.width-decrease.lore", Collections.singletonList("&7Width: &f{width}"), placeholders));
        inventory.setItem(SLOT_WIDTH_PLUS, button(player, Material.LIME_DYE, "gui.editor.width-increase.name", "&a+ Width", "gui.editor.width-increase.lore", Collections.singletonList("&7Width: &f{width}"), placeholders));
        inventory.setItem(SLOT_HEIGHT_MINUS, button(player, Material.RED_DYE, "gui.editor.height-decrease.name", "&c- Height", "gui.editor.height-decrease.lore", Collections.singletonList("&7Height: &f{height}"), placeholders));
        inventory.setItem(SLOT_HEIGHT_PLUS, button(player, Material.LIME_DYE, "gui.editor.height-increase.name", "&a+ Height", "gui.editor.height-increase.lore", Collections.singletonList("&7Height: &f{height}"), placeholders));
        inventory.setItem(SLOT_CANCEL, button(player, Material.BARRIER, "gui.editor.cancel.name", "&cCancel", "gui.editor.cancel.lore", Collections.singletonList("&7Return the original item"), placeholders));
        inventory.setItem(SLOT_APPLY, button(player, Material.EMERALD, "gui.editor.apply.name", "&aApply", "gui.editor.apply.lore", Collections.singletonList("&7Confirm {width}x{height}"), placeholders));
    }

    private String title(Player player) {
        return localeService.trRaw(player, "gui.editor.title", "&0Image editor", Collections.<String, String>emptyMap());
    }

    private Map<String, String> sizePlaceholders(EditorSession session) {
        Map<String, String> placeholders = new HashMap<String, String>();
        if (session == null || !session.hasItem()) {
            placeholders.put("filename", "-");
            placeholders.put("current_width", "-");
            placeholders.put("current_height", "-");
            placeholders.put("width", "-");
            placeholders.put("height", "-");
            return placeholders;
        }
        placeholders.put("filename", Texts.getBasename(session.filename()));
        placeholders.put("current_width", String.valueOf(session.originalWidth()));
        placeholders.put("current_height", String.valueOf(session.originalHeight()));
        placeholders.put("width", String.valueOf(session.width()));
        placeholders.put("height", String.valueOf(session.height()));
        return placeholders;
    }

    private ItemStack button(
        Player player,
        Material material,
        String nameKey,
        String nameDefault,
        String loreKey,
        List<String> loreDefault,
        Map<String, String> placeholders
    ) {
        String name = localeService.trRaw(player, nameKey, nameDefault, placeholders);
        List<String> lore = localeService.trRawList(player, loreKey, loreDefault, placeholders);
        return icon(material, name, lore, placeholders);
    }

    private ItemStack icon(Material material, String name, List<String> lore, Map<String, String> placeholders) {
        ItemStack item = new ItemStack(material == null ? Material.PAPER : material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(Texts.applyPlaceholders(name, placeholders));
        ArrayList<String> lines = new ArrayList<String>();
        if (lore != null) {
            for (int i = 0; i < lore.size(); i++) {
                lines.add(Texts.applyPlaceholders(lore.get(i), placeholders));
            }
        }
        meta.setLore(lines);
        item.setItemMeta(meta);
        return item;
    }

    private int minWidth(Player player) {
        return Math.min(Math.max(1, settings.getEditorMinWidth()), maxWidth(player));
    }

    private int maxWidth(Player player) {
        return Math.max(1, Math.min(settings.getEditorMaxWidth(), FakeImage.getMaxImageDimension(player)));
    }

    private int minHeight(Player player) {
        return Math.min(Math.max(1, settings.getEditorMinHeight()), maxHeight(player));
    }

    private int maxHeight(Player player) {
        return Math.max(1, Math.min(settings.getEditorMaxHeight(), FakeImage.getMaxImageDimension(player)));
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private boolean isAir(ItemStack item) {
        return item == null || item.getType() == Material.AIR || item.getAmount() <= 0;
    }

    private void send(Player player, String key, String fallback) {
        send(player, key, fallback, Collections.<String, String>emptyMap());
    }

    private void send(Player player, String key, String fallback, Map<String, String> placeholders) {
        AddonSettings localSettings = settings;
        String prefix = localSettings == null ? "" : Texts.applyPlaceholders(localSettings.getPrefix(), placeholders);
        String body = localeService.trRaw(player, "messages." + key, fallback, placeholders);
        player.sendMessage(prefix + body);
    }
}
