package me.desht.scrollingmenusign.commands;

import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.enums.SMSMenuAction;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class RemoveItemCommand extends SMSAbstractCommand {

	public RemoveItemCommand() {
		super("sms remove", 2, 2);
		setPermissionNode("scrollingmenusign.commands.remove");
		setUsage(new String[] {
				"/sms remove <menu-name> @<pos>",
				"/sms remove <menu-name> <item-label>"
		});
		setQuotedArgs(true);
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {

		String menuName = args[0];
		String item = args[1];

		if (item.matches("@[0-9]+")) {
			// backwards compatibility - numeric indices should be prefixed with a '@'
			// but we'll allow raw numbers to be used 
			item = item.substring(1);
		}

		try {
			SMSMenu menu = SMSMenu.getMenu(menuName);
			menu.removeItem(item);
			menu.notifyObservers(SMSMenuAction.REPAINT);
			MiscUtil.statusMessage(sender, "Menu entry &f#" + item + "&- removed from &e" + menuName);
		} catch (IndexOutOfBoundsException e) {
			MiscUtil.errorMessage(sender, "Item index " + item + " out of range");
		} catch (IllegalArgumentException e) {
			MiscUtil.errorMessage(sender, e.getMessage());
		}

		return true;
	}

}
