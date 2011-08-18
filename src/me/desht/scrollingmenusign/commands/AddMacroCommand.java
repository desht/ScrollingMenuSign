package me.desht.scrollingmenusign.commands;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMacro;
import me.desht.scrollingmenusign.SMSUtils;
import me.desht.scrollingmenusign.ScrollingMenuSign;

import org.bukkit.entity.Player;

public class AddMacroCommand extends AbstractCommand {

	public AddMacroCommand() {
		super("sms m a", 2);
		setPermissionNode("scrollingmenusign.commands.macro.add");
		setUsage("/sms macro add <macro> <command>");
	}

	@Override
	public boolean execute(ScrollingMenuSign plugin, Player player, String[] args) throws SMSException {
		String s = combine(args, 1);
		SMSMacro.addCommand(args[0], s);
		SMSUtils.statusMessage(player, "Added command to macro &e" + args[0] + "&-.");
		
		plugin.saveMacros();
		
		return true;
	}

}
