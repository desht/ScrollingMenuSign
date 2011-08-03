package me.desht.scrollingmenusign;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class SMSUtils {
	private static String prevColour;
	protected static final Logger logger = Logger.getLogger("Minecraft");
	protected static final String messageFormat = "ScrollingMenuSign: %s";
	
	static void errorMessage(Player player, String string) {
		prevColour = ChatColor.RED.toString();
		message(player, string, ChatColor.RED, Level.WARNING);
	}

	static void statusMessage(Player player, String string) {
		prevColour = ChatColor.AQUA.toString();
		message(player, string, ChatColor.AQUA, Level.INFO);
	}

	static void alertMessage(Player player, String string) {
		if (player == null) {
			return;
		}
		prevColour = ChatColor.YELLOW.toString();
		message(player, string, ChatColor.YELLOW, Level.INFO);
	}

	static void generalMessage(Player player, String string) {
		prevColour = ChatColor.WHITE.toString();
		message(player, string, Level.INFO);
	}
	
	static void broadcastMessage(String string) {
		prevColour = ChatColor.YELLOW.toString();
		Bukkit.getServer().broadcastMessage(parseColourSpec("&4::&-" + string)); //$NON-NLS-1$
	}

	private static void message(Player player, String string, Level level) {
		for (String line : string.split("\\n")) { //$NON-NLS-1$
			if (player != null) {
				player.sendMessage(parseColourSpec(line));
			} else {
				log(level, ChatColor.stripColor(parseColourSpec(line)));
			}
		}
	}

	private static void message(Player player, String string, ChatColor colour, Level level) {
		for (String line : string.split("\\n")) { //$NON-NLS-1$
			if (player != null) {
				player.sendMessage(colour + parseColourSpec(line));
			} else {
				log(level, ChatColor.stripColor(parseColourSpec(line)));
			}
		}
	}

	static String parseColourSpec(String spec) {
		String res = spec.replaceAll("&(?<!&&)(?=[0-9a-fA-F])", "\u00A7"); //$NON-NLS-1$ //$NON-NLS-2$
		return res.replace("&-", prevColour).replace("&&", "&"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	static String formatLocation(Location loc) {
		return String.format("%d,%d,%d,%s", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), loc.getWorld().getName());
	}

	static Location parseLocation(String arglist) {
		return parseLocation(arglist, null);
	}
	
	static Location parseLocation(String arglist, Player player) {
		String s = player == null ? ",worldname" : "";
		String args[] = arglist.split(",");
		try {
			int x = Integer.parseInt(args[0]);
			int y = Integer.parseInt(args[1]);
			int z = Integer.parseInt(args[2]);
			World w = (player == null) ?
					Bukkit.getServer().getWorld(args[3]) :
					player.getWorld();
			if (w == null) throw new IllegalArgumentException("Unknown world: " + args[3]);
			return new Location(w, x, y, z);
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new IllegalArgumentException("You must specify all of x,y,z" + s + ".");
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid location: x,y,z" + s + ".");
		}
	}
	
	static void log(String message) {
		if (message != null) {
			logger.log(Level.INFO, String.format(messageFormat, message));
		}
	}

	static void log(Level level, String message) {
		if (level == null) {
			level = Level.INFO;
		}
		if (message != null) {
			logger.log(level, String.format(messageFormat, message));
		}
	}

	static void log(Level level, String message, Exception err) {
		if (err == null) {
			log(level, message);
		} else {
			logger.log(level, String.format(messageFormat,
					message == null ? (err == null ? "?" : err.getMessage()) : message), err);
		}
	}

	static String parseColourSpec(Player player, String spec) {
		if (player == null ||
				SMSPermissions.isAllowedTo(player, "scrollingmenusign.coloursigns") || 
				SMSPermissions.isAllowedTo(player, "scrollingmenusign.colorsigns"))
		{
			String res = spec.replaceAll("&(?<!&&)(?=[0-9a-fA-F])", "\u00A7");
			return res.replace("&&", "&");
		} else {
			return spec;
		}		
	}
		
	static String unParseColourSpec(String spec) {
		return spec.replaceAll("\u00A7", "&");
	}
	

	static String deColourise(String s) {
		return s.replaceAll("\u00A7.", "");
	}

}
