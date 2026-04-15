package io.josemmo.bukkit.plugin.addon.imgui.util;

import org.bukkit.ChatColor;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class Texts {
    private Texts() {
    }

    public static String colorize(String value) {
        return ChatColor.translateAlternateColorCodes('&', value == null ? "" : value);
    }

    public static String applyPlaceholders(String template, Map<String, String> placeholders) {
        String value = template == null ? "" : template;
        if (placeholders == null || placeholders.isEmpty()) {
            return colorize(value);
        }
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            value = value.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return colorize(value);
    }

    public static List<String> colorizeAll(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return Collections.emptyList();
        }
        for (int i = 0; i < lines.size(); i++) {
            lines.set(i, colorize(lines.get(i)));
        }
        return lines;
    }

    public static String getBasename(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        String normalized = stripFileExtension(path.replace('\\', '/'));
        int lastSlash = normalized.lastIndexOf('/');
        if (lastSlash < 0 || lastSlash == normalized.length() - 1) {
            return normalized;
        }
        return normalized.substring(lastSlash + 1);
    }

    public static String getPathWithoutExtension(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        return stripFileExtension(path.replace('\\', '/'));
    }

    private static String stripFileExtension(String path) {
        int lastSlash = path.lastIndexOf('/');
        int lastDot = path.lastIndexOf('.');
        if (lastDot <= lastSlash) {
            return path;
        }
        return path.substring(0, lastDot);
    }
}
