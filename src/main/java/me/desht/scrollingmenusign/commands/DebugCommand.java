package me.desht.scrollingmenusign.commands;

import java.util.logging.Level;

import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.util.MiscUtil;
import me.desht.scrollingmenusign.util.SMSLogger;

import org.bukkit.entity.Player;

public class DebugCommand extends AbstractCommand {

	public DebugCommand() {
		super("sms deb", 0, 0);
		setPermissionNode("scrollingmenusign.commands.debug");
		setUsage("/sms debug");
	}

	@Override
	public boolean execute(ScrollingMenuSign plugin, Player player, String[] args) {
		// backwards compatibility - only toggles between INFO and FINE
		// use /sms set log_level for better control
		
		Level l = SMSLogger.getLogLevel();
		if (l.intValue() > Level.INFO.intValue()) {
			SMSLogger.setLogLevel(Level.INFO);
		} else {
			SMSLogger.setLogLevel(Level.FINE);
		}
		MiscUtil.statusMessage(player, "Log level is now " + SMSLogger.getLogLevel());
		MiscUtil.statusMessage(player, "  &6(use &n/sms set log_level <level>&r for better control)");
	
		return true;
	}

}
