package me.desht.scrollingmenusign.commands;

import java.util.List;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSHandler;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSMenuItem;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.views.SMSView;
import me.desht.util.MessagePager;

import org.bukkit.entity.Player;

public class ShowMenuCommand extends AbstractCommand {

	public ShowMenuCommand() {
		super("sms sh", 0, 1);
		setPermissionNode("scrollingmenusign.commands.show");
		setUsage("/sms show <menu-name>");
	}

	@Override
	public boolean execute(ScrollingMenuSign plugin, Player player, String[] args) throws SMSException {	
		SMSHandler handler = plugin.getHandler();
		
		SMSMenu menu = null;
		if (args.length > 0) {
			menu = handler.getMenu(args[0]);
		} else {
			notFromConsole(player);
			SMSView v = SMSView.getViewForLocation(player.getTargetBlock(null, 3).getLocation());
			if (v == null)
				throw new SMSException("You are not looking at a menu view.");
			menu = v.getMenu();
		}
		
		MessagePager.clear(player);
		MessagePager.add(player, String.format("Menu &e%s&-: title &f%s&- &c%s",
				menu.getName(),  menu.getTitle(),  menu.formatUses(player)));
		if (!menu.getDefaultCommand().isEmpty()) {
			MessagePager.add(player, " Default command: &f" + menu.getDefaultCommand());
		}
		
		List<SMSMenuItem> items = menu.getItems();
		int n = 1;
		for (SMSMenuItem item : items) {
			String s = String.format("&e%2d) &f%s " + "&f[%s] \"%s\"&f &c%s",
					n, item.getLabel(), item.getCommand(), item.getMessage(), item.formatUses(player));
			n++;
			MessagePager.add(player, s);
		}
		
		MessagePager.showPage(player);
		
		return true;
	}

}
