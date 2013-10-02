package me.desht.scrollingmenusign.commands;

import java.util.Arrays;
import java.util.List;

import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSPersistence;
import me.desht.scrollingmenusign.ScrollingMenuSign;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class ReloadCommand extends SMSAbstractCommand {

	public ReloadCommand() {
		super("sms reload");
		setPermissionNode("scrollingmenusign.commands.reload");
		setUsage("/sms reload [menus] [macros] [config]");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {

		boolean loadMenus = false;
		boolean loadViews = false;
		boolean loadMacros = false;
		boolean loadConfig = false;
		boolean loadAll = false;
		boolean loadVariables = false;
		boolean loadFonts = false;

		if (args.length == 0) {
			loadAll = true;
		} else {
            for (String arg : args) {
                if (arg.startsWith("me")) {
                    loadMenus = true;
                } else if (arg.startsWith("ma")) {
                    loadMacros = true;
                } else if (arg.startsWith("c")) {
                    loadConfig = true;
                } else if (arg.startsWith("va")) {
                    loadVariables = true;
                } else if (arg.startsWith("vi")) {
                    loadViews = true;
                } else if (arg.startsWith("fo")) {
                    loadFonts = true;
                }
            }
		}
		if (loadAll || loadConfig) {
			plugin.reloadConfig();
			SMSMenu.updateAllMenus();
		}
		if (loadAll || loadMenus) {
			SMSPersistence.loadMenus();
			SMSPersistence.loadViews();
		}
		if (loadViews && !loadMenus) {
			SMSPersistence.loadViews();
		}
		if (loadAll || loadMacros) {
			SMSPersistence.loadMacros();
		}
		if (loadAll || loadVariables) {
			SMSPersistence.loadVariables();
		}
		if (loadAll || loadFonts) {
			ScrollingMenuSign.getInstance().setupCustomFonts();
		}

		MiscUtil.statusMessage(sender, "Reload complete.");

		return true;
	}

	@Override
	public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
		if (args.length >= 1) {
			List<String> opts = Arrays.asList("menus", "macros", "config", "views", "vars", "fonts");
			return filterPrefix(sender, opts, args[args.length - 1]);
		} else {
			showUsage(sender);
			return noCompletions(sender);
		}
	}
}
