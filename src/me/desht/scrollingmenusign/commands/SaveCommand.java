package me.desht.scrollingmenusign.commands;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSPersistence;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.util.MiscUtil;
import me.desht.util.PermissionsUtils;

import org.bukkit.entity.Player;

public class SaveCommand extends AbstractCommand {

	public SaveCommand() {
		super("sms sa");
		setPermissionNode("scrollingmenusign.commands.save");
		setUsage("/sms save [menus] [macros]");
	}

	@Override
	public boolean execute(ScrollingMenuSign plugin, Player player, String[] args) throws SMSException {
		PermissionsUtils.requirePerms(player, "scrollingmenusign.commands.save");
		
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
			SMSPersistence.saveMenusAndViews();
		if (saveAll || saveMacros)
			SMSPersistence.saveMacros();
		
		if (player != null)
			MiscUtil.statusMessage(player, "Save complete.");

		return true;
	}

}
