package me.desht.scrollingmenusign.commands;

import java.util.List;

import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.SMSMacro;
import me.desht.scrollingmenusign.SMSPersistence;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class AddMacroCommand extends SMSAbstractCommand {

	public AddMacroCommand() {
		super("sms macro add", 2);
		setPermissionNode("scrollingmenusign.commands.macro");
		setUsage("/sms macro add <macro> <command>");
		setQuotedArgs(true);
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		String s = combine(args, 1);

		SMSMacro.getMacro(args[0], true).addLine(s);
		MiscUtil.statusMessage(sender, "Added command to macro &e" + args[0] + "&-.");

		SMSPersistence.saveMacros();
		return true;
	}

	@Override
	public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
		if (args.length == 1) {
			return getMacroCompletions(sender, args[0]);
		} else {
			showUsage(sender);
			return noCompletions(sender);
		}
	}
}
