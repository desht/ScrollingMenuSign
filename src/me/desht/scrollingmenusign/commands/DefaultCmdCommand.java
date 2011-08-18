package me.desht.scrollingmenusign.commands;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSUtils;
import me.desht.scrollingmenusign.ScrollingMenuSign;

import org.bukkit.entity.Player;

public class DefaultCmdCommand extends AbstractCommand {

	public DefaultCmdCommand() {
		super("sms def", 1);
		setPermissionNode("scrollingmenusign.commands.defcmd");
		setUsage("/sms defcmd <menu-name> [<default-command>]");
	}

	@Override
	public boolean execute(ScrollingMenuSign plugin, Player player, String[] args) throws SMSException {
		String menuName = args[0];
		SMSMenu menu = plugin.getHandler().getMenu(menuName);
		String cmd = combine(args, 1);
		menu.setDefaultCommand(cmd);
		
		if (cmd.isEmpty()) {
			SMSUtils.statusMessage(player, "Default command has been cleared for menu &e" + menuName);
		} else {
			SMSUtils.statusMessage(player, "Default command has been set for menu &e" + menuName);
		}
		
		return false;
	}

}
