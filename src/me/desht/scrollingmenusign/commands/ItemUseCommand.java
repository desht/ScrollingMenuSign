package me.desht.scrollingmenusign.commands;

import java.util.ArrayList;
import java.util.Arrays;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSMenuItem;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.util.MiscUtil;

import org.bukkit.entity.Player;

public class ItemUseCommand extends AbstractCommand {

	public ItemUseCommand() {
		super("sms u", 2, 4);
		setPermissionNode("scrollingmenusign.commands.uses");
		setUsage(new String[] {
				"/sms uses <menu> <item> <count> [global]",
				"/sms uses <menu> <item> clear",
				"/sms uses <menu> <count> [global]",
				"/sms uses <menu> clear",
		});
	}

	@Override
	public boolean execute(ScrollingMenuSign plugin, Player player, String[] args) throws SMSException {		
		ArrayList<String> a = new ArrayList<String>(Arrays.asList(args));
		
		boolean isGlobal = false;
		boolean isClearing = false;
		
		if (partialMatch(a.get(a.size() - 1), "g")) {
			isGlobal = true;
			a.remove(a.size() - 1);
		}
		if (partialMatch(a.get(a.size() - 1), "c")) {
			isClearing = true;
		}
		
		SMSMenu menu = plugin.getHandler().getMenu(a.get(0));
		
		if (a.size() == 3) {
			// dealing with an item
			int idx = menu.indexOfItem(a.get(1));
			if (idx <= 0) {
				throw new SMSException("Unknown menu item: " + a.get(1));
			}
			SMSMenuItem item = menu.getItem(idx);
			if (isClearing) {
				item.getUseLimits().clearUses();
				MiscUtil.statusMessage(player, "Unset all usage limits for item &e" + item.getLabel());
			} else {
				int count = parseNumber(a.get(2));
				if (isGlobal) {
					item.getUseLimits().setGlobalUses(count);
					MiscUtil.statusMessage(player, "Set GLOBAL use limit for item &e" + item.getLabel()
							+ "&- to " + count + ".");
				} else {
					item.getUseLimits().setUses(count);
					MiscUtil.statusMessage(player, "Set PER-PLAYER use limit for item &e" + item.getLabel()
							+ "&- to " + count + ".");
				}
			}
		} else if (a.size() == 2) {
			// dealing with a menu
			if (isClearing) {
				menu.getUseLimits().clearUses();
				MiscUtil.statusMessage(player, "Unset all usage limits for menu &e" + menu.getName());
			} else {
				int count = parseNumber(a.get(1));
				if (isGlobal) {
					menu.getUseLimits().setGlobalUses(count);
					MiscUtil.statusMessage(player, "Set GLOBAL use limit for menu &e" + menu.getName()
							+ "&- to " + count + ".");
				} else {
					menu.getUseLimits().setUses(count);
					MiscUtil.statusMessage(player, "Set PER-PLAYER use limit for menu &e" + menu.getName()
							+ "&- to " + count + ".");
				}
			}
		}
		
		return true;
	}

	private static int parseNumber(String s) throws SMSException {
		int count;
		try {
			count = Integer.parseInt(s);
		} catch (NumberFormatException e) {
			throw new SMSException("Invalid numeric argument: " + s);
		}
		return count;
	}
}
