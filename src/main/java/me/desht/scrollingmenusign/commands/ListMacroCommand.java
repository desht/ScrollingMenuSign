package me.desht.scrollingmenusign.commands;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMacro;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.dhutils.MessagePager;

import org.bukkit.entity.Player;

public class ListMacroCommand extends AbstractCommand {

	public ListMacroCommand() {
		super("sms m l", 0, 1);
		setPermissionNode("scrollingmenusign.commands.macro");
		setUsage("/sms macro list [<macro-name>]");
	}

	@Override
	public boolean execute(ScrollingMenuSign plugin, Player player, String[] args) throws SMSException {
		MessagePager pager = MessagePager.getPager(player).clear();
		int i = 1;
		if (args.length == 0) {
			pager.add("&e" + SMSMacro.getMacros().size() + " macros:");
			for (SMSMacro m : SMSMacro.listMacros(true)) {
				pager.add(" &e" + i++ + ") &f" + m.getName() + "   &e[" + m.getLines().size() + " lines]");
			}
		} else {
			SMSMacro m = SMSMacro.getMacro(args[0], false);
			pager.add("&fMacro &e" + m.getName() + "&f [" + m.getLines().size() + " lines]:");
			for (String l : m.getLines()) {
				pager.add(" &e" + i++ + ") &f" + l);
			}
		}
		pager.showPage();
		
		return true;
	}

}
