package me.desht.scrollingmenusign.util;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.ScrollingMenuSign;

import org.bukkit.entity.Player;

public class PermissionsUtils {

	/**
	 * Check if the player has the specified permission node.
	 * 
	 * @param player	Player to check
	 * @param node		Node to check for
	 * @return	true if the player has the permission node, false otherwise
	 */
	public static boolean isAllowedTo(Player player, String node) {
		boolean allowed;
		if (player == null || node == null) {
			allowed = true;
		} else {
			// We'll try use the native Bukkit hasPermission() method if possible.
			// This is mainly for the benefit of PermissionsEx, which seems to have problems if we use its native method
			// to check for parent permission nodes, but using the native superperms call is OK.
			if (ScrollingMenuSign.permission != null && !ScrollingMenuSign.permission.hasSuperPermsCompat()) {
				allowed = ScrollingMenuSign.permission.has(player, node);
			} else {
				allowed = player.hasPermission(node);
			}
		}

		SMSLogger.fine("Permission check: player=" + player + ", node=" + node + ", allowed=" + allowed);

		return allowed;
	}

	/**
	 * Throw an exception if the player does not have the specified permission.
	 * 
	 * @param player	Player to check
	 * @param node		Require permission node
	 * @throws SMSException	if the player does not have the node
	 */
	public static void requirePerms(Player player, String node) throws SMSException {
		if (!isAllowedTo(player, node)) {
			throw new SMSException("You are not allowed to do that.");
		}
	}
}
