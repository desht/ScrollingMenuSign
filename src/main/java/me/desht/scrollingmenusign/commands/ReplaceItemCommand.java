package me.desht.scrollingmenusign.commands;

import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.commands.AbstractCommand;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.parser.CommandParser;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class ReplaceItemCommand extends AbstractCommand {
	
	public ReplaceItemCommand() {
		super("sms rep", 3);
		setPermissionNode("scrollingmenusign.commands.replace");
		setUsage(new String[] {
				"/sms replace <menu-name> @<pos> \"label\" [\"command\"] [\"message\"]",
				"/sms replace <menu-name> \"label\" [\"command\"] [\"message\"]",
		});
		setQuotedArgs(true);
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		String menuName = args[0];
		int pos = -1;
		
		int argPos;
		if (args[1].startsWith("@")) {
			try {
				pos = Integer.parseInt(args[1].substring(1));
			} catch (NumberFormatException e) {
				throw new SMSException(e.getMessage() + " bad numeric index");
			}
			argPos = 2;
		} else {
			argPos = 1;
		}

		SMSMenu menu = SMSMenu.getMenu(menuName);
		
		String label = args[argPos];
		String command = args.length >= argPos+2 ? args[argPos+1] : "";
		String message = args.length >= argPos+3 ? args[argPos+2] : "";
		
		if (sender instanceof Player && !new CommandParser().verifyCreationPerms((Player) sender, command)) {
			throw new SMSException("You do not have permission to add that kind of command.");
		}
		
		if (pos > 0) {
			if (label.isEmpty()) label = menu.getItemAt(pos, true).getLabel();
			if (command.isEmpty()) command = menu.getItemAt(pos, true).getCommand();
			if (message.isEmpty()) message = menu.getItemAt(pos, true).getMessage();
			menu.replaceItem(pos, label, command, message);
			MiscUtil.statusMessage(sender, "Menu item &f" + label + "&- replaced in &e" + menuName + "&-, position &e" + pos);
		} else {
			if (command.isEmpty()) command = menu.getItem(label, true).getCommand();
			if (message.isEmpty()) message = menu.getItem(label, true).getMessage();
			menu.replaceItem(label, command, message);
			MiscUtil.statusMessage(sender, "Menu item &f" + label + "&- replaced in &e" + menuName + "&-");
		}
		
		return true;
	}
	
}
