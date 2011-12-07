package me.desht.scrollingmenusign.commands;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSPersistence;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.util.MiscUtil;
import me.desht.scrollingmenusign.util.PermissionsUtils;

import org.bukkit.entity.Player;

public class ReloadCommand extends AbstractCommand {

	public ReloadCommand() {
		super("sms rel");
		setPermissionNode("scrollingmenusign.commands.reload");
		setUsage("/sms reload [menus] [macros] [config]");
	}

	@Override
	public boolean execute(ScrollingMenuSign plugin, Player player, String[] args) throws SMSException {
		PermissionsUtils.requirePerms(player, "scrollingmenusign.commands.reload");

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

		if (player != null)
			MiscUtil.statusMessage(player, "Reload complete.");

		return true;
	}
}
