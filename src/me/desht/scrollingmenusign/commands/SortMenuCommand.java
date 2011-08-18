package me.desht.scrollingmenusign.commands;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSHandler;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSPermissions;
import me.desht.scrollingmenusign.SMSUtils;
import me.desht.scrollingmenusign.ScrollingMenuSign;

import org.bukkit.entity.Player;

public class SortMenuCommand extends AbstractCommand {

	public SortMenuCommand() {
		super("sms so", 0, 2);
		setPermissionNode("scrollingmenusign.commands.sort");
		setUsage("sms sort [<menu-name>] [<auto>]");
	}

	@Override
	public boolean execute(ScrollingMenuSign plugin, Player player, String[] args) throws SMSException {
		SMSPermissions.requirePerms(player, "scrollingmenusign.commands.sort");
		
		SMSHandler handler = plugin.getHandler();
		SMSMenu menu = null;
		if (args.length > 0) {
			menu = handler.getMenu(args[0]);
		} else {
			notFromConsole(player);
			menu = handler.getMenu(SMSMenu.getTargetedMenuSign(player, true));
		}
		
		if (partialMatch(args, 1, "a")) {	// autosort
			menu.setAutosort(true);
			menu.sortItems();
			SMSUtils.statusMessage(player, "Menu &e" + menu.getName() + "&- has been sorted (autosort enabled)");
		} else {
			menu.setAutosort(false);
			menu.sortItems();
			SMSUtils.statusMessage(player, "Menu &e" + menu.getName() + "&- has been sorted (autosort disabled)");
		}
		menu.updateSigns();
		
		return true;
	}
}
