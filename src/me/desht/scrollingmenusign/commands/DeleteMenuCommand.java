package me.desht.scrollingmenusign.commands;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSHandler;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSUtils;
import me.desht.scrollingmenusign.ScrollingMenuSign;

import org.bukkit.entity.Player;

public class DeleteMenuCommand extends AbstractCommand {
	
	public DeleteMenuCommand() {
		super("sms d", 0, 1);
		setPermissionNode("scrollingmenusign.commands.delete");
		setUsage("/sms delete <menu>");
	}

	@Override
	public boolean execute(ScrollingMenuSign plugin, Player player, String[] args) throws SMSException {
		SMSHandler handler = plugin.getHandler();
		
		SMSMenu menu = null;
		if (args.length > 0) {
			menu = handler.getMenu(args[0]);
		} else {
			notFromConsole(player);
			menu = handler.getMenu(SMSMenu.getTargetedMenuSign(player, true));
		}
		handler.deleteMenu(menu.getName());
		SMSUtils.statusMessage(player, "Deleted menu &e" + menu.getName());

		return true;
	}

}
