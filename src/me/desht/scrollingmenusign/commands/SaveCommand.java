package me.desht.scrollingmenusign.commands;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSPermissions;
import me.desht.scrollingmenusign.SMSUtils;
import me.desht.scrollingmenusign.ScrollingMenuSign;

import org.bukkit.entity.Player;

public class SaveCommand extends AbstractCommand {

	public SaveCommand() {
		super("sms sa");
		setPermissionNode("scrollingmenusign.commands.save");
		setUsage("/sms save [menus] [macros]");
	}

	@Override
	public boolean execute(ScrollingMenuSign plugin, Player player, String[] args) throws SMSException {
		SMSPermissions.requirePerms(player, "scrollingmenusign.commands.save");
		
		Boolean saveMenus = false;
		Boolean saveMacros = false;
		Boolean saveAll = false;
		if (args.length == 0) {
			saveAll = true;
		} else {
			for (int i = 0 ; i < args.length; i++) {
				if (args[i].equalsIgnoreCase("menus")) {
					saveMenus = true;
				} else if (args[i].equalsIgnoreCase("macros")) {
					saveMacros = true;
				}
			}
		}
		if (saveAll || saveMenus)
			plugin.saveMenus();
		if (saveAll || saveMacros)
			plugin.saveMacros();
		
		SMSUtils.statusMessage(player, "Save complete.");

		return true;
	}

}
