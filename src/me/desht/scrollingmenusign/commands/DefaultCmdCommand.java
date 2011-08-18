package me.desht.scrollingmenusign.commands;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.util.MiscUtil;

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
			MiscUtil.statusMessage(player, "Default command has been cleared for menu &e" + menuName);
		} else {
			MiscUtil.statusMessage(player, "Default command has been set for menu &e" + menuName);
		}
		
		return false;
	}

}
