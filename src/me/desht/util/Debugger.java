package me.desht.util;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class Debugger {
	private static Debugger debuggerInstance = null;

	private Map<String,Integer> debuggers;
	
	public static Debugger getDebugger() {
		if (debuggerInstance == null)
			debuggerInstance = new Debugger();
		return debuggerInstance;
	}
	
	private Debugger() {
		debuggers = new HashMap<String,Integer>();
	}

	public void addDebugger(Player p, int level) {
		debuggers.put(debuggerName(p), level);
	}
	
	public void removeDebugger(Player p) {
		debuggers.remove(debuggerName(p));
	}
	
	public int getDebugLevel(Player p) {
		String name = debuggerName(p);
		if (!debuggers.containsKey(name) || debuggers.get(name) == 0) {
			return 0;
		} else {
			return debuggers.get(name);
		}
	}
	
	public void toggleDebug(Player p) {
		if (getDebugLevel(p) == 0) {
			addDebugger(p, 1);
		} else {
			removeDebugger(p);
		}
	}
	
	public void debug(String message) {
		debug(message, 1);
	}
	public void debug(String message, int level) {
		if (debuggers.keySet().size() == 0)
			return;
		for (String name : debuggers.keySet()) {
			if (level >= debuggers.get(name)) {
				if (name.equals("&console")) {
					debugMessage(null, message);
				} else {
					Player p = Bukkit.getServer().getPlayer(name);
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
		StringBuilder msg = new StringBuilder(ChatColor.GREEN.toString()).append("[DEBUG] ");
		msg.append(ChatColor.DARK_GREEN.toString()).append(message);
		MiscUtil.statusMessage(p, msg.toString());
	}
}
