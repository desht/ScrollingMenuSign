package me.desht.scrollingmenusign.commands;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.util.MiscUtil;
import me.desht.util.PermissionsUtils;

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
		
		Boolean loadMenus = false;
		Boolean loadMacros = false;
		Boolean loadConfig = false;
		Boolean loadAll = false;
		if (args.length == 0) {
			loadAll = true;
		} else {
			for (int i = 1 ; 0 < args.length; i++) {
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
				plugin.getConfiguration().load();
				SMSMenu.updateAllMenus();
		}
		if (loadAll || loadMenus)
			plugin.loadMenus();
		if (loadAll || loadMacros)
			plugin.loadMacros();
		
		MiscUtil.statusMessage(player, "Reload complete.");
		
		return true;
	}
}
