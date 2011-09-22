package me.desht.scrollingmenusign;

import java.util.logging.Level;

import me.desht.util.MiscUtil;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class SMSCommandSigns {

	static com.edwardhand.commandsigns.CommandSignsHandler csHandler = null;
	// version 1.0.1 from http://forums.bukkit.org/threads/33860/
	//		-- not sure if can be hooked into, but good to check against (not the same)
	static com.hans.CommandSigns.CommandSigns cs2 = null;

	private SMSCommandSigns() {
	}

	static void setup() {
		if (csHandler == null) {
			Plugin csPlugin = Bukkit.getServer().getPluginManager().getPlugin("CommandSigns");
			if (csPlugin != null) {
				if (csPlugin instanceof com.edwardhand.commandsigns.CommandSigns) {
					csHandler = ((com.edwardhand.commandsigns.CommandSigns) csPlugin).getHandler();
					MiscUtil.log(Level.INFO, "CommandSigns API integration enabled");
				} else if (csPlugin instanceof com.hans.CommandSigns.CommandSigns) {
					cs2 = ((com.hans.CommandSigns.CommandSigns) csPlugin);
				}
			} else {
				MiscUtil.log(Level.INFO, "CommandSigns API not available");
			}
		}
	}

	public static boolean isActive() {
		return csHandler != null;
	}

	public static boolean hasEnablingPermissions(Player player, String cmd) {
		return csHandler != null && csHandler.hasEnablingPermissions(player, cmd);
	}

	static void runCommandString(Player player, String cmd) {
		csHandler.runCommandString(cmd, player);
	}
}
