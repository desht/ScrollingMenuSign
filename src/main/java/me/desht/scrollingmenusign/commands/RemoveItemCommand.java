package me.desht.scrollingmenusign.commands;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.scrollingmenusign.util.MiscUtil;

import org.bukkit.entity.Player;

public class RemoveItemCommand extends AbstractCommand {

	public RemoveItemCommand() {
		super("sms rem", 2, 2);
		setPermissionNode("scrollingmenusign.commands.remove");
		setUsage("/sms remove <menu-name> <item-index|item-label>");
		setQuotedArgs(true);
	}

	@Override
	public boolean execute(ScrollingMenuSign plugin, Player player, String[] args) throws SMSException {

		String menuName = args[0];
		String item = args[1];

		try {
			SMSMenu menu = plugin.getHandler().getMenu(menuName);
			menu.removeItem(item);
			menu.notifyObservers(SMSMenuAction.REPAINT);
			MiscUtil.statusMessage(player, "Menu entry &f#" + item + "&- removed from &e" + menuName);
		} catch (IndexOutOfBoundsException e) {
			MiscUtil.errorMessage(player, "Item index " + item + " out of range");
		} catch (IllegalArgumentException e) {
			MiscUtil.errorMessage(player, e.getMessage());
		}
		
		return true;
	}

}
