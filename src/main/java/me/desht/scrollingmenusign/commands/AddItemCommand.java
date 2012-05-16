package me.desht.scrollingmenusign.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import me.desht.scrollingmenusign.SMSConfig;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.scrollingmenusign.parser.CommandParser;
import me.desht.dhutils.MiscUtil;

import org.bukkit.entity.Player;

public class AddItemCommand extends AbstractCommand {

	public AddItemCommand() {
		super("sms a", 2);
		setPermissionNode("scrollingmenusign.commands.add");
		setUsage("/sms add <menu-name> \"label\" [\"command\"] [\"message\"]");
		setQuotedArgs(true);
	}

	@Override
	public boolean execute(ScrollingMenuSign plugin, Player player, String[] args) throws SMSException {

		String menuName = args[0];

		String sep = SMSConfig.getConfig().getString("sms.menuitem_separator", "|");
		List<String> items;		
		if (args[1].contains(sep)) {
			items = Arrays.asList(combine(args, 1).split(Pattern.quote(sep)));
			String[] usage = getUsage();
			MiscUtil.statusMessage(player, "&6NOTE: preferred syntax is now &f" + usage[0]);
			MiscUtil.statusMessage(player, " &6(label/command/message can be quoted if they contain whitespace)");
		} else {
			items = new ArrayList<String>();
			for (int i = 1; i < args.length; i++) {
				items.add(args[i]);
			}
		}

		SMSMenu menu = plugin.getHandler().getMenu(menuName);

		if (items.size() < 2 && menu.getDefaultCommand().isEmpty()) {
			throw new SMSException("Missing command and feedback message");
		}

		String label = MiscUtil.parseColourSpec(player, items.get(0));
		String cmd = items.size() >= 2 ? items.get(1) : "";
		String msg = items.size() >= 3 ? items.get(2) : "";

		if (player != null && !new CommandParser().verifyCreationPerms(player, cmd)) {
			throw new SMSException("You do not have permission to add that kind of command.");
		}

		menu.addItem(label, cmd, msg);
		menu.notifyObservers(SMSMenuAction.REPAINT);

		MiscUtil.statusMessage(player, "Menu entry &f" + label + "&- added to: &e" + menuName);

		return true;
	}

}
