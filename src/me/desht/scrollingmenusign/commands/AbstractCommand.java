package me.desht.scrollingmenusign.commands;

import org.bukkit.entity.Player;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.ScrollingMenuSign;

public abstract class AbstractCommand {
	private String command;	// the command
	private String subCommands[];
	private String usage[];
	private int minArgs, maxArgs;
	private String permissionNode;
	
	public AbstractCommand(String label) {
		_initArgs(label.split(" "));
		
		minArgs = subCommands.length;
		maxArgs = Integer.MAX_VALUE;
	}
	
	public AbstractCommand(String label, int minArgs, int maxArgs) {
		_initArgs(label.split(" "));
		
		this.minArgs = minArgs + subCommands.length;
		this.maxArgs = maxArgs + subCommands.length;
	}
	
	private void _initArgs(String[] fields) {
		this.command = fields[0];
		this.subCommands = new String[fields.length - 1];
		for (int i = 1; i < fields.length; i++) {
			subCommands[i] = fields[i];
		}
	}
	
	public boolean matches(String label, String[] args) {
		if (!label.equalsIgnoreCase(this.command))
			return false;
		
		if (args.length < minArgs || args.length > maxArgs) {
			return false;
		}
		
		for (int i = 1; i < args.length; i++) {
			if (!CommandManager.partialMatch(args[i], this.subCommands[i])) {
				return false;
			}
		}
		
		return true;
	}
	
	String[] getArgs(String[] args) {
		String[] result = new String[args.length - subCommands.length];
		for (int i = subCommands.length; i < args.length; i++) {
			result[i - subCommands.length] = args[i];
		}
		return result;
	}
	
	protected void setPermissionNode(String node) {
		this.permissionNode = node;
	}
	
	void setUsage(String[] usage) {
		this.usage = usage;
	}
	
	String[] getUsage() {
		return usage;
	}

	protected String getPermissionNode() {
		return permissionNode;
	}
	
	public abstract boolean execute(ScrollingMenuSign plugin, Player player, String[] args) throws SMSException;
}
