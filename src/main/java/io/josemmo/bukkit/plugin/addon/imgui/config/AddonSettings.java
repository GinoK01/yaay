package io.josemmo.bukkit.plugin.addon.imgui.config;

import org.bukkit.Material;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AddonSettings {
    private final boolean enabled;
    private final boolean usePlayerLocale;
    private final long languageChangeCooldownMs;
    private final String fallbackLanguage;
    private final String prefix;
    private final String usePermission;
    private final String reloadPermission;
    private final boolean reloadSubcommandEnabled;
    private final boolean requirePublicOrOwn;
    private final String ownPathPattern;
    private final String publicPathPattern;
    private final Material claimItemMaterial;
    private final int claimItemAmount;
    private final int claimItemWidth;
    private final int claimItemHeight;
    private final boolean claimItemAutoHeight;
    private final int claimItemFlags;
    private final String claimItemNameFormat;
    private final boolean claimItemClearLore;
    private final List<String> claimItemLore;
    private final boolean asyncOpen;
    private final long cacheTtlMs;
    private final boolean closeMenusOnReload;
    private final boolean autoReloadEnabled;
    private final long autoReloadDebounceMs;
    private final String menuTitle;
    private final int menuRows;
    private final List<Integer> contentSlots;
    private final boolean fillerEnabled;
    private final Material fillerMaterial;
    private final String fillerName;
    private final List<String> fillerLore;
    private final Material imageIconMaterial;
    private final String imageIconNameFormat;
    private final List<String> imageIconLore;
    private final int previousSlot;
    private final Material previousMaterial;
    private final String previousName;
    private final List<String> previousLore;
    private final int nextSlot;
    private final Material nextMaterial;
    private final String nextName;
    private final List<String> nextLore;
    private final int refreshSlot;
    private final Material refreshMaterial;
    private final String refreshName;
    private final List<String> refreshLore;
    private final boolean languageMenuEnabled;
    private final int languageSlot;
    private final Material languageMaterial;
    private final String languageName;
    private final List<String> languageLore;
    private final int infoSlot;
    private final Material infoMaterial;
    private final String infoName;
    private final List<String> infoLore;
    private final int closeSlot;
    private final Material closeMaterial;
    private final String closeName;
    private final List<String> closeLore;
    private final int emptySlot;
    private final Material emptyMaterial;
    private final String emptyName;
    private final List<String> emptyLore;
    private final boolean hourlyLimitEnabled;
    private final int hourlyLimitMaxItems;
    private final boolean claimCooldownEnabled;
    private final long claimCooldownMs;
    private final boolean strictInventoryLock;
    private final boolean blockShiftClick;
    private final boolean blockNumberKey;
    private final boolean blockCollectToCursor;
    private final boolean blockDrag;
    private final boolean blockCreativeActions;
    private final boolean claimLockEnabled;

    public AddonSettings(
        boolean enabled,
        boolean usePlayerLocale,
        long languageChangeCooldownMs,
        String fallbackLanguage,
        String prefix,
        String usePermission,
        String reloadPermission,
        boolean reloadSubcommandEnabled,
        boolean requirePublicOrOwn,
        String ownPathPattern,
        String publicPathPattern,
        Material claimItemMaterial,
        int claimItemAmount,
        int claimItemWidth,
        int claimItemHeight,
        boolean claimItemAutoHeight,
        int claimItemFlags,
        String claimItemNameFormat,
        boolean claimItemClearLore,
        List<String> claimItemLore,
        boolean asyncOpen,
        long cacheTtlMs,
        boolean closeMenusOnReload,
        boolean autoReloadEnabled,
        long autoReloadDebounceMs,
        String menuTitle,
        int menuRows,
        List<Integer> contentSlots,
        boolean fillerEnabled,
        Material fillerMaterial,
        String fillerName,
        List<String> fillerLore,
        Material imageIconMaterial,
        String imageIconNameFormat,
        List<String> imageIconLore,
        int previousSlot,
        Material previousMaterial,
        String previousName,
        List<String> previousLore,
        int nextSlot,
        Material nextMaterial,
        String nextName,
        List<String> nextLore,
        int refreshSlot,
        Material refreshMaterial,
        String refreshName,
        List<String> refreshLore,
        boolean languageMenuEnabled,
        int languageSlot,
        Material languageMaterial,
        String languageName,
        List<String> languageLore,
        int infoSlot,
        Material infoMaterial,
        String infoName,
        List<String> infoLore,
        int closeSlot,
        Material closeMaterial,
        String closeName,
        List<String> closeLore,
        int emptySlot,
        Material emptyMaterial,
        String emptyName,
        List<String> emptyLore,
        boolean hourlyLimitEnabled,
        int hourlyLimitMaxItems,
        boolean claimCooldownEnabled,
        long claimCooldownMs,
        boolean strictInventoryLock,
        boolean blockShiftClick,
        boolean blockNumberKey,
        boolean blockCollectToCursor,
        boolean blockDrag,
        boolean blockCreativeActions,
        boolean claimLockEnabled
    ) {
        this.enabled = enabled;
        this.usePlayerLocale = usePlayerLocale;
        this.languageChangeCooldownMs = languageChangeCooldownMs;
        this.fallbackLanguage = fallbackLanguage;
        this.prefix = prefix;
        this.usePermission = usePermission;
        this.reloadPermission = reloadPermission;
        this.reloadSubcommandEnabled = reloadSubcommandEnabled;
        this.requirePublicOrOwn = requirePublicOrOwn;
        this.ownPathPattern = ownPathPattern;
        this.publicPathPattern = publicPathPattern;
        this.claimItemMaterial = claimItemMaterial;
        this.claimItemAmount = claimItemAmount;
        this.claimItemWidth = claimItemWidth;
        this.claimItemHeight = claimItemHeight;
        this.claimItemAutoHeight = claimItemAutoHeight;
        this.claimItemFlags = claimItemFlags;
        this.claimItemNameFormat = claimItemNameFormat;
        this.claimItemClearLore = claimItemClearLore;
        this.claimItemLore = immutableList(claimItemLore);
        this.asyncOpen = asyncOpen;
        this.cacheTtlMs = cacheTtlMs;
        this.closeMenusOnReload = closeMenusOnReload;
        this.autoReloadEnabled = autoReloadEnabled;
        this.autoReloadDebounceMs = autoReloadDebounceMs;
        this.menuTitle = menuTitle;
        this.menuRows = menuRows;
        this.contentSlots = immutableList(contentSlots);
        this.fillerEnabled = fillerEnabled;
        this.fillerMaterial = fillerMaterial;
        this.fillerName = fillerName;
        this.fillerLore = immutableList(fillerLore);
        this.imageIconMaterial = imageIconMaterial;
        this.imageIconNameFormat = imageIconNameFormat;
        this.imageIconLore = immutableList(imageIconLore);
        this.previousSlot = previousSlot;
        this.previousMaterial = previousMaterial;
        this.previousName = previousName;
        this.previousLore = immutableList(previousLore);
        this.nextSlot = nextSlot;
        this.nextMaterial = nextMaterial;
        this.nextName = nextName;
        this.nextLore = immutableList(nextLore);
        this.refreshSlot = refreshSlot;
        this.refreshMaterial = refreshMaterial;
        this.refreshName = refreshName;
        this.refreshLore = immutableList(refreshLore);
        this.languageMenuEnabled = languageMenuEnabled;
        this.languageSlot = languageSlot;
        this.languageMaterial = languageMaterial;
        this.languageName = languageName;
        this.languageLore = immutableList(languageLore);
        this.infoSlot = infoSlot;
        this.infoMaterial = infoMaterial;
        this.infoName = infoName;
        this.infoLore = immutableList(infoLore);
        this.closeSlot = closeSlot;
        this.closeMaterial = closeMaterial;
        this.closeName = closeName;
        this.closeLore = immutableList(closeLore);
        this.emptySlot = emptySlot;
        this.emptyMaterial = emptyMaterial;
        this.emptyName = emptyName;
        this.emptyLore = immutableList(emptyLore);
        this.hourlyLimitEnabled = hourlyLimitEnabled;
        this.hourlyLimitMaxItems = hourlyLimitMaxItems;
        this.claimCooldownEnabled = claimCooldownEnabled;
        this.claimCooldownMs = claimCooldownMs;
        this.strictInventoryLock = strictInventoryLock;
        this.blockShiftClick = blockShiftClick;
        this.blockNumberKey = blockNumberKey;
        this.blockCollectToCursor = blockCollectToCursor;
        this.blockDrag = blockDrag;
        this.blockCreativeActions = blockCreativeActions;
        this.claimLockEnabled = claimLockEnabled;
    }

    private static <T> List<T> immutableList(List<T> value) {
        if (value == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<T>(value));
    }

    public boolean isEnabled() { return enabled; }
    public boolean isUsePlayerLocale() { return usePlayerLocale; }
    public long getLanguageChangeCooldownMs() { return languageChangeCooldownMs; }
    public String getFallbackLanguage() { return fallbackLanguage; }
    public String getPrefix() { return prefix; }
    public String getUsePermission() { return usePermission; }
    public String getReloadPermission() { return reloadPermission; }
    public boolean isReloadSubcommandEnabled() { return reloadSubcommandEnabled; }
    public boolean isRequirePublicOrOwn() { return requirePublicOrOwn; }
    public String getOwnPathPattern() { return ownPathPattern; }
    public String getPublicPathPattern() { return publicPathPattern; }
    public Material getClaimItemMaterial() { return claimItemMaterial; }
    public int getClaimItemAmount() { return claimItemAmount; }
    public int getClaimItemWidth() { return claimItemWidth; }
    public int getClaimItemHeight() { return claimItemHeight; }
    public boolean isClaimItemAutoHeight() { return claimItemAutoHeight; }
    public int getClaimItemFlags() { return claimItemFlags; }
    public String getClaimItemNameFormat() { return claimItemNameFormat; }
    public boolean isClaimItemClearLore() { return claimItemClearLore; }
    public List<String> getClaimItemLore() { return claimItemLore; }
    public boolean isAsyncOpen() { return asyncOpen; }
    public long getCacheTtlMs() { return cacheTtlMs; }
    public boolean isCloseMenusOnReload() { return closeMenusOnReload; }
    public boolean isAutoReloadEnabled() { return autoReloadEnabled; }
    public long getAutoReloadDebounceMs() { return autoReloadDebounceMs; }
    public String getMenuTitle() { return menuTitle; }
    public int getMenuRows() { return menuRows; }
    public List<Integer> getContentSlots() { return contentSlots; }
    public boolean isFillerEnabled() { return fillerEnabled; }
    public Material getFillerMaterial() { return fillerMaterial; }
    public String getFillerName() { return fillerName; }
    public List<String> getFillerLore() { return fillerLore; }
    public Material getImageIconMaterial() { return imageIconMaterial; }
    public String getImageIconNameFormat() { return imageIconNameFormat; }
    public List<String> getImageIconLore() { return imageIconLore; }
    public int getPreviousSlot() { return previousSlot; }
    public Material getPreviousMaterial() { return previousMaterial; }
    public String getPreviousName() { return previousName; }
    public List<String> getPreviousLore() { return previousLore; }
    public int getNextSlot() { return nextSlot; }
    public Material getNextMaterial() { return nextMaterial; }
    public String getNextName() { return nextName; }
    public List<String> getNextLore() { return nextLore; }
    public int getRefreshSlot() { return refreshSlot; }
    public Material getRefreshMaterial() { return refreshMaterial; }
    public String getRefreshName() { return refreshName; }
    public List<String> getRefreshLore() { return refreshLore; }
    public boolean isLanguageMenuEnabled() { return languageMenuEnabled; }
    public int getLanguageSlot() { return languageSlot; }
    public Material getLanguageMaterial() { return languageMaterial; }
    public String getLanguageName() { return languageName; }
    public List<String> getLanguageLore() { return languageLore; }
    public int getInfoSlot() { return infoSlot; }
    public Material getInfoMaterial() { return infoMaterial; }
    public String getInfoName() { return infoName; }
    public List<String> getInfoLore() { return infoLore; }
    public int getCloseSlot() { return closeSlot; }
    public Material getCloseMaterial() { return closeMaterial; }
    public String getCloseName() { return closeName; }
    public List<String> getCloseLore() { return closeLore; }
    public int getEmptySlot() { return emptySlot; }
    public Material getEmptyMaterial() { return emptyMaterial; }
    public String getEmptyName() { return emptyName; }
    public List<String> getEmptyLore() { return emptyLore; }
    public boolean isHourlyLimitEnabled() { return hourlyLimitEnabled; }
    public int getHourlyLimitMaxItems() { return hourlyLimitMaxItems; }
    public boolean isClaimCooldownEnabled() { return claimCooldownEnabled; }
    public long getClaimCooldownMs() { return claimCooldownMs; }
    public boolean isStrictInventoryLock() { return strictInventoryLock; }
    public boolean isBlockShiftClick() { return blockShiftClick; }
    public boolean isBlockNumberKey() { return blockNumberKey; }
    public boolean isBlockCollectToCursor() { return blockCollectToCursor; }
    public boolean isBlockDrag() { return blockDrag; }
    public boolean isBlockCreativeActions() { return blockCreativeActions; }
    public boolean isClaimLockEnabled() { return claimLockEnabled; }
}
