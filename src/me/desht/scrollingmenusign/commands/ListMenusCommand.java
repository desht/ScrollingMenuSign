package me.desht.scrollingmenusign.commands;

import java.util.ArrayList;
import java.util.List;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSHandler;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.views.SMSView;
import me.desht.util.MessagePager;
import me.desht.util.MiscUtil;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class ListMenusCommand extends AbstractCommand {

	public ListMenusCommand() {
		super("sms l", 0, 1);
		setPermissionNode("scrollingmenusign.commands.list");
		setUsage("/sms list [<menu-name>]");
	}

	@Override
	public boolean execute(ScrollingMenuSign plugin, Player player, String[] args) throws SMSException {		
		
		MessagePager.clear(player);
		SMSHandler handler = plugin.getHandler();
		if (args.length > 0) {
			SMSMenu menu = handler.getMenu(args[0]);
			listMenu(player, menu, true);
		} else {
			List<SMSMenu> menus = handler.listMenus(true);
			if (menus.size() == 0) {
				MiscUtil.statusMessage(player, "No menu signs exist.");
			} else {
				for (SMSMenu menu : menus) {
					listMenu(player, menu, false);
				}
			}
		}
		MessagePager.showPage(player);
		
		return true;
	}

	private void listMenu(Player player, SMSMenu menu, boolean listViews) {
		List<SMSView> views = SMSView.getViewsForMenu(menu, true);
		
		ChatColor viewCol = views.size() > 0 ? ChatColor.YELLOW : ChatColor.RED;
		String message = String.format("&e%s &2\"%s&2\" &e[%d items] %s[%d views]",
		                               menu.getName(), menu.getTitle(), menu.getItemCount(),
		                               viewCol.toString(), views.size());
		List<String> lines = new ArrayList<String>();
		lines.add(message);
		if (listViews) {
			for (SMSView v : views) {
				lines.add(" &5*&- " + v.getName() + ": " + v.toString());
			}
		}
		MessagePager.add(player, lines);
	}
}
