package me.desht.scrollingmenusign.commandlets;

import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.views.CommandTrigger;

import org.bukkit.command.CommandSender;

public abstract class BaseCommandlet {
	private final String name;

	protected BaseCommandlet(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public abstract boolean execute(ScrollingMenuSign plugin, CommandSender sender, CommandTrigger trigger, String cmd, String[] args);
}
