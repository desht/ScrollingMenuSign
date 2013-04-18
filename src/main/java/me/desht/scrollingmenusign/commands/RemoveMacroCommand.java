package me.desht.scrollingmenusign.commands;

import java.util.List;

import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMacro;
import me.desht.scrollingmenusign.SMSPersistence;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class RemoveMacroCommand extends SMSAbstractCommand {

	public RemoveMacroCommand() {
		super("sms macro remove", 1, 2);
		setPermissionNode("scrollingmenusign.commands.macro");
		setUsage("/sms macro remove <macro> [<command-index>]");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		if (args.length == 1) {
			SMSMacro.removeMacro(args[0]);
			MiscUtil.statusMessage(sender, "Removed macro &e" + args[0] + "&-.");	
		} else {
			try {
				int index = parseNumber(args[1]);
				SMSMacro.getMacro(args[0]).removeLine(index - 1);
				MiscUtil.statusMessage(sender, "Removed command #" + index + " from macro &e" + args[0] + "&-.");
			} catch (IndexOutOfBoundsException e) {
				throw new SMSException("invalid index: " + args[1]);
			}
		}

		SMSPersistence.saveMacros();

		return true;
	}

	@Override
	public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
		switch (args.length) {
		case 1:
			return getMacroCompletions(sender, args[0]);
		default:
			showUsage(sender);
			return noCompletions(sender);
		}
		// TODO handle 2-argument case
	}
}
