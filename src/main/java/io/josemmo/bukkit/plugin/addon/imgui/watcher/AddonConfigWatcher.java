package io.josemmo.bukkit.plugin.addon.imgui.watcher;

import io.josemmo.bukkit.plugin.addon.imgui.ImguiAddonPlugin;
import io.josemmo.bukkit.plugin.storage.FileSystemWatcher;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

public class AddonConfigWatcher extends FileSystemWatcher {
    private final ImguiAddonPlugin plugin;

    public AddonConfigWatcher(ImguiAddonPlugin plugin, Path basePath) {
        super(basePath);
        this.plugin = plugin;
    }

    public void startWatching() {
        start();
    }

    public void stopWatching() {
        stop();
    }

    @Override
    protected void onFileCreated(@NotNull Path path) {
        notifyIfYaml(path);
    }

    @Override
    protected void onFileModified(@NotNull Path path) {
        notifyIfYaml(path);
    }

    @Override
    protected void onFileDeleted(@NotNull Path path) {
        notifyIfYaml(path);
    }

    private void notifyIfYaml(Path path) {
        String value = path.getFileName().toString().toLowerCase();
        if (value.endsWith(".yml")) {
            plugin.onWatchedConfigChange(path);
        }
    }
}
