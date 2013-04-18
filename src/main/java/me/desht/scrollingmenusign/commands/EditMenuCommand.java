package me.desht.scrollingmenusign.commands;

import java.util.List;

import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSMenuItem;
import me.desht.scrollingmenusign.SMSValidate;
import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.scrollingmenusign.parser.CommandParser;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class EditMenuCommand extends SMSAbstractCommand {

	public EditMenuCommand() {
		super("sms edit", 3);
		setPermissionNode("scrollingmenusign.commands.edit");
		setUsage(new String[] {
				"/sms edit <menu-name> @<pos> <replacements...>",
				"/sms edit <menu-name> <label> <replacements...>",
				"Replacement options (all take a string argument):",
				"  -label      The new item label",
				"  -command    The new command to run",
				"  -feedback   The new feedback message to display",
				"  -icon       The new material used for the item's icon",
				"  -lore       The new lore for the item (use '+text' to append)",
				"  -move       The new position in the menu for the item",
		});
		setQuotedArgs(true);
		setOptions(new String[] { "label:s", "command:s", "feedback:s", "icon:s", "move:i", "lore:s" });
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		SMSMenu menu = getMenu(sender, args[0]);
		menu.ensureAllowedToModify(sender);

		int pos = 0;
		if (args[1].startsWith("@")) {
			try {
				pos = Integer.parseInt(args[1].substring(1));
			} catch (NumberFormatException e) {
				throw new SMSException(e.getMessage() + " bad numeric index");
			}
		} else {
			pos = menu.indexOfItem(args[1]);
		}

		SMSMenuItem currentItem = menu.getItemAt(pos, true);
		String label   = hasOption("label") ? MiscUtil.parseColourSpec(getStringOption("label")) : currentItem.getLabel();
		String command = hasOption("command") ? getStringOption("command") : currentItem.getCommand();
		String message = hasOption("feedback") ? MiscUtil.parseColourSpec(getStringOption("feedback")) : currentItem.getMessage();
		String iconMat = hasOption("icon") ? getStringOption("icon") : currentItem.getIconMaterial().toString();
		List<String> lore = currentItem.getLoreAsList();
		if (hasOption("lore")) {
			String l = getStringOption("lore");
			String l1;
			if (l.startsWith("+") && l.length() > 1) {
				l1 = l.substring(1);
			} else {
				lore.clear();
				l1 = l;
			}
			if (!l1.isEmpty()) {
				for (String s : l1.split("\\\\\\\\")) {
					lore.add(s);
				}
			}
		}

		if (!command.isEmpty() && sender instanceof Player && !new CommandParser().verifyCreationPerms((Player) sender, command)) {
			throw new SMSException("You do not have permission to add that kind of command.");
		}

		SMSMenuItem newItem = new SMSMenuItem(menu, label, command, message, iconMat, lore.toArray(new String[lore.size()]));
		newItem.setUseLimits(currentItem.getUseLimits());

		if (hasOption("move")) {
			int newPos = getIntOption("move");
			SMSValidate.isTrue(newPos >= 1 && newPos <= menu.getItemCount(), "Invalid position for -move: " + newPos);
			menu.removeItem(pos);
			menu.insertItem(newPos, newItem);
			MiscUtil.statusMessage(sender, "Menu item &f" + label + "&- edited in &e" + menu.getName() + "&-, new position &e" + newPos);
		} else {
			menu.replaceItem(pos, newItem);
			MiscUtil.statusMessage(sender, "Menu item &f" + label + "&- edited in &e" + menu.getName() + "&-, position &e" + pos);		
		}
		menu.notifyObservers(SMSMenuAction.REPAINT);

		return true;
	}

	@Override
	public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
		switch (args.length) {
		case 1:
			return getMenuCompletions(plugin, sender, args[0]);
		case 2:
			SMSMenu menu = getMenu(sender, args[0]);
			return getMenuItemCompletions(sender, menu, args[1]);
		default:
			showUsage(sender);
			return noCompletions(sender);
		}
	}
}
