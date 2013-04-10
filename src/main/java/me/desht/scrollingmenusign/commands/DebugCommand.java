package me.desht.scrollingmenusign.commands;

import java.util.logging.Level;

import me.desht.dhutils.ConfigurationManager;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.ScrollingMenuSign;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class DebugCommand extends SMSAbstractCommand {

	public DebugCommand() {
		super("sms debug", 0, 0);
		setPermissionNode("scrollingmenusign.commands.debug");
		setUsage("/sms debug");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender player, String[] args) {
		// backwards compatibility - only toggles between INFO and FINE
		// use /sms set log_level for better control

		ConfigurationManager configManager = ((ScrollingMenuSign) plugin).getConfigManager();
		Level l = LogUtils.getLogLevel();
		if (l.intValue() < Level.INFO.intValue()) {
			configManager.set("log_level", "info");
		} else {
			configManager.set("log_level", "fine");
		}
		MiscUtil.statusMessage(player, "Log level is now " + LogUtils.getLogLevel());
		MiscUtil.statusMessage(player, "  &6(use &n/sms set log_level <level>&r for finer control)");

		return true;
	}

}
