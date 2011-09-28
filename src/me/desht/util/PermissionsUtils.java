package me.desht.util;

import java.util.logging.Level;

import me.desht.scrollingmenusign.SMSException;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.bukkit.PermissionsEx;

import com.platymuus.bukkit.permissions.Group;
import com.platymuus.bukkit.permissions.PermissionsPlugin;

import de.bananaco.permissions.worlds.WorldPermissionsManager;

public class PermissionsUtils {
	private static Plugin activePlugin = null;
	
	private static PermissionsPlugin permissionsBukkit = null;	// PermissionsBukkit
	private static PermissionManager permissionManager = null;	// PermissionsEx
	private static WorldPermissionsManager wpm = null;			// bPermissions

	private PermissionsUtils() {	
	}

	/**
	 * Try to detect a supported permissions plugin.
	 */
	public static void setup() {
		PluginManager pm = Bukkit.getServer().getPluginManager();
		Plugin plugin = null;

		if ((plugin = pm.getPlugin("PermissionsBukkit")) != null) {
			permissionsBukkit = (PermissionsPlugin) plugin;
		} else if ((plugin = pm.getPlugin("PermissionsEx")) != null) {
			permissionManager = PermissionsEx.getPermissionManager();
		} else if ((plugin = pm.getPlugin("bPermissions")) != null) {
			wpm = de.bananaco.permissions.Permissions.getWorldPermissionsManager();
		}

		activePlugin = plugin;
		
		if (plugin != null) {
			MiscUtil.log(Level.INFO, "Permissions plugin detected: " + plugin.getDescription().getName() + " v" + plugin.getDescription().getVersion());
		} else {
			MiscUtil.log(Level.INFO, "No Permissions plugin detected - using built-in Bukkit superperms for permissions.");
		}
	}

	/**
	 * Is there a supported permissions plugin active?
	 * 
	 * @return true if a supported permissions plugin is active, false otherwise
	 */
	public static boolean isPluginActive() {
		return activePlugin != null;
	}
	
	/**
	 * Check if the player has the specified permission node.
	 * 
	 * @param player	Player to check
	 * @param node		Node to check for
	 * @return	true if the player has the permission node, false otherwise
	 */
	public static Boolean isAllowedTo(Player player, String node) {
		if (player == null || node == null)
			return true;
		else 
			return player.hasPermission(node);
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
			throw new SMSException("You are not allowed to do that (need node " + node + ").");
		}
	}

	/**
	 * Check if the player is in the specified group.
	 * 
	 * @param player
	 * @param group
	 * @return
	 */
	public static boolean isInGroup(Player player, String group) {
		if (permissionsBukkit != null) {
			for (Group grp :  permissionsBukkit.getGroups(player.getName())) {
				if (grp.getName().equalsIgnoreCase(group))
					return true;
			}
		} else if (permissionManager != null) {
			return permissionManager.getUser(player).inGroup(group);
		} else if (wpm != null) {
			for (String grp : wpm.getPermissionSet(player.getWorld().getName()).getGroups(player)) {
				if (grp.equalsIgnoreCase(group))
					return true;
			}
		} 

		return false;
	}

