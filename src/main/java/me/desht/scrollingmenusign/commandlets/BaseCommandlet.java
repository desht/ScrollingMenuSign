package me.desht.scrollingmenusign.commandlets;

import org.bukkit.command.CommandSender;

import me.desht.scrollingmenusign.views.SMSView;

public abstract class BaseCommandlet {
	public abstract void execute(CommandSender sender, SMSView view, String cmd, String[] args);
}
