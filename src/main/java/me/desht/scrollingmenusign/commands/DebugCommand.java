package me.desht.scrollingmenusign.commands;

import java.util.logging.Level;

import me.desht.scrollingmenusign.SMSConfig;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.commands.AbstractCommand;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class DebugCommand extends AbstractCommand {

	public DebugCommand() {
		super("sms deb", 0, 0);
		setPermissionNode("scrollingmenusign.commands.debug");
		setUsage("/sms debug");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender player, String[] args) {
		// backwards compatibility - only toggles between INFO and FINE
		// use /sms set log_level for better control
		
		Level l = LogUtils.getLogLevel();
		if (l.intValue() < Level.INFO.intValue()) {
			SMSConfig.setPluginConfiguration("log_level", "info");
		} else {
			SMSConfig.setPluginConfiguration("log_level", "fine");
		}
		MiscUtil.statusMessage(player, "Log level is now " + LogUtils.getLogLevel());
		MiscUtil.statusMessage(player, "  &6(use &n/sms set log_level <level>&r for finer control)");
	
		return true;
	}

}
