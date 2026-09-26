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
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
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
    private final AtomicInteger searchGeneration = new AtomicInteger(0);
    private final Map<UUID, SearchPrompt> searchPrompts = new ConcurrentHashMap<UUID, SearchPrompt>();
    private volatile AddonSettings settings;
    private EditorService editorService;

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

    public void setEditorService(EditorService editorService) {
        this.editorService = editorService;
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
        if (player != null) {
            searchPrompts.remove(player.getUniqueId());
        }
        openMenuKeepingPrompt(player, requestedPage, null);
    }

    public boolean consumeSearchChat(Player player, String text) {
        if (player == null || !searchPrompts.containsKey(player.getUniqueId())) {
            return false;
        }

        final SearchPrompt prompt = searchPrompts.remove(player.getUniqueId());
        if (prompt == null) {
            return false;
        }

        final String message = text == null ? "" : text;
        player.getScheduler().run(plugin, new Consumer<ScheduledTask>() {
            @Override
            public void accept(ScheduledTask scheduledTask) {
                applySearchPrompt(player, prompt, message);
            }
        }, null);
        return true;
    }

    private void openMenuKeepingPrompt(Player player, int requestedPage, String searchQuery) {
        if (player != null && editorService != null && editorService.hasHeldItem(player.getUniqueId())) {
            editorService.openEditor(player);
            return;
        }
        AddonSettings localSettings = settings;
        if (localSettings == null || !localSettings.isEnabled()) {
            return;
        }

        List<String> filenames = getVisibleFilenames(player);
        if (filenames.isEmpty()) {
            player.sendMessage(localeService.tr(player, "menu-empty"));
        }
        openWithData(player, filenames, requestedPage, searchQuery);
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

        if (localSettings.isSearchEnabled() && rawSlot == localSettings.getSearchSlot()) {
            handleSearchClick(player, session, clickType);
            return;
        }

        if (localSettings.isEditorEnabled() && rawSlot == localSettings.getEditorSlot()) {
            if (editorService != null) {
                editorService.openEditor(player);
            }
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
        searchPrompts.remove(playerId);
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
        searchPrompts.clear();
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
        session.setAllFilenames(filenames);
        session.setFilenames(filterFilenames(player, filenames, session.getSearchQuery()));
        int maxPages = session.getMaxPages(settings.getContentSlots().size());
        if (session.getPage() > maxPages) {
            session.setPage(maxPages);
        }
        rerenderOpenMenu(player, session);
    }

    private void openWithData(Player player, List<String> filenames, int requestedPage, String searchQuery) {
        AddonSettings localSettings = settings;
        if (localSettings == null) {
            return;
        }

        int itemsPerPage = Math.max(1, localSettings.getContentSlots().size());
        int page = Math.max(1, requestedPage);

        long token = tokenCounter.incrementAndGet();
        GuiSession session = new GuiSession(player.getUniqueId(), token, filenames, page);
        session.setSearchQuery(normalizeQuery(searchQuery));
        session.setFilenames(filterFilenames(player, session.getAllFilenames(), session.getSearchQuery()));
        int maxPages = session.getMaxPages(itemsPerPage);
        if (session.getPage() > maxPages) {
            session.setPage(maxPages);
        }
        sessions.put(player.getUniqueId(), session);

        ImguiMenuHolder holder = new ImguiMenuHolder(player.getUniqueId(), token);
        Map<String, String> placeholders = createPagePlaceholders(session, session.getFilenames().size());
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

        boolean showCatalogEmpty = session.getAllFilenames().isEmpty();
        boolean showSearchEmpty = !showCatalogEmpty && session.getFilenames().isEmpty() && session.getSearchQuery() != null;
        int emptySlot = localSettings.getEmptySlot();
        if (localSettings.isFillerEnabled()) {
            ItemStack filler = createIcon(
                localSettings.getFillerMaterial(),
                localeService.trRaw(player, "gui.filler.name", localSettings.getFillerName(), Collections.<String, String>emptyMap()),
                localeService.trRawList(player, "gui.filler.lore", localSettings.getFillerLore(), Collections.<String, String>emptyMap()),
                Collections.<String, String>emptyMap()
            );
            for (int i = 0; i < inventory.getSize(); i++) {
                if ((showCatalogEmpty || showSearchEmpty) && i == emptySlot) {
                    continue;
                }
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
            ImageFile imageFile = corePlugin.getStorage().get(filename);
            ImageItemFactory.ResolvedSize resolvedSize = ImageItemFactory.resolveSize(
                player,
                imageFile,
                localSettings,
                displayMetadata
            );
            placeholders.put("width", String.valueOf(resolvedSize.getWidth()));
            placeholders.put("height", String.valueOf(resolvedSize.getHeight()));
            String iconName = ImageItemFactory.appendSizeSuffixIfNeeded(
                displayMetadata.getNameTemplate(),
                displayMetadata.getLoreTemplates(),
                resolvedSize.getWidth(),
                resolvedSize.getHeight()
            );
            ItemStack icon = createIcon(
                localSettings.getImageIconMaterial(),
                iconName,
                displayMetadata.getLoreTemplates(),
                placeholders
            );
            inventory.setItem(slot, icon);
            session.getSlotToFilename().put(slot, filename);
        }

        if (session.getAllFilenames().isEmpty()) {
            ItemStack emptyIcon = createIcon(
                localSettings.getEmptyMaterial(),
                localeService.trRaw(player, "gui.empty-state.name", localSettings.getEmptyName(), Collections.<String, String>emptyMap()),
                localeService.trRawList(player, "gui.empty-state.lore", localSettings.getEmptyLore(), Collections.<String, String>emptyMap()),
                createPagePlaceholders(session, 0)
            );
            inventory.setItem(localSettings.getEmptySlot(), emptyIcon);
        } else if (filenames.isEmpty() && session.getSearchQuery() != null) {
            Map<String, String> placeholders = createPagePlaceholders(session, 0);
            placeholders.put("query", displayQuery(session.getSearchQuery()));
            ItemStack emptySearch = createIcon(
                Material.BARRIER,
                localeService.trRaw(player, "gui.search-empty.name", "&7Nothing found", Collections.<String, String>emptyMap()),
                localeService.trRawList(player, "gui.search-empty.lore", Collections.singletonList("&8Search: &f{query}"), Collections.<String, String>emptyMap()),
                placeholders
            );
            inventory.setItem(localSettings.getEmptySlot(), emptySearch);
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

        if (localSettings.isSearchEnabled()) {
            inventory.setItem(localSettings.getSearchSlot(), createSearchIcon(player, session));
        }

        if (localSettings.isEditorEnabled()) {
            session.getSlotToFilename().remove(Integer.valueOf(localSettings.getEditorSlot()));
            inventory.setItem(localSettings.getEditorSlot(), createIcon(
                localSettings.getEditorMaterial(),
                localeService.trRaw(player, "gui.navigation.editor.name", localSettings.getEditorName(), Collections.<String, String>emptyMap()),
                localeService.trRawList(player, "gui.navigation.editor.lore", localSettings.getEditorLore(), Collections.<String, String>emptyMap()),
                Collections.<String, String>emptyMap()
            ));
        }

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

        openWithData(player, session.getAllFilenames(), session.getPage(), session.getSearchQuery());
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

    private void handleSearchClick(Player player, GuiSession session, ClickType clickType) {
        String query = session.getSearchQuery();
        boolean active = query != null && !query.isEmpty();
        if (active && clickType.isLeftClick()) {
            openWithData(player, session.getAllFilenames(), 1, null);
            return;
        }
        if ((active && clickType.isRightClick()) || (!active && clickType.isLeftClick())) {
            beginSearchPrompt(player, session);
        }
    }

    private void beginSearchPrompt(Player player, GuiSession session) {
        final int generation = searchGeneration.incrementAndGet();
        final UUID playerId = player.getUniqueId();
        searchPrompts.put(playerId, new SearchPrompt(generation, session.getPage(), session.getSearchQuery()));
        player.closeInventory();

        Map<String, String> placeholders = new HashMap<String, String>();
        placeholders.put("max_chars", "36");
        sendLocalized(
            player,
            "search-prompt",
            "Type a search (max {max_chars} characters). cancel to stop.",
            placeholders
        );

        corePlugin.getScheduler().runInGame(new Runnable() {
            @Override
            public void run() {
                SearchPrompt current = searchPrompts.get(playerId);
                if (current != null && current.generation == generation) {
                    searchPrompts.remove(playerId);
                }
            }
        }, 20L * 60L);
    }

    private void applySearchPrompt(Player player, SearchPrompt prompt, String text) {
        if (player == null || !player.isOnline()) {
            return;
        }

        String trimmed = text == null ? "" : text.trim();
        if ("cancel".equalsIgnoreCase(trimmed) || "cancelar".equalsIgnoreCase(trimmed)) {
            sendLocalized(player, "search-cancelled", "Search cancelled", Collections.<String, String>emptyMap());
            openMenuKeepingPrompt(player, prompt.page, prompt.query);
            return;
        }
        if (trimmed.isEmpty()) {
            openMenuKeepingPrompt(player, prompt.page, prompt.query);
            return;
        }
        openMenuKeepingPrompt(player, 1, trimmed);
    }

    private ItemStack createSearchIcon(Player player, GuiSession session) {
        AddonSettings localSettings = settings;
        String query = session.getSearchQuery();
        Map<String, String> placeholders = createPagePlaceholders(session, session.getFilenames().size());
        if (query == null || query.isEmpty()) {
            return createIcon(
                localSettings.getSearchMaterial(),
                localeService.trRaw(player, "gui.navigation.search.name", localSettings.getSearchName(), Collections.<String, String>emptyMap()),
                localeService.trRawList(player, "gui.navigation.search.lore", localSettings.getSearchLore(), Collections.<String, String>emptyMap()),
                placeholders
            );
        }

        placeholders.put("query", displayQuery(query));
        return createIcon(
            localSettings.getSearchMaterial(),
            localeService.trRaw(player, "gui.navigation.search.active-name", localSettings.getSearchActiveName(), Collections.<String, String>emptyMap()),
            localeService.trRawList(player, "gui.navigation.search.active-lore", localSettings.getSearchActiveLore(), Collections.<String, String>emptyMap()),
            placeholders
        );
    }

    private List<String> filterFilenames(Player player, List<String> filenames, String query) {
        String needle = normalizeQuery(query);
        if (needle == null) {
            return new ArrayList<String>(filenames);
        }

        AddonSettings localSettings = settings;
        List<String> filtered = new ArrayList<String>();
        for (int i = 0; i < filenames.size(); i++) {
            String filename = filenames.get(i);
            if (matchesSearch(player, localSettings, filename, needle)) {
                filtered.add(filename);
            }
        }
        return filtered;
    }

    private boolean matchesSearch(Player player, AddonSettings localSettings, String filename, String query) {
        String needle = query.toLowerCase(Locale.ROOT);
        if (filename != null && filename.toLowerCase(Locale.ROOT).contains(needle)) {
            return true;
        }

        String basename = Texts.getBasename(filename).toLowerCase(Locale.ROOT);
        if (basename.contains(needle)) {
            return true;
        }

        String filepath = Texts.getPathWithoutExtension(filename).toLowerCase(Locale.ROOT);
        if (filepath.contains(needle)) {
            return true;
        }

        String plainName = plainDisplayName(player, localSettings, filename);
        return plainName.toLowerCase(Locale.ROOT).contains(needle);
    }

    private String plainDisplayName(Player player, AddonSettings localSettings, String filename) {
        if (localSettings == null || filename == null) {
            return "";
        }

        String imageIconNameTemplate = localeService.trRaw(
            player,
            "gui.image-icon.name",
            localSettings.getImageIconNameFormat(),
            Collections.<String, String>emptyMap()
        );
        DisplayMetadataService.DisplayMetadata displayMetadata = displayMetadataService.resolve(
            player,
            localSettings,
            filename,
            imageIconNameTemplate,
            Collections.<String>emptyList()
        );
        Map<String, String> placeholders = new HashMap<String, String>();
        addFilenamePlaceholders(placeholders, filename);
        String resolved = Texts.applyPlaceholders(displayMetadata.getNameTemplate(), placeholders);
        String stripped = ChatColor.stripColor(resolved);
        return stripped == null ? "" : stripped;
    }

    private void sendLocalized(Player player, String key, String fallback, Map<String, String> placeholders) {
        AddonSettings localSettings = settings;
        String prefix = localSettings == null ? "" : Texts.applyPlaceholders(localSettings.getPrefix(), placeholders);
        String body = localeService.trRaw(player, "messages." + key, fallback, placeholders);
        player.sendMessage(prefix + body);
    }

    private String normalizeQuery(String query) {
        if (query == null) {
            return null;
        }
        String trimmed = query.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed;
    }

    private String displayQuery(String query) {
        String shown = query;
        if (shown.length() > 26) {
            shown = shown.substring(0, 26);
        }
        return shown.replace("&", "").replace("\u00A7", "").replace("{", "").replace("}", "");
    }

    private static final class SearchPrompt {
        private final int generation;
        private final int page;
        private final String query;

        private SearchPrompt(int generation, int page, String query) {
            this.generation = generation;
            this.page = page;
            this.query = query;
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
