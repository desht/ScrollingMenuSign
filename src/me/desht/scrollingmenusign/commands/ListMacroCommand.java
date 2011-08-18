package me.desht.scrollingmenusign.commands;

import java.util.List;

import me.desht.scrollingmenusign.MessageBuffer;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMacro;
import me.desht.scrollingmenusign.ScrollingMenuSign;

import org.bukkit.entity.Player;

public class ListMacroCommand extends AbstractCommand {

	public ListMacroCommand() {
		super("sms m l", 0, 1);
		setPermissionNode("scrollingmenusign.commands.macro");
		setUsage("/sms macro list [<macro-name>]");
	}

	@Override
	public boolean execute(ScrollingMenuSign plugin, Player player, String[] args) throws SMSException {
		
		MessageBuffer.clear(player);
		int i = 1;
		if (args.length == 0) {
			List<String> macros = SMSMacro.getMacros();
			MessageBuffer.add(player, "&e" + macros.size() + " macros");
			for (String m : macros) {
				MessageBuffer.add(player, " &e" + i++ + ") &f" + m);
			}
		} else {
			List<String> cmds = SMSMacro.getCommands(args[0]);
			MessageBuffer.add(player, "&e" + cmds.size() + " macro entries");
			for (String c : cmds) {
				MessageBuffer.add(player, " &e" + i++ + ") &f" + c);
			}
		}
		MessageBuffer.showPage(player);
		
		return true;
	}

}
