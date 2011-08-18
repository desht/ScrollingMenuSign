package me.desht.scrollingmenusign.commands;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSUtils;
import me.desht.scrollingmenusign.ScrollingMenuSign;

import org.bukkit.entity.Player;

public class DebugCommand extends AbstractCommand {

	public DebugCommand() {
		super("sms deb", 0, 0);
		setPermissionNode("scrollingmenusign.commands.debug");
		setUsage("/sms debug");
	}

	@Override
	public boolean execute(ScrollingMenuSign plugin, Player player, String[] args) throws SMSException {
		plugin.getDebugger().toggleDebug(player);
		int level = plugin.getDebugger().getDebugLevel(player);
		if (level > 0) {
			SMSUtils.statusMessage(player, "Debugging enabled.");
		} else {
			SMSUtils.statusMessage(player, "Debugging disabled.");
		}
	
		return true;
	}

}
