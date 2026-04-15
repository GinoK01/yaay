package io.josemmo.bukkit.plugin.addon.imgui;

import io.josemmo.bukkit.plugin.YamipaPlugin;
import io.josemmo.bukkit.plugin.addon.imgui.api.ImguiLanguageService;
import io.josemmo.bukkit.plugin.addon.imgui.api.ImguiLanguageServiceImpl;
import io.josemmo.bukkit.plugin.addon.imgui.command.ImguiCommand;
import io.josemmo.bukkit.plugin.addon.imgui.config.AddonSettings;
import io.josemmo.bukkit.plugin.addon.imgui.config.ConfigManager;
import io.josemmo.bukkit.plugin.addon.imgui.display.DisplayMetadataService;
import io.josemmo.bukkit.plugin.addon.imgui.gui.GuiService;
import io.josemmo.bukkit.plugin.addon.imgui.i18n.LocaleService;
import io.josemmo.bukkit.plugin.addon.imgui.limits.HourlyLimitService;
import io.josemmo.bukkit.plugin.addon.imgui.security.DroppedImageItemMetadataListener;
import io.josemmo.bukkit.plugin.addon.imgui.security.InventoryProtectionListener;
import io.josemmo.bukkit.plugin.addon.imgui.watcher.AddonConfigWatcher;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import java.nio.file.Path;

public class ImguiAddonPlugin extends JavaPlugin {
    private ConfigManager configManager;
    private LocaleService localeService;
    private DisplayMetadataService displayMetadataService;
    private HourlyLimitService hourlyLimitService;
    private GuiService guiService;
    private ImguiLanguageService languageService;
    private AddonConfigWatcher configWatcher;
    private volatile AddonSettings settings;
    private volatile long lastAutoReloadAtMs;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        configManager.ensureDefaults();

        localeService = new LocaleService(this);
        languageService = new ImguiLanguageServiceImpl(localeService);
        displayMetadataService = new DisplayMetadataService(getDataFolder(), localeService);
        hourlyLimitService = new HourlyLimitService();
        guiService = new GuiService(this, YamipaPlugin.getInstance(), localeService, displayMetadataService, hourlyLimitService);

        boolean loaded = reloadInternal(null, false, true);
        if (!loaded) {
            getLogger().severe("Failed to load addon configuration");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getServicesManager().register(ImguiLanguageService.class, languageService, this, ServicePriority.Normal);

        InventoryProtectionListener inventoryProtectionListener = new InventoryProtectionListener(this, guiService);
        getServer().getPluginManager().registerEvents(inventoryProtectionListener, this);
        getServer().getPluginManager().registerEvents(
            new DroppedImageItemMetadataListener(guiService, localeService, displayMetadataService),
            this
        );

        ImguiCommand imguiCommand = new ImguiCommand(this, guiService, localeService);
        PluginCommand command = getCommand("imgui");
        if (command != null) {
            command.setExecutor(imguiCommand);
            command.setTabCompleter(imguiCommand);
        }

        reconfigureWatcher();
        getLogger().info("YAAY enabled");
    }

    @Override
    public void onDisable() {
        if (configWatcher != null) {
            configWatcher.stopWatching();
            configWatcher = null;
        }
        if (guiService != null) {
            guiService.closeAllMenus();
        }
        if (languageService != null) {
            getServer().getServicesManager().unregister(ImguiLanguageService.class, languageService);
            languageService = null;
        }
    }

    public AddonSettings getSettings() {
        return settings;
    }

    public void requestReload(final CommandSender initiator, final boolean auto) {
        YamipaPlugin.getInstance().getScheduler().runInGame(new Runnable() {
            @Override
            public void run() {
                reloadInternal(initiator, auto, false);
            }
        }, 0);
    }

    public void onWatchedConfigChange(Path path) {
        AddonSettings localSettings = settings;
        if (localSettings == null || !localSettings.isAutoReloadEnabled()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastAutoReloadAtMs < localSettings.getAutoReloadDebounceMs()) {
            return;
        }
        lastAutoReloadAtMs = now;

        requestReload(null, true);
    }

    private synchronized boolean reloadInternal(CommandSender initiator, boolean auto, boolean startup) {
        try {
            AddonSettings newSettings = configManager.loadSettings();
            localeService.reload(newSettings);
            displayMetadataService.reload();
            settings = newSettings;
            guiService.applySettings(newSettings);
            reconfigureWatcher();

            if (!startup) {
                if (initiator != null) {
                    initiator.sendMessage(localeService.tr(initiator, "reloaded"));
                } else if (auto) {
                    getLogger().info("Configuration auto-reloaded");
                }
            }
            return true;
        } catch (Exception e) {
            getLogger().severe("Failed to reload addon configuration: " + e.getMessage());
            if (initiator != null) {
                initiator.sendMessage("[Imgui] Failed to reload configuration");
            }
            return false;
        }
    }

    private void reconfigureWatcher() {
        AddonSettings localSettings = settings;

        if (configWatcher != null) {
            configWatcher.stopWatching();
            configWatcher = null;
        }

        if (localSettings == null || !localSettings.isAutoReloadEnabled()) {
            return;
        }

        configWatcher = new AddonConfigWatcher(this, getDataFolder().toPath());
        try {
            configWatcher.startWatching();
        } catch (RuntimeException ex) {
            getLogger().warning("Failed to start config watcher: " + ex.getMessage());
            configWatcher = null;
        }
    }
}
