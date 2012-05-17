package me.desht.scrollingmenusign.commands;

import me.desht.scrollingmenusign.SMSMacro;
import me.desht.scrollingmenusign.SMSPersistence;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.commands.AbstractCommand;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class AddMacroCommand extends AbstractCommand {

	public AddMacroCommand() {
		super("sms m a", 2);
		setPermissionNode("scrollingmenusign.commands.macro");
		setUsage("/sms macro add <macro> <command>");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		String s = combine(args, 1);

		SMSMacro.getMacro(args[0], true).addLine(s);
		MiscUtil.statusMessage(sender, "Added command to macro &e" + args[0] + "&-.");

		SMSPersistence.saveMacros();
		return true;
	}

}
