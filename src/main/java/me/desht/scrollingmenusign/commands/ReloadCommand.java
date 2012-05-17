package me.desht.scrollingmenusign.commands;

import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.commands.AbstractCommand;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSPersistence;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class ReloadCommand extends AbstractCommand {

	public ReloadCommand() {
		super("sms rel");
		setPermissionNode("scrollingmenusign.commands.reload");
		setUsage("/sms reload [menus] [macros] [config]");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		
		boolean loadMenus = false;
		boolean loadMacros = false;
		boolean loadConfig = false;
		boolean loadAll = false;
		if (args.length == 0) {
			loadAll = true;
		} else {
			for (int i = 0 ; i < args.length; i++) {
				if (args[i].equalsIgnoreCase("menus")) {
					loadMenus = true;
				} else if (args[i].equalsIgnoreCase("macros")) {
					loadMacros = true;
				} else if (args[i].equalsIgnoreCase("config")) {
					loadConfig = true;
				}
			}
		}
		if (loadAll || loadConfig) {
			plugin.reloadConfig();
			SMSMenu.updateAllMenus();
		}
		if (loadAll || loadMenus) {
			SMSPersistence.loadMenusAndViews();
		}
		if (loadAll || loadMacros) {
			SMSPersistence.loadMacros();
		}

		if (sender != null)
			MiscUtil.statusMessage(sender, "Reload complete.");

		return true;
	}
}
