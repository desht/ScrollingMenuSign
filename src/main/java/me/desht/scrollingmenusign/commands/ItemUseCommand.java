package me.desht.scrollingmenusign.commands;

import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.commands.AbstractCommand;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSMenuItem;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class ItemUseCommand extends AbstractCommand {

	public ItemUseCommand() {
		super("sms u", 2, 4);
		setPermissionNode("scrollingmenusign.commands.uses");
		setUsage(new String[] {
				"/sms uses <menu> <item> <count> [-global]",
				"/sms uses <menu> <item> clear",
				"/sms uses <menu> <count> [-global]",
				"/sms uses <menu> clear",
		});
		setQuotedArgs(true);
		setOptions(new String[] { "global", "g" });
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender player, String[] args) {
		SMSMenu menu = SMSMenu.getMenu(args[0]);
				
		boolean isGlobal = getBooleanOption("global") || getBooleanOption("g");
		boolean isClearing = args[args.length - 1].startsWith("c");

		if (args.length == 3) {
			// dealing with an item
			int idx = menu.indexOfItem(args[1]);
			SMSMenuItem item = menu.getItemAt(idx);
			if (item == null) {
				throw new SMSException("Unknown menu item: " + args[1]);
			}
			if (isClearing) {
				item.getUseLimits().clearUses();
				MiscUtil.statusMessage(player, "Unset all usage limits for item &e" + item.getLabel());
			} else {
				int count = parseNumber(args[2]);
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
		} else if (args.length == 2) {
			// dealing with a menu
			if (isClearing) {
				menu.getUseLimits().clearUses();
				MiscUtil.statusMessage(player, "Unset all usage limits for menu &e" + menu.getName());
			} else {
				int count = parseNumber(args[1]);
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
