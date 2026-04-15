package io.josemmo.bukkit.plugin.addon.imgui.command;

import io.josemmo.bukkit.plugin.addon.imgui.ImguiAddonPlugin;
import io.josemmo.bukkit.plugin.addon.imgui.config.AddonSettings;
import io.josemmo.bukkit.plugin.addon.imgui.gui.GuiService;
import io.josemmo.bukkit.plugin.addon.imgui.i18n.LocaleService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ImguiCommand implements CommandExecutor, TabCompleter {
    private final ImguiAddonPlugin plugin;
    private final GuiService guiService;
    private final LocaleService localeService;

    public ImguiCommand(ImguiAddonPlugin plugin, GuiService guiService, LocaleService localeService) {
        this.plugin = plugin;
        this.guiService = guiService;
        this.localeService = localeService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        AddonSettings settings = guiService.getSettings();
        if (settings == null) {
            return true;
        }

        if (args.length > 0 && "reload".equalsIgnoreCase(args[0])) {
            if (!settings.isReloadSubcommandEnabled()) {
                return true;
            }
            if (!sender.hasPermission(settings.getReloadPermission())) {
                sender.sendMessage(localeService.tr(sender, "no-permission"));
                return true;
            }
            plugin.requestReload(sender, false);
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(localeService.tr(sender, "players-only"));
            return true;
        }

        if (!sender.hasPermission(settings.getUsePermission())) {
            sender.sendMessage(localeService.tr(sender, "no-permission"));
            return true;
        }

        Player player = (Player) sender;
        player.sendMessage(localeService.tr(player, "loading"));
        guiService.openMenu(player, 1);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        AddonSettings settings = guiService.getSettings();
        if (settings == null || !settings.isReloadSubcommandEnabled()) {
            return Collections.emptyList();
        }
        if (!sender.hasPermission(settings.getReloadPermission())) {
            return Collections.emptyList();
        }
        if (args.length == 1 && "reload".startsWith(args[0].toLowerCase())) {
            List<String> values = new ArrayList<String>();
            values.add("reload");
            return values;
        }
        return Collections.emptyList();
    }
}
