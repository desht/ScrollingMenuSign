package me.desht.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import me.desht.scrollingmenusign.SMSException;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;
import com.platymuus.bukkit.permissions.Group;
import com.platymuus.bukkit.permissions.PermissionInfo;
import com.platymuus.bukkit.permissions.PermissionsPlugin;

public class PermissionsUtils {
	private static PermissionHandler permissionHandler = null;
	private static PermissionsPlugin permissionsBukkit;
	
	private PermissionsUtils() {	
	}
	
	public static void setup() {
		Plugin permissionsPlugin = Bukkit.getServer().getPluginManager().getPlugin("Permissions");

		if (permissionHandler == null) {
			if (permissionsPlugin != null) {
				permissionHandler = ((Permissions) permissionsPlugin).getHandler();
				MiscUtil.log(Level.INFO, "Permissions detected");
			} else {
				MiscUtil.log(Level.INFO, "Permissions not detected, using Bukkit superperms");
			}
		}
		
		Plugin permissionsBukkitPlugin = Bukkit.getServer().getPluginManager().getPlugin("PermissionsBukkit");
		if (permissionsBukkitPlugin != null) {
			permissionsBukkit = (PermissionsPlugin) permissionsBukkitPlugin;
			MiscUtil.log(Level.INFO, "PermissionsBukkit detected");
		}
	}
	
	public static Boolean isAllowedTo(Player player, String node) {
		if (player == null || node == null)
			return true;
		// if Permissions is in force, then use that
		if (permissionHandler != null) {
			return permissionHandler.has(player, node);
		} else {
			return player.hasPermission(node);
		}
	}
	
	public static void requirePerms(Player player, String node) throws SMSException {
		if (permissionHandler == null) {
			// Once support for Permissions is dropped, this check will be all that's required
			if (player == null) {
				return;
			} else if (player.hasPermission(node)) {
				return;
			} else {
				throw new SMSException("You are not allowed to do that (need node " + node + ").");
			}
		} else {
			if (!isAllowedTo(player, node)) {
				throw new SMSException("You are not allowed to do that (need node " + node + ").");
			}
		}
	}
	
	public static boolean isInGroup(Player player, String group) {
		if (permissionsBukkit != null) {
			for (Group grp :  permissionsBukkit.getGroups(player.getName())) {
				if (grp.getName().equalsIgnoreCase(group))
					return true;
			}
			return false;
		} else if (permissionHandler != null) {
			// TODO: permissions 3.x check
			return false;
		} else {
			return false;
		}
	}
	
	
	/**
	 * Elevate the permissions of player to match those of target
	 * 
	 * @param player
	 * @param target
	 */
	public static List<String> elevate(Player player, String targetGrp) {
		List<String> tempPerms = new ArrayList<String>();
		if (permissionsBukkit != null) {
			Group grp = permissionsBukkit.getGroup(targetGrp);
			PermissionInfo info = grp.getInfo();
			Map<String, Boolean> perms = info.getPermissions();
			PluginCommand command = permissionsBukkit.getCommand("permissions");
			QuietConsoleCommandSender console = new QuietConsoleCommandSender(Bukkit.getServer());
			
			for (Entry<String, Boolean> e : perms.entrySet()) {
				System.out.println("got perm " + e.getKey() + " = " + e.getValue());
//				if (e.getValue() != null && e.getValue() && !player.hasPermission(e.getKey())) {
				if (!player.hasPermission(e.getKey())) {
					tempPerms.add(e.getKey());
					command.getExecutor().onCommand(console, command, "permissions", new String[] { "player", "setperm", player.getName(), e.getKey(), "true"} );
				}
			}
		}
		
		return tempPerms;
	}

	public static void deElevate(Player player, List<String> tempPerms) {
		PluginCommand command = permissionsBukkit.getCommand("permissions");
		QuietConsoleCommandSender console = new QuietConsoleCommandSender(Bukkit.getServer());
		for (String perm : tempPerms) {
			command.getExecutor().onCommand(console, command, "permissions", new String[] { "player", "unsetperm", player.getName(), perm } );
		}
	}
	

}
