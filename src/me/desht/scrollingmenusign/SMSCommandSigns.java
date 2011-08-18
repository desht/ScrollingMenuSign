package me.desht.scrollingmenusign;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.edwardhand.commandsigns.CommandSigns;
import com.edwardhand.commandsigns.CommandSignsHandler;

public class SMSCommandSigns {
	static CommandSignsHandler csHandler = null;
	
	private SMSCommandSigns() {
	}
	
	static void setup() {
		Plugin csPlugin = Bukkit.getServer().getPluginManager().getPlugin("CommandSigns");
		if (csHandler == null) {
			if (csPlugin != null) {
				csHandler = ((CommandSigns) csPlugin).getHandler();
				SMSUtils.log(Level.INFO, "CommandSigns API integration enabled");
			} else {
				SMSUtils.log(Level.INFO, "CommandSigns API not available");
			}
		}
	}
	
	public static boolean isActive() {
		return csHandler != null;
	}
	
	public static boolean hasEnablingPermissions(Player player, String cmd) {
		return csHandler.hasEnablingPermissions(player, cmd);
	}
	
	static void runCommandString(Player player, String cmd) {
		csHandler.runCommandString(cmd, player);
	}
}
