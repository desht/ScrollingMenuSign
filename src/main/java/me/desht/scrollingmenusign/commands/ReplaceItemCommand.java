package me.desht.scrollingmenusign.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.commands.AbstractCommand;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.parser.CommandParser;

public class ReplaceItemCommand extends AbstractCommand {
	
	public ReplaceItemCommand() {
		super("sms rep", 3);
		setPermissionNode("scrollingmenusign.commands.replace");
		setUsage("/sms replace <menu-name> <position> \"label\" [\"command\"] [\"message\"]");
		setQuotedArgs(true);
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		String menuName = args[0];
		int pos;
		try {
			pos = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			throw new SMSException(e.getMessage());
		}

		SMSMenu menu = SMSMenu.getMenu(menuName);
		
		String label = args[2];
		String command = args.length >= 4 ? args[3] : "";
		String message = args.length >= 5 ? args[4] : "";
		
		if (sender instanceof Player && !new CommandParser().verifyCreationPerms((Player) sender, command)) {
			throw new SMSException("You do not have permission to add that kind of command.");
		}
		
		menu.replaceItem(pos, label, command, message);
		
		MiscUtil.statusMessage(sender, "Menu item &f" + label + "&- replaced in: &e" + menuName + "&-, position &e" + pos);
		
		return true;
	}

}
