package me.desht.scrollingmenusign.commands;

import me.desht.scrollingmenusign.MenuRemovalAction;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSHandler;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSUtils;
import me.desht.scrollingmenusign.ScrollingMenuSign;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class RemoveViewCommand extends AbstractCommand {

	public RemoveViewCommand() {
		super("sms b", 0, 1);
		setPermissionNode("scrollingmenusign.commands.break");
		setUsage("/sms break [<loc>]");
	}

	@Override
	public boolean execute(ScrollingMenuSign plugin, Player player, String[] args) throws SMSException {
		Location loc = null;
		if (args.length == 0) {
			notFromConsole(player);
			Block b = player.getTargetBlock(null, 3);
			loc = b.getLocation();
		} else {
			try {
				loc = SMSUtils.parseLocation(args[0], player);
			} catch (IllegalArgumentException e) {
				throw new SMSException(e.getMessage());
			}
		}
		
		SMSHandler handler = plugin.getHandler();
		String menuName = handler.getMenuNameAt(loc);
		if (menuName != null) {
			SMSMenu menu = handler.getMenu(menuName);
			menu.removeSign(loc, MenuRemovalAction.BLANK_SIGN);
			SMSUtils.statusMessage(player, "Sign @ &f" + SMSUtils.formatLocation(loc) +
			                       "&- was removed from menu &e" + menu.getName() + "&-.");	
		} else {
			throw new SMSException("You are not looking at a menu.");
		}
		
		return true;
	}
}
