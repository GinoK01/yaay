package io.josemmo.bukkit.plugin.addon.imgui.editor;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public final class EditorSessionStore {
    private final File file;
    private final Logger logger;
    private final Object lock = new Object();

    public EditorSessionStore(File dataFolder, Logger logger) {
        this.file = new File(dataFolder, "editor-sessions.yml");
        this.logger = logger;
    }

    public boolean save(UUID playerId, ItemStack item) {
        if (playerId == null || item == null) {
            return false;
        }
        synchronized (lock) {
            YamlConfiguration config = load();
            config.set(path(playerId), item.clone());
            return write(config);
        }
    }

    public ItemStack peek(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        synchronized (lock) {
            ItemStack item = load().getItemStack(path(playerId));
            return item == null ? null : item.clone();
        }
    }

    public void delete(UUID playerId) {
        if (playerId == null) {
            return;
        }
        synchronized (lock) {
            YamlConfiguration config = load();
            if (!config.contains(path(playerId))) {
                return;
            }
            config.set(path(playerId), null);
            write(config);
        }
    }

    public Map<UUID, ItemStack> all() {
        synchronized (lock) {
            ConfigurationSection section = load().getConfigurationSection("sessions");
            if (section == null) {
                return Collections.emptyMap();
            }
            Map<UUID, ItemStack> items = new HashMap<UUID, ItemStack>();
            for (String key : section.getKeys(false)) {
                try {
                    UUID playerId = UUID.fromString(key);
                    ItemStack item = section.getItemStack(key);
                    if (item != null) {
                        items.put(playerId, item.clone());
                    }
                } catch (IllegalArgumentException ex) {
                    logger.warning("Ignoring invalid editor session id " + key);
                }
            }
            return items;
        }
    }

    private YamlConfiguration load() {
        if (!file.exists()) {
            return new YamlConfiguration();
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    private boolean write(YamlConfiguration config) {
        try {
            config.save(file);
            return true;
        } catch (IOException ex) {
            logger.warning("Failed to save editor sessions: " + ex.getMessage());
            return false;
        }
    }

    private String path(UUID playerId) {
        return "sessions." + playerId;
    }
}
