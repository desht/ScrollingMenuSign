package me.desht.scrollingmenusign.commandlets;

import org.bukkit.command.CommandSender;

import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.views.SMSView;

public abstract class BaseCommandlet {
	public abstract void execute(ScrollingMenuSign plugin, CommandSender sender, SMSView view, String cmd, String[] args);
}
