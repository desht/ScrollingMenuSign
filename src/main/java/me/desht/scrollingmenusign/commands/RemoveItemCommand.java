package me.desht.scrollingmenusign.commands;

import java.util.List;

import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.SMSException;
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
			SMSMenu menu = getMenu(sender, menuName);
			menu.ensureAllowedToModify(sender);
			menu.removeItem(item);
			menu.notifyObservers(SMSMenuAction.REPAINT);
			MiscUtil.statusMessage(sender, "Menu entry &f#" + item + "&- removed from &e" + menu.getName());
		} catch (IndexOutOfBoundsException e) {
			throw new SMSException("Item index " + item + " out of range");
		} catch (IllegalArgumentException e) {
			throw new SMSException(e.getMessage());
		}

		return true;
	}

	@Override
	public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
		switch (args.length) {
		case 1:
			return getMenuCompletions(plugin, sender, args[0]);
		case 2:
			SMSMenu menu = SMSMenu.getMenu(args[0]);
			return getMenuItemCompletions(sender, menu, args[1]);
		default:
			showUsage(sender);
			return noCompletions(sender);
		}
	}
}
