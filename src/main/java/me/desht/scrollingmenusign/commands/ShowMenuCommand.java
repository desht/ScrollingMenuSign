package me.desht.scrollingmenusign.commands;

import java.util.List;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSHandler;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSMenuItem;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.util.MessagePager;
import me.desht.scrollingmenusign.util.PermissionsUtils;
import me.desht.scrollingmenusign.views.SMSMapView;
import me.desht.scrollingmenusign.views.SMSView;

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
		SMSView view = null;
		if (args.length > 0) {
			menu = handler.getMenu(args[0]);
		} else {
			notFromConsole(player);
			view = SMSView.getViewForLocation(player.getTargetBlock(null, 3).getLocation());
			if (view == null) {
				if (player.getItemInHand().getTypeId() == 358) {		// map
					PermissionsUtils.requirePerms(player, "scrollingmenusign.maps");
					short mapId = player.getItemInHand().getDurability();
					view = SMSMapView.getViewForId(mapId);
				}
			}
			if (view == null) {
				throw new SMSException("You are not looking at a menu view.");
			}
			menu = view.getMenu();
		}
		
		MessagePager pager = MessagePager.getPager(player).clear();
		String mo = menu.getOwner().isEmpty() ? "(no one)" : menu.getOwner();
		pager.add(String.format("Menu &e%s&-, Title \"&f%s&-\", Owner &e%s&-",
		                                       menu.getName(),  menu.getTitle(), mo));
		if (!menu.formatUses(player).isEmpty()) {
			pager.add("&c" + menu.formatUses(player));
		}
		if (!menu.getDefaultCommand().isEmpty()) {
			pager.add(" Default command: &f" + menu.getDefaultCommand());
		}
		if (view != null) {
			String owner = view.getAttributeAsString("owner");
			pager.add(String.format("View &e%s&-, Owner &e%s&-",
			                                       view.getName(), owner.isEmpty() ? "(no one)" : owner));
		}
		
		List<SMSMenuItem> items = menu.getItems();
		int n = 1;
		for (SMSMenuItem item : items) {
			String s = String.format("&e%2d) &f%s " + "&f[%s] \"%s\"&f &c%s",
					n, item.getLabel(), item.getCommand(), item.getMessage(), item.formatUses(player));
			n++;
			pager.add(s);
		}
		
		pager.showPage();
		
		return true;
	}

}
