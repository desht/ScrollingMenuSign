package me.desht.util;

import java.util.logging.Level;

import me.desht.scrollingmenusign.SMSException;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;

public class PermissionsUtils {
	private static PermissionHandler permissionHandler = null;
	
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
				throw new SMSException("You are not allowed to do that.");
			}
		} else {
			if (!isAllowedTo(player, node)) {
				throw new SMSException("You are not allowed to do that.");
			}
		}
	}
}
