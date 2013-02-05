package me.desht.scrollingmenusign.commandlets;

import java.util.HashMap;
import java.util.Map;

import me.desht.scrollingmenusign.ScrollingMenuSign;

public class CommandletManager {
	private final ScrollingMenuSign plugin;
	private final Map<String, BaseCommandlet> map;
	
	public CommandletManager(ScrollingMenuSign plugin) {
		this.map = new HashMap<String, BaseCommandlet>();
		this.plugin = plugin;
	}
	
	public void registerCommandlet(String name, BaseCommandlet cmd) {
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
