package me.desht.scrollingmenusign;

import org.bukkit.command.CommandSender;

interface SMSUseLimitable {
	void autosave();
	String getDescription();
	String formatUses(CommandSender sender);
}
