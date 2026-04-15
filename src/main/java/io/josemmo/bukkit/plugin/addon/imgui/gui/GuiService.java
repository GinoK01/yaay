package io.josemmo.bukkit.plugin.addon.imgui.gui;

import io.josemmo.bukkit.plugin.YamipaPlugin;
import io.josemmo.bukkit.plugin.addon.imgui.ImguiAddonPlugin;
import io.josemmo.bukkit.plugin.addon.imgui.config.AddonSettings;
import io.josemmo.bukkit.plugin.addon.imgui.display.DisplayMetadataService;
import io.josemmo.bukkit.plugin.addon.imgui.i18n.LocaleService;
import io.josemmo.bukkit.plugin.addon.imgui.limits.HourlyLimitService;
import io.josemmo.bukkit.plugin.addon.imgui.util.ImageItemFactory;
import io.josemmo.bukkit.plugin.addon.imgui.util.InventoryUtil;
import io.josemmo.bukkit.plugin.addon.imgui.util.Texts;
import io.josemmo.bukkit.plugin.storage.ImageFile;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class GuiService {
    private final ImguiAddonPlugin plugin;
    private final YamipaPlugin corePlugin;
    private final LocaleService localeService;
    private final DisplayMetadataService displayMetadataService;
    private final HourlyLimitService hourlyLimitService;
    private final AtomicLong tokenCounter = new AtomicLong(0L);
    private final Map<UUID, GuiSession> sessions = new ConcurrentHashMap<UUID, GuiSession>();
    private final Map<UUID, CachedFilenames> filenameCache = new ConcurrentHashMap<UUID, CachedFilenames>();
    private final Map<UUID, Long> lastClaimAt = new ConcurrentHashMap<UUID, Long>();
    private final Map<UUID, Long> lastLanguageChangeAt = new ConcurrentHashMap<UUID, Long>();
    private final Set<UUID> claimLocks = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());
    private volatile AddonSettings settings;

    public GuiService(
        ImguiAddonPlugin plugin,
        YamipaPlugin corePlugin,
        LocaleService localeService,
        DisplayMetadataService displayMetadataService,
        HourlyLimitService hourlyLimitService
    ) {
        this.plugin = plugin;
        this.corePlugin = corePlugin;
        this.localeService = localeService;
        this.displayMetadataService = displayMetadataService;
        this.hourlyLimitService = hourlyLimitService;
    }

    public void applySettings(AddonSettings settings) {
        this.settings = settings;
        if (settings.isCloseMenusOnReload()) {
            closeAllMenus();
        } else {
            refreshOpenMenus();
        }
    }

    public AddonSettings getSettings() {
        return settings;
    }

    public void openMenu(Player player, int requestedPage) {
        AddonSettings localSettings = settings;
        if (localSettings == null || !localSettings.isEnabled()) {
            return;
        }

        List<String> filenames = getVisibleFilenames(player);
        if (filenames.isEmpty()) {
            player.sendMessage(localeService.tr(player, "menu-empty"));
        }
        openWithData(player, filenames, requestedPage);
    }

    public void handleTopClick(Player player, int rawSlot, ClickType clickType) {
        GuiSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }

        if (!clickType.isLeftClick() && !clickType.isRightClick()) {
            return;
        }

        AddonSettings localSettings = settings;
        if (localSettings == null) {
            return;
        }

        if (rawSlot == localSettings.getPreviousSlot()) {
            if (session.getPage() > 1) {
                session.setPage(session.getPage() - 1);
                rerenderOpenMenu(player, session);
            }
            return;
        }

        if (rawSlot == localSettings.getNextSlot()) {
            int maxPages = session.getMaxPages(localSettings.getContentSlots().size());
            if (session.getPage() < maxPages) {
                session.setPage(session.getPage() + 1);
                rerenderOpenMenu(player, session);
            }
            return;
        }

        if (rawSlot == localSettings.getRefreshSlot()) {
            refreshMenu(player, session);
            return;
        }

        if (localSettings.isLanguageMenuEnabled() && rawSlot == localSettings.getLanguageSlot()) {
            cyclePlayerLanguage(player, session);
            return;
        }

        if (rawSlot == localSettings.getCloseSlot()) {
            player.closeInventory();
            return;
        }

        String filename = session.getSlotToFilename().get(rawSlot);
        if (filename == null) {
            return;
        }

        claimImage(player, filename);
    }

    public boolean isMenuInventory(Inventory inventory) {
        return inventory != null && inventory.getHolder() instanceof ImguiMenuHolder;
    }

    public boolean belongsToPlayer(Inventory inventory, UUID playerId) {
        if (!isMenuInventory(inventory)) {
            return false;
        }
        ImguiMenuHolder holder = (ImguiMenuHolder) inventory.getHolder();
        return holder.getPlayerId().equals(playerId);
    }

    public void onInventoryClosed(Player player, Inventory closedInventory) {
        if (player == null || closedInventory == null || !(closedInventory.getHolder() instanceof ImguiMenuHolder)) {
            return;
        }

        UUID playerId = player.getUniqueId();
        ImguiMenuHolder closedHolder = (ImguiMenuHolder) closedInventory.getHolder();
        if (!closedHolder.getPlayerId().equals(playerId)) {
            return;
        }

        GuiSession session = sessions.get(playerId);
        if (session != null && session.getToken() == closedHolder.getToken()) {
            sessions.remove(playerId);
            claimLocks.remove(playerId);
        }
    }

    public void onPlayerDisconnected(Player player) {
        if (player == null) {
            return;
        }

        UUID playerId = player.getUniqueId();
        sessions.remove(playerId);
        claimLocks.remove(playerId);
        lastClaimAt.remove(playerId);
        lastLanguageChangeAt.remove(playerId);
    }

    public void clearCache(UUID playerId) {
        filenameCache.remove(playerId);
    }

    public void closeAllMenus() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Inventory top = player.getOpenInventory().getTopInventory();
            if (isMenuInventory(top) && belongsToPlayer(top, player.getUniqueId())) {
                player.closeInventory();
            }
        }
        sessions.clear();
        claimLocks.clear();
        lastLanguageChangeAt.clear();
    }

    private void refreshOpenMenus() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Inventory top = player.getOpenInventory().getTopInventory();
            if (!isMenuInventory(top) || !belongsToPlayer(top, player.getUniqueId())) {
                continue;
            }
            GuiSession session = sessions.get(player.getUniqueId());
            if (session == null) {
                continue;
            }
            refreshMenu(player, session);
        }
    }

    private void refreshMenu(Player player, GuiSession session) {
        clearCache(player.getUniqueId());
        List<String> filenames = getVisibleFilenames(player);
        session.setFilenames(filenames);
        int maxPages = session.getMaxPages(settings.getContentSlots().size());
        if (session.getPage() > maxPages) {
            session.setPage(maxPages);
        }
        rerenderOpenMenu(player, session);
    }

    private void openWithData(Player player, List<String> filenames, int requestedPage) {
        AddonSettings localSettings = settings;
        if (localSettings == null) {
            return;
        }

        int itemsPerPage = Math.max(1, localSettings.getContentSlots().size());
        int maxPages = Math.max(1, (int) Math.ceil((double) Math.max(1, filenames.size()) / (double) itemsPerPage));
        int page = Math.max(1, Math.min(requestedPage, maxPages));

        long token = tokenCounter.incrementAndGet();
        GuiSession session = new GuiSession(player.getUniqueId(), token, filenames, page);
        sessions.put(player.getUniqueId(), session);

        ImguiMenuHolder holder = new ImguiMenuHolder(player.getUniqueId(), token);
        Map<String, String> placeholders = createPagePlaceholders(session, filenames.size());
        String titleTemplate = localeService.trRaw(
            player,
            "gui.title",
            localSettings.getMenuTitle(),
            Collections.<String, String>emptyMap()
        );
        String title = Texts.applyPlaceholders(titleTemplate, placeholders);
        Inventory inventory = Bukkit.createInventory(holder, localSettings.getMenuRows() * 9, title);
        holder.setInventory(inventory);
        renderInventory(player, inventory, session);
        player.openInventory(inventory);
    }

    private void rerenderOpenMenu(Player player, GuiSession session) {
        Inventory top = player.getOpenInventory().getTopInventory();
        if (!(top.getHolder() instanceof ImguiMenuHolder)) {
            return;
        }

        ImguiMenuHolder holder = (ImguiMenuHolder) top.getHolder();
        if (holder.getToken() != session.getToken()) {
            return;
        }

        renderInventory(player, top, session);
        player.updateInventory();
    }

    private void renderInventory(Player player, Inventory inventory, GuiSession session) {
        AddonSettings localSettings = settings;
        if (localSettings == null) {
            return;
        }

        inventory.clear();

        if (localSettings.isFillerEnabled()) {
            ItemStack filler = createIcon(
                localSettings.getFillerMaterial(),
                localeService.trRaw(player, "gui.filler.name", localSettings.getFillerName(), Collections.<String, String>emptyMap()),
                localeService.trRawList(player, "gui.filler.lore", localSettings.getFillerLore(), Collections.<String, String>emptyMap()),
                Collections.<String, String>emptyMap()
            );
            for (int i = 0; i < inventory.getSize(); i++) {
                inventory.setItem(i, filler);
            }
        }

        session.clearSlotBindings();
        List<String> filenames = session.getFilenames();
        List<Integer> slots = localSettings.getContentSlots();
        int page = session.getPage();
        int itemsPerPage = Math.max(1, slots.size());
        int start = (page - 1) * itemsPerPage;
        int end = Math.min(filenames.size(), start + itemsPerPage);

        for (int i = start; i < end; i++) {
            String filename = filenames.get(i);
            int slotIndex = i - start;
            if (slotIndex < 0 || slotIndex >= slots.size()) {
                continue;
            }

            int slot = slots.get(slotIndex);
            Map<String, String> placeholders = createPagePlaceholders(session, filenames.size());
            addFilenamePlaceholders(placeholders, filename);
            String imageIconNameTemplate = localeService.trRaw(
                player,
                "gui.image-icon.name",
                localSettings.getImageIconNameFormat(),
                Collections.<String, String>emptyMap()
            );
            List<String> imageIconLoreTemplate = localeService.trRawList(
                player,
                "gui.image-icon.lore",
                localSettings.getImageIconLore(),
                Collections.<String, String>emptyMap()
            );
            DisplayMetadataService.DisplayMetadata displayMetadata = displayMetadataService.resolve(
                player,
                localSettings,
                filename,
                imageIconNameTemplate,
                imageIconLoreTemplate
            );
            ItemStack icon = createIcon(
                localSettings.getImageIconMaterial(),
                displayMetadata.getNameTemplate(),
                displayMetadata.getLoreTemplates(),
                placeholders
            );
            inventory.setItem(slot, icon);
            session.getSlotToFilename().put(slot, filename);
        }

        if (filenames.isEmpty()) {
            ItemStack emptyIcon = createIcon(
                localSettings.getEmptyMaterial(),
                localeService.trRaw(player, "gui.empty-state.name", localSettings.getEmptyName(), Collections.<String, String>emptyMap()),
                localeService.trRawList(player, "gui.empty-state.lore", localSettings.getEmptyLore(), Collections.<String, String>emptyMap()),
                createPagePlaceholders(session, 0)
            );
            inventory.setItem(localSettings.getEmptySlot(), emptyIcon);
        }

        int maxPages = session.getMaxPages(itemsPerPage);
        if (session.getPage() > 1) {
            Map<String, String> placeholders = createPagePlaceholders(session, filenames.size());
            placeholders.put("target_page", String.valueOf(session.getPage() - 1));
            inventory.setItem(localSettings.getPreviousSlot(), createIcon(
                localSettings.getPreviousMaterial(),
                localeService.trRaw(player, "gui.navigation.previous.name", localSettings.getPreviousName(), Collections.<String, String>emptyMap()),
                localeService.trRawList(player, "gui.navigation.previous.lore", localSettings.getPreviousLore(), Collections.<String, String>emptyMap()),
                placeholders
            ));
        }

        if (session.getPage() < maxPages) {
            Map<String, String> placeholders = createPagePlaceholders(session, filenames.size());
            placeholders.put("target_page", String.valueOf(session.getPage() + 1));
            inventory.setItem(localSettings.getNextSlot(), createIcon(
                localSettings.getNextMaterial(),
                localeService.trRaw(player, "gui.navigation.next.name", localSettings.getNextName(), Collections.<String, String>emptyMap()),
                localeService.trRawList(player, "gui.navigation.next.lore", localSettings.getNextLore(), Collections.<String, String>emptyMap()),
                placeholders
            ));
        }

        inventory.setItem(localSettings.getRefreshSlot(), createIcon(
            localSettings.getRefreshMaterial(),
            localeService.trRaw(player, "gui.navigation.refresh.name", localSettings.getRefreshName(), Collections.<String, String>emptyMap()),
            localeService.trRawList(player, "gui.navigation.refresh.lore", localSettings.getRefreshLore(), Collections.<String, String>emptyMap()),
            createPagePlaceholders(session, filenames.size())
        ));

        if (localSettings.isLanguageMenuEnabled()) {
            Map<String, String> placeholders = createPagePlaceholders(session, filenames.size());
            String currentLanguage = localeService.resolveLanguage(player);
            placeholders.put("language", localeService.getLanguageLabel(player, currentLanguage));
            placeholders.put("language_code", currentLanguage.toUpperCase(Locale.ROOT));
            placeholders.put("languages", String.valueOf(localeService.getAvailableLanguages().size()));
            inventory.setItem(localSettings.getLanguageSlot(), createIcon(
                localSettings.getLanguageMaterial(),
                localeService.trRaw(player, "gui.navigation.language.name", localSettings.getLanguageName(), Collections.<String, String>emptyMap()),
                localeService.trRawList(player, "gui.navigation.language.lore", localSettings.getLanguageLore(), Collections.<String, String>emptyMap()),
                placeholders
            ));
        }

        inventory.setItem(localSettings.getInfoSlot(), createIcon(
            localSettings.getInfoMaterial(),
            localeService.trRaw(player, "gui.navigation.info.name", localSettings.getInfoName(), Collections.<String, String>emptyMap()),
            localeService.trRawList(player, "gui.navigation.info.lore", localSettings.getInfoLore(), Collections.<String, String>emptyMap()),
            createPagePlaceholders(session, filenames.size())
        ));

        inventory.setItem(localSettings.getCloseSlot(), createIcon(
            localSettings.getCloseMaterial(),
            localeService.trRaw(player, "gui.navigation.close.name", localSettings.getCloseName(), Collections.<String, String>emptyMap()),
            localeService.trRawList(player, "gui.navigation.close.lore", localSettings.getCloseLore(), Collections.<String, String>emptyMap()),
            createPagePlaceholders(session, filenames.size())
        ));
    }

    private ItemStack createIcon(Material material, String name, List<String> lore, Map<String, String> placeholders) {
        Material effectiveMaterial = material == null ? Material.PAPER : material;
        ItemStack item = new ItemStack(effectiveMaterial, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setDisplayName(Texts.applyPlaceholders(name, placeholders));

        List<String> outputLore = new ArrayList<String>();
        for (String line : lore) {
            outputLore.add(Texts.applyPlaceholders(line, placeholders));
        }
        meta.setLore(outputLore);

        item.setItemMeta(meta);
        return item;
    }

    private Map<String, String> createPagePlaceholders(GuiSession session, int totalCount) {
        AddonSettings localSettings = settings;
        int itemsPerPage = Math.max(1, localSettings.getContentSlots().size());
        int maxPage = Math.max(1, session.getMaxPages(itemsPerPage));
        Map<String, String> placeholders = new HashMap<String, String>();
        placeholders.put("current_page", String.valueOf(session.getPage()));
        placeholders.put("page", String.valueOf(session.getPage()));
        placeholders.put("max_page", String.valueOf(maxPage));
        placeholders.put("count", String.valueOf(totalCount));
        return placeholders;
    }

    private void cyclePlayerLanguage(Player player, GuiSession session) {
        AddonSettings localSettings = settings;
        if (localSettings == null) {
            return;
        }

        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        long cooldownMs = localSettings.getLanguageChangeCooldownMs();
        if (cooldownMs > 0L) {
            Long lastChangedAt = lastLanguageChangeAt.get(playerId);
            if (lastChangedAt != null) {
                long elapsed = now - lastChangedAt;
                long remainingMs = cooldownMs - elapsed;
                if (remainingMs > 0L) {
                    Map<String, String> placeholders = new HashMap<String, String>();
                    placeholders.put("remaining_ms", String.valueOf(remainingMs));
                    long remainingSeconds = Math.max(1L, (long) Math.ceil(remainingMs / 1000.0D));
                    placeholders.put("remaining_seconds", String.valueOf(remainingSeconds));
                    player.sendMessage(localeService.tr(player, "language-change-cooldown", placeholders));
                    return;
                }
            }
        }

        String previousLanguage = localeService.resolveLanguage(player);
        String nextLanguage = localeService.cyclePlayerLanguage(player);

        Map<String, String> placeholders = new HashMap<String, String>();
        placeholders.put("language", localeService.getLanguageLabel(player, nextLanguage));
        placeholders.put("language_code", nextLanguage.toUpperCase(Locale.ROOT));

        if (!nextLanguage.equals(previousLanguage)) {
            lastLanguageChangeAt.put(playerId, now);
            player.sendMessage(localeService.tr(player, "language-changed", placeholders));
        } else {
            player.sendMessage(localeService.tr(player, "language-change-unavailable", placeholders));
        }

        openWithData(player, session.getFilenames(), session.getPage());
    }

    private void addFilenamePlaceholders(Map<String, String> placeholders, String filename) {
        String basename = Texts.getBasename(filename);
        placeholders.put("filename", basename);
        placeholders.put("basename", basename);
        placeholders.put("filepath", Texts.getPathWithoutExtension(filename));
    }

    private List<String> getVisibleFilenames(Player player) {
        AddonSettings localSettings = settings;
        CachedFilenames cached = filenameCache.get(player.getUniqueId());
        long now = System.currentTimeMillis();
        if (cached != null && cached.expiresAt >= now) {
            return new ArrayList<String>(cached.filenames);
        }

        List<String> filenames = new ArrayList<String>(corePlugin.getStorage().getFilenames(player));
        if (localSettings.isRequirePublicOrOwn()) {
            List<String> filtered = new ArrayList<String>();
            for (String filename : filenames) {
                if (matchesVisibilityRules(localSettings, player, filename)) {
                    filtered.add(filename);
                }
            }
            filenames = filtered;
        }

        filenameCache.put(player.getUniqueId(), new CachedFilenames(filenames, now + localSettings.getCacheTtlMs()));
        return new ArrayList<String>(filenames);
    }

    private boolean matchesVisibilityRules(AddonSettings localSettings, Player player, String filename) {
        String ownPattern = applyPatternTokens(localSettings.getOwnPathPattern(), player);
        String publicPattern = applyPatternTokens(localSettings.getPublicPathPattern(), player);

        try {
            boolean matchesOwn = Pattern.compile(ownPattern).matcher(filename).find();
            boolean matchesPublic = Pattern.compile(publicPattern).matcher(filename).find();
            return matchesOwn || matchesPublic;
        } catch (PatternSyntaxException ex) {
            plugin.getLogger().warning("Invalid visibility regex in addon config: " + ex.getMessage());
            return false;
        }
    }

    private String applyPatternTokens(String pattern, Player player) {
        String value = pattern == null ? "" : pattern;
        value = value.replaceAll("#player#", Matcher.quoteReplacement(Pattern.quote(player.getName())));
        value = value.replaceAll("#uuid#", player.getUniqueId().toString());
        return value;
    }

    private void claimImage(Player player, String filename) {
        AddonSettings localSettings = settings;
        UUID playerId = player.getUniqueId();

        if (localSettings.isClaimCooldownEnabled()) {
            long now = System.currentTimeMillis();
            Long lastAt = lastClaimAt.get(playerId);
            if (lastAt != null && now - lastAt < localSettings.getClaimCooldownMs()) {
                player.sendMessage(localeService.tr(player, "claim-cooldown"));
                return;
            }
            lastClaimAt.put(playerId, now);
        }

        if (localSettings.isClaimLockEnabled() && !claimLocks.add(playerId)) {
            return;
        }

        try {
            ImageFile imageFile = corePlugin.getStorage().get(filename);
            if (imageFile == null || !corePlugin.getStorage().isPathAllowed(filename, player)) {
                player.sendMessage(localeService.tr(player, "image-unavailable"));
                GuiSession session = sessions.get(playerId);
                if (session != null) {
                    refreshMenu(player, session);
                }
                return;
            }

            ItemStack claimItem;
            try {
                String itemNameTemplate = localeService.trRaw(
                    player,
                    "item.name-format",
                    localSettings.getClaimItemNameFormat(),
                    Collections.<String, String>emptyMap()
                );
                List<String> itemLoreTemplate = localeService.trRawList(
                    player,
                    "item.lore",
                    localSettings.getClaimItemLore(),
                    Collections.<String, String>emptyMap()
                );
                DisplayMetadataService.DisplayMetadata displayMetadata = displayMetadataService.resolve(
                    player,
                    localSettings,
                    filename,
                    itemNameTemplate,
                    itemLoreTemplate
                );
                claimItem = ImageItemFactory.createClaimItem(player, imageFile, localSettings, displayMetadata);
            } catch (Exception ex) {
                player.sendMessage(localeService.tr(player, "image-unavailable"));
                return;
            }

            if (!InventoryUtil.hasSpaceFor(player.getInventory(), claimItem)) {
                player.sendMessage(localeService.tr(player, "inventory-full"));
                return;
            }

            HourlyLimitService.LimitResult limitResult = hourlyLimitService.tryConsume(playerId, claimItem.getAmount(), localSettings);
            if (!limitResult.isAllowed()) {
                Map<String, String> placeholders = new HashMap<String, String>();
                placeholders.put("limit", String.valueOf(limitResult.getLimit()));
                player.sendMessage(localeService.tr(player, "hourly-limit-reached", placeholders));
                return;
            }

            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(claimItem);
            if (!leftovers.isEmpty()) {
                hourlyLimitService.rollback(playerId, claimItem.getAmount());
                player.sendMessage(localeService.tr(player, "inventory-full"));
                return;
            }

            Map<String, String> placeholders = new HashMap<String, String>();
            placeholders.put("amount", String.valueOf(claimItem.getAmount()));
            placeholders.put("filename", Texts.getBasename(filename));
            placeholders.put("filepath", Texts.getPathWithoutExtension(filename));
            player.sendMessage(localeService.tr(player, "item-given", placeholders));
        } finally {
            if (localSettings.isClaimLockEnabled()) {
                claimLocks.remove(playerId);
            }
        }
    }

    private static class CachedFilenames {
        final List<String> filenames;
        final long expiresAt;

        CachedFilenames(List<String> filenames, long expiresAt) {
            this.filenames = new ArrayList<String>(filenames);
            this.expiresAt = expiresAt;
        }
    }
}
