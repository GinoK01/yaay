package io.josemmo.bukkit.plugin.addon.imgui.i18n;

import io.josemmo.bukkit.plugin.addon.imgui.ImguiAddonPlugin;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LanguagePreferenceStore {
    private final ImguiAddonPlugin plugin;
    private final File databaseFile;
    private final Map<UUID, String> cache = new ConcurrentHashMap<UUID, String>();
    private volatile boolean available;

    public LanguagePreferenceStore(ImguiAddonPlugin plugin) {
        this.plugin = plugin;
        this.databaseFile = new File(plugin.getDataFolder(), "language-preferences.db");
        this.available = initialize();
    }

    public boolean isAvailable() {
        return available;
    }

    public String getLanguage(UUID playerId) {
        if (playerId == null) {
            return null;
        }

        String cached = cache.get(playerId);
        if (cached != null) {
            return cached;
        }

        if (!available) {
            return null;
        }

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT language_code FROM player_language_preferences WHERE player_uuid = ?"
             )) {
            statement.setString(1, playerId.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                String language = result.getString("language_code");
                if (language != null && !language.trim().isEmpty()) {
                    cache.put(playerId, language);
                }
                return language;
            }
        } catch (SQLException ex) {
            plugin.getLogger().warning("Failed to load language preference for " + playerId + ": " + ex.getMessage());
            return null;
        }
    }

    public boolean setLanguage(UUID playerId, String language) {
        if (playerId == null || language == null || language.trim().isEmpty()) {
            return false;
        }

        String normalizedLanguage = language.trim().toLowerCase(Locale.ROOT);
        cache.put(playerId, normalizedLanguage);
        if (!available) {
            return true;
        }

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "INSERT INTO player_language_preferences (player_uuid, language_code, updated_at) VALUES (?, ?, ?) " +
                 "ON CONFLICT(player_uuid) DO UPDATE SET language_code=excluded.language_code, updated_at=excluded.updated_at"
             )) {
            statement.setString(1, playerId.toString());
            statement.setString(2, normalizedLanguage);
            statement.setLong(3, System.currentTimeMillis());
            statement.executeUpdate();
            return true;
        } catch (SQLException ex) {
            plugin.getLogger().warning("Failed to store language preference for " + playerId + ": " + ex.getMessage());
            return false;
        }
    }

    public boolean clearLanguage(UUID playerId) {
        if (playerId == null) {
            return false;
        }

        cache.remove(playerId);
        if (!available) {
            return true;
        }

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "DELETE FROM player_language_preferences WHERE player_uuid = ?"
             )) {
            statement.setString(1, playerId.toString());
            statement.executeUpdate();
            return true;
        } catch (SQLException ex) {
            plugin.getLogger().warning("Failed to clear language preference for " + playerId + ": " + ex.getMessage());
            return false;
        }
    }

    private boolean initialize() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ex) {
            plugin.getLogger().warning("SQLite driver not found, language preference persistence disabled");
            return false;
        }

        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                "CREATE TABLE IF NOT EXISTS player_language_preferences (" +
                "player_uuid TEXT PRIMARY KEY, " +
                "language_code TEXT NOT NULL, " +
                "updated_at INTEGER NOT NULL)"
            );
            return true;
        } catch (SQLException ex) {
            plugin.getLogger().warning("Failed to initialize language preferences database: " + ex.getMessage());
            return false;
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
    }
}