//	/**
//	 * Elevate the permissions of player to match those of target
//	 *
//	 * @param player	The player to elevate
//	 * @param target	The target player to copy the permissions from
//	 * @return			List of temporary nodes which were granted
//	 */
//	public static List<String> elevate(Player player, String target) {
//		List<String> tempNodes = new ArrayList<String>();
//
//		if (wpm != null) {
//			// bPermissions does things a bit differently - we copy the target's group(s)
//			// to the player, not permission nodes
//			PermissionSet ps = wpm.getPermissionSet(player.getWorld());
//			for (String grp : ps.getGroups(target)) {
//				if (!isInGroup(player, grp)) {
//					ps.addGroup(player, grp);
//					tempNodes.add(grp);
//				}
//			}
//			return tempNodes;
//		} else {
//			List<String> nodes = getPermissionNodes(target, player.getWorld());
//			if (nodes == null)
//				return null;
//			for (String node : nodes) {
//				if (!isAllowedTo(player, node))
//					tempNodes.add(node);
//			}
//		}
//
//		for (String perm: tempNodes) {
//			Debugger.getDebugger().debug("grant perm " + perm);
//			if (permissionsBukkit != null) {
//				// PermissionsBukkit has no API call to modify permissions - use a console command to do it
//				String cmd = String.format("permissions player setperm %s %s true", player.getName(), perm);
//				QuietConsoleCommandSender console = new QuietConsoleCommandSender(Bukkit.getServer());
//				Bukkit.getServer().dispatchCommand(console, cmd);
//			} else if (permissionHandler != null) {
//				com.nijiko.permissions.User user = permissionHandler.getUserObject(player.getWorld().getName(), player.getName());
//				if (user != null) {
//					user.addPermission(perm);
//				}
//			} else if (permissionManager != null) {
//				PermissionUser user = permissionManager.getUser(player.getName());
//				if (user != null) {
//					user.addPermission(perm, player.getWorld().getName());
//				}
//			} else if (wpm != null) {
//				PermissionSet ps = wpm.getPermissionSet(player.getWorld());
//				for (String grp : tempNodes) {
//					ps.addGroup(player, grp);
//				}
//			}
//		}
//
//		return tempNodes;
//	}
//
//	/**
//	 * De-elevate a player's permissions.
//	 * 
//	 * @param player	The player to de-elevate
//	 * @param tempPerms	The list of permission nodes to remove
//	 */
//	public static void deElevate(Player player, List<String> tempPerms) {
//		if (tempPerms == null)
//			return;
//
//		QuietConsoleCommandSender console = new QuietConsoleCommandSender(Bukkit.getServer());
//		for (String perm : tempPerms) {
//
//			Debugger.getDebugger().debug("withdraw perm " + perm);
//
//			if (permissionsBukkit != null) {		
//				String cmd = String.format("permissions player unsetperm %s %s true", player.getName(), perm);
//				Bukkit.getServer().dispatchCommand(console, cmd);
//			} else if (permissionHandler != null) {
//				com.nijiko.permissions.User user = permissionHandler.getUserObject(player.getWorld().getName(), player.getName());
//				if (user != null) {
//					user.removePermission(perm);
//				}
//			} else if (permissionManager != null) {
//				PermissionUser user = permissionManager.getUser(player.getName());
//				if (user != null) {
//					user.removePermission(perm, player.getWorld().getName());
//				}
//			} else if (wpm != null) {
//				// bPermissions does things a bit differently -
//				// we need to remove groups from the player rather than permission nodes
//				PermissionSet ps = wpm.getPermissionSet(player.getWorld());
//				for (String grp : tempPerms) {
//					ps.removeGroup(player, grp);
//				}
//			}
//		}
//	}
//
//	/**
//	 * Get a full list of the player's permission nodes.
//	 * 
//	 * @param playerName	Name of the player to check for
//	 * @param w				Player's world (use first known world if null is passed)
//	 * @return				A list of permission node strings
//	 */
//	public static List<String> getPermissionNodes(String playerName, World w) {
//		if (w == null)
//			w = Bukkit.getServer().getWorlds().get(0);
//
//		List<String> res = null;
//		if (permissionsBukkit != null) {
//			Map<String, Boolean> perms;
//			PermissionInfo info = permissionsBukkit.getPlayerInfo(playerName);
//			if (info == null)
//				return null;
//
//			try {
//				// this call currently throws an NPE if no explicit permissions defined
//				perms = info.getPermissions();
//			} catch (NullPointerException e) {
//				perms = new HashMap<String, Boolean>();
//			}
//			for (Group grp : info.getGroups()) {
//				PermissionInfo gInfo = grp.getInfo();
//				try {
//					// this call currently throws an NPE if no explicit permissions defined
//					Map<String, Boolean> gPerms = gInfo.getPermissions();
//					for (Entry<String, Boolean> e : gPerms.entrySet()) {
//						perms.put(e.getKey(), e.getValue());
//					}
//				} catch (NullPointerException e) {
//				}
//			}
//			res = new ArrayList<String>(perms.keySet());
//		} else if (permissionHandler != null) {
//			try {
//				com.nijiko.permissions.User user = permissionHandler.getUserObject(w.getName(), playerName);
//				if (user != null) {
//					res = new ArrayList<String>();
//					for (String s : user.getAllPermissions())
//						res.add(s);
//				}
//			} catch (NoSuchMethodError e) {
//				MiscUtil.log(Level.WARNING, "This version of Permissions doesn't appear to support permissions elevation (need Permissions 3.x)");
//				return null;
//			}
//		} else if (permissionManager != null) {
//			PermissionUser user = permissionManager.getUser(playerName);
//			if (user != null) {
//				res = Arrays.asList(user.getPermissions(w.getName()));
//			}
//		} else if (wpm != null) {
//			res = wpm.getPermissionSet(w).getPlayerNodes(playerName);
//		}
//
//		return res;
//	}

//	/**
//	 * Temporarily grant op status to a player.  We don't use player.setOp() because we don't
//	 * want the ops.txt file to be written.
//	 * 
//	 * @param player	The player to grant ops status to
//	 * @return			A set of all player names who currently have ops status
//	 */
//	@SuppressWarnings("unchecked")
//	public static Set<String> grantOpStatus(Player player) {
//		Field opsSetField = null;
//		Set<String> opsSet = null;
//	
//		try {
//			opsSetField = ServerConfigurationManager.class.getDeclaredField("operators");
//			opsSet = (Set<String>) opsSetField.get(((CraftServer)player.getServer()).getHandle());
//		} catch (NoSuchFieldException nfe) {
//			// earlier versions of CraftBukkit don't have the public "operators" field
//			// instead we need to use reflection to expose the private "h" field (yuck)
//			try {
//				opsSetField = ServerConfigurationManager.class.getDeclaredField("h");
//				opsSetField.setAccessible(true);
//				opsSet = (Set<String>) opsSetField.get(((CraftServer)player.getServer()).getHandle());
//			} catch (Exception e) {
//				MiscUtil.log(Level.WARNING, "Exception thrown when finding opsSet: " + e.getClass() + ": " + e.getMessage()); 
//			}
//		} catch (IllegalAccessException e) {
//			MiscUtil.log(Level.WARNING, "Exception thrown when finding opsSet: " + e.getClass() + ": " + e.getMessage());
//		}
//
//		if (opsSet != null) {
//			if (opsSet.contains(player.getName().toLowerCase())) {
//				// player is already an Op
//				opsSet = null;
//			} else {
//				opsSet.add(player.getName().toLowerCase());
//			}
//			player.recalculatePermissions();
//			Debugger.getDebugger().debug("granted op to " + player.getName());
//		}
//		
//		return opsSet;
//	}
//
//	/**
//	 * Revoke ops status from a player
//	 * 
//	 * @param player	The player to revoke ops status from
//	 * @param opsSet	A set of all player names who currently have ops status, as returned by @see #grantOpStatus(Player)
//	 */
//	public static void revokeOpStatus(Player player, Set<String> opsSet) {
//		if (opsSet != null) {
//			opsSet.remove(player.getName().toLowerCase());
//			player.recalculatePermissions();
//			Debugger.getDebugger().debug("revoked op from " + player.getName());
//		}
//	}
}
