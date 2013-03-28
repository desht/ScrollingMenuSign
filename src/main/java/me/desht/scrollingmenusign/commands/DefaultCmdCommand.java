package me.desht.scrollingmenusign.commands;

import me.desht.scrollingmenusign.SMSMenu;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.commands.AbstractCommand;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class DefaultCmdCommand extends AbstractCommand {

	public DefaultCmdCommand() {
		super("sms defcmd", 1);
		setPermissionNode("scrollingmenusign.commands.defcmd");
		setUsage("/sms defcmd <menu-name> [<default-command>]");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		String menuName = args[0];
		SMSMenu menu = SMSMenu.getMenu(menuName);
		String cmd = combine(args, 1);
		menu.setDefaultCommand(cmd);
		
		if (cmd.isEmpty()) {
			MiscUtil.statusMessage(sender, "Default command has been cleared for menu &e" + menuName);
		} else {
			MiscUtil.statusMessage(sender, "Default command has been set for menu &e" + menuName);
		}
		
		return true;
	}

}
