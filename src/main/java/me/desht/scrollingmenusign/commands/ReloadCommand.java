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
		boolean loadViews = false;
		boolean loadMacros = false;
		boolean loadConfig = false;
		boolean loadAll = false;
		boolean loadVariables = false;
		
		if (args.length == 0) {
			loadAll = true;
		} else {
			for (int i = 0 ; i < args.length; i++) {
				if (args[i].startsWith("me")) {
					loadMenus = true;
				} else if (args[i].startsWith("ma")) {
					loadMacros = true;
				} else if (args[i].startsWith("c")) {
					loadConfig = true;
				} else if (args[i].startsWith("va")) {
					loadVariables = true;
				} else if (args[i].startsWith("vi")) {
					loadViews = true;
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
		if (loadAll || loadViews) {
			SMSPersistence.loadViews();
		}
		if (loadAll || loadMacros) {
			SMSPersistence.loadMacros();
		}
		if (loadAll || loadVariables) {
			SMSPersistence.loadVariables();
		}

		MiscUtil.statusMessage(sender, "Reload complete.");

		return true;
	}
}
