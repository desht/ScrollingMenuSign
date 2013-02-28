package me.desht.scrollingmenusign.commandlets;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.Validate;

import me.desht.scrollingmenusign.ScrollingMenuSign;

public class CommandletManager {
	private final ScrollingMenuSign plugin;
	private final Map<String, BaseCommandlet> map;
	
	public CommandletManager(ScrollingMenuSign plugin) {
		this.map = new HashMap<String, BaseCommandlet>();
		this.plugin = plugin;
	}
	
	public void registerCommandlet(BaseCommandlet cmd) {
		String name = cmd.getName();
		Validate.isTrue(!map.containsKey(name), "Commandlet " + name + " is already registered");
		map.put(name, cmd);
	}
	
	public boolean hasCommandlet(String name) {
		return map.containsKey(name);
	}
	
	public BaseCommandlet getCommandlet(String name) {
		return map.get(name);
	}
	
	public ScrollingMenuSign getPlugin() {
		return plugin;
	}
}
