package me.desht.scrollingmenusign.commands;

import java.util.List;

import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.SMSMenu;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class DefaultCmdCommand extends SMSAbstractCommand {

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

	@Override
	public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
		if (args.length == 1) {
			return getMenuCompletions(plugin, sender, args[0]);
		} else {
			return noCompletions(sender);
		}
	}
}
