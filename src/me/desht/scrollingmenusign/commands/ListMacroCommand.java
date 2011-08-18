package me.desht.scrollingmenusign.commands;

import java.util.List;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMacro;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.util.MessagePager;

import org.bukkit.entity.Player;

public class ListMacroCommand extends AbstractCommand {

	public ListMacroCommand() {
		super("sms m l", 0, 1);
		setPermissionNode("scrollingmenusign.commands.macro");
		setUsage("/sms macro list [<macro-name>]");
	}

	@Override
	public boolean execute(ScrollingMenuSign plugin, Player player, String[] args) throws SMSException {
		
		MessagePager.clear(player);
		int i = 1;
		if (args.length == 0) {
			List<String> macros = SMSMacro.getMacros();
			MessagePager.add(player, "&e" + macros.size() + " macros");
			for (String m : macros) {
				MessagePager.add(player, " &e" + i++ + ") &f" + m);
			}
		} else {
			List<String> cmds = SMSMacro.getCommands(args[0]);
			MessagePager.add(player, "&e" + cmds.size() + " macro entries");
			for (String c : cmds) {
				MessagePager.add(player, " &e" + i++ + ") &f" + c);
			}
		}
		MessagePager.showPage(player);
		
		return true;
	}

}
