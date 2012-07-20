package me.desht.scrollingmenusign.commands;

import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.commands.AbstractCommand;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.scrollingmenusign.parser.CommandParser;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class AddItemCommand extends AbstractCommand {

	public AddItemCommand() {
		super("sms a", 2);
		setPermissionNode("scrollingmenusign.commands.add");
		setUsage(new String[] {
				"/sms add <menu-name> \"label\" [\"command\"] [\"message\"]",
				"/sms add <menu-name> @<pos> \"label\" [\"command\"] [\"message\"]"
		});
		setQuotedArgs(true);
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		String menuName = args[0];
		
		int pos = -1;
		int argPos = 1;
		if (args[1].startsWith("@")) {
			try {
				pos = Integer.parseInt(args[1].substring(1));
			} catch (NumberFormatException e) {
				throw new SMSException(e.getMessage() + " bad numeric index");
			}
			argPos = 2;
		}

		SMSMenu menu = SMSMenu.getMenu(menuName);

		if (args.length < argPos+2 && menu.getDefaultCommand().isEmpty()) {
			throw new SMSException("Missing command and feedback message");
		}

		String label = MiscUtil.parseColourSpec(sender, args[argPos]);
		String cmd = args.length >= argPos+2 ? args[argPos+1] : "";
		String msg = args.length >= argPos+3 ? args[argPos+2] : "";

		if (sender instanceof Player && !new CommandParser().verifyCreationPerms((Player) sender, cmd)) {
			throw new SMSException("You do not have permission to add that kind of command.");
		}

		if (pos < 0) {
			menu.addItem(label, cmd, msg);
			MiscUtil.statusMessage(sender, "Menu item &f" + label + "&- added to &e" + menuName);
		} else {
			menu.insertItem(pos, label, cmd, msg);
			int realPos = menu.indexOfItem(label);
			MiscUtil.statusMessage(sender, "Menu item &f" + label + "&- inserted in &e" + menuName + "&- at position " + realPos);
		}

		menu.notifyObservers(SMSMenuAction.REPAINT);

		return true;
	}

}
