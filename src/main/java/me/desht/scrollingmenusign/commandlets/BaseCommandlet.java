package me.desht.scrollingmenusign.commandlets;

import org.bukkit.command.CommandSender;

import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.views.SMSView;

public abstract class BaseCommandlet {
	private final String name;
	
	protected BaseCommandlet(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public abstract void execute(ScrollingMenuSign plugin, CommandSender sender, SMSView view, String cmd, String[] args);
}
