package me.desht.scrollingmenusign.commands;

import java.util.List;

import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSMenuItem;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class ItemUseCommand extends SMSAbstractCommand {

	public ItemUseCommand() {
		super("sms uses", 2, 4);
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
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		SMSMenu menu = SMSMenu.getMenu(args[0]);
		menu.ensureAllowedToModify(sender);

		boolean isGlobal = getBooleanOption("global") || getBooleanOption("g");
		boolean isClearing = args[args.length - 1].startsWith("c");

		if (args.length == 3) {
			// dealing with an item
			int idx = menu.indexOfItem(args[1]);
			SMSMenuItem item = menu.getItemAt(idx, true);
			if (isClearing) {
				item.getUseLimits().clearUses();
				MiscUtil.statusMessage(sender, "Unset all usage limits for item &e" + item.getLabel());
			} else {
				int count = parseNumber(args[2]);
				if (isGlobal) {
					item.getUseLimits().setGlobalUses(count);
					MiscUtil.statusMessage(sender, "Set GLOBAL use limit for item &e" + item.getLabel()
					                       + "&- to " + count + ".");
				} else {
					item.getUseLimits().setUses(count);
					MiscUtil.statusMessage(sender, "Set PER-PLAYER use limit for item &e" + item.getLabel()
					                       + "&- to " + count + ".");
				}
			}
		} else if (args.length == 2) {
			// dealing with a menu
			if (isClearing) {
				menu.getUseLimits().clearUses();
				MiscUtil.statusMessage(sender, "Unset all usage limits for menu &e" + menu.getName());
			} else {
				int count = parseNumber(args[1]);
				if (isGlobal) {
					menu.getUseLimits().setGlobalUses(count);
					MiscUtil.statusMessage(sender, "Set GLOBAL use limit for menu &e" + menu.getName()
					                       + "&- to " + count + ".");
				} else {
					menu.getUseLimits().setUses(count);
					MiscUtil.statusMessage(sender, "Set PER-PLAYER use limit for menu &e" + menu.getName()
					                       + "&- to " + count + ".");
				}
			}
		}

		return true;
	}

	@Override
	public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
		switch (args.length) {
		case 1:
			return getMenuCompletions(plugin, sender, args[0]);
		default:
			showUsage(sender);
			return noCompletions(sender);
		}
	}
}
