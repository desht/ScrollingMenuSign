package me.desht.scrollingmenusign;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class SMSDebugger {
	private ScrollingMenuSign plugin;
	private Map<String,Integer> debuggers;
	
	SMSDebugger(ScrollingMenuSign plugin) {
		this.plugin = plugin;
		
		debuggers = new HashMap<String,Integer>();
	}

	void addDebugger(Player p, int level) {
		debuggers.put(debuggerName(p), level);
	}
	
	void removeDebugger(Player p) {
		debuggers.remove(debuggerName(p));
	}
	
	int getDebugLevel(Player p) {
		String name = debuggerName(p);
		if (!debuggers.containsKey(name) || debuggers.get(name) == 0) {
			return 0;
		} else {
			return debuggers.get(name);
		}
	}
	
	void toggleDebug(Player p) {
		if (getDebugLevel(p) == 0) {
			addDebugger(p, 1);
		} else {
			removeDebugger(p);
		}
	}
	
	void debug(String message) {
		debug(message, 1);
	}
	void debug(String message, int level) {
		if (debuggers.keySet().size() == 0)
			return;
		for (String name : debuggers.keySet()) {
			if (level >= debuggers.get(name)) {
				if (name.equals("&console")) {
					debugMessage(null, message);
				} else {
					Player p = plugin.getServer().getPlayer(name);
					if (p != null) {
						debugMessage(p, message);
					} else {
						removeDebugger(p);
					}
				}
			}
		}
	}
	
	private String debuggerName(Player p) {
		String name;
		if (p == null) {
			name = "&console";
		} else {
			name = p.getName();
		}
		return name;
	}

	private void debugMessage(Player p, String message) {
		plugin.status_message(p, ChatColor.DARK_GREEN + "[SMS] " + ChatColor.GREEN + message);
	}
}
