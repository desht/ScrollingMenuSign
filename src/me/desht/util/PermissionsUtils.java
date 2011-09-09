package me.desht.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import me.desht.scrollingmenusign.SMSException;
import net.minecraft.server.ServerConfigurationManager;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.bukkit.PermissionsEx;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;
import com.platymuus.bukkit.permissions.Group;
import com.platymuus.bukkit.permissions.PermissionInfo;
import com.platymuus.bukkit.permissions.PermissionsPlugin;

import de.bananaco.permissions.interfaces.PermissionSet;
import de.bananaco.permissions.worlds.WorldPermissionsManager;

public class PermissionsUtils {
	private static PermissionHandler permissionHandler = null;	// Permissions 2.x/3.x
	private static PermissionsPlugin permissionsBukkit = null;	// PermissionsBukkit
	private static PermissionManager permissionManager = null;	// PermissionsEx
	private static WorldPermissionsManager wpm = null;			// bPermissions

	private PermissionsUtils() {	
	}

	public static void setup() {
		PluginManager pm = Bukkit.getServer().getPluginManager();

		Plugin permissionsBukkitPlugin = pm.getPlugin("PermissionsBukkit");
		if (permissionsBukkitPlugin != null && permissionsBukkit == null) {
			permissionsBukkit = (PermissionsPlugin) permissionsBukkitPlugin;
			MiscUtil.log(Level.INFO, "PermissionsBukkit detected");
			return;
		}

		Plugin permissionsPlugin = pm.getPlugin("Permissions");
		if (permissionsPlugin != null && permissionHandler == null) {
			permissionHandler = ((Permissions) permissionsPlugin).getHandler();
			MiscUtil.log(Level.INFO, "Permissions detected");
			return;
		}

		Plugin permissionsExPlugin = pm.getPlugin("PermissionsEx");
		if (permissionsExPlugin != null && permissionManager == null) {
			permissionManager = PermissionsEx.getPermissionManager();
			MiscUtil.log(Level.INFO, "PermissionsEx detected");
			return;
		}

		Plugin bPermissionsPlugin = pm.getPlugin("bPermissions");
		if (bPermissionsPlugin != null && wpm == null) {
			wpm = de.bananaco.permissions.Permissions.getWorldPermissionsManager();
			MiscUtil.log(Level.INFO, "bPermissions detected");
			return;
		}

		MiscUtil.log(Level.INFO, "No Permissions plugin detected - command elevation/restriction not available.");
		MiscUtil.log(Level.INFO, "Using built-in Bukkit superperms for permissions.");
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
		// if Permissions 2.x/3.x is active, then use that
		if (permissionHandler != null) {
			return permissionHandler.has(player, node);
		} else {
			return player.hasPermission(node);
		}
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
		} else if (permissionHandler != null) {
			String[] groups = permissionHandler.getGroups(player.getWorld().getName(), player.getName());
			if (groups != null) {
				for (String s : groups) {
					if (s != null && s.equalsIgnoreCase(group))
						return true;
				}
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


	/**
	 * Elevate the permissions of player to match those of target
	 *
	 * @param player	The player to elevate
	 * @param target	The target player to copy the permissions from
	 * @return			List of temporary nodes which were granted
	 */
	public static List<String> elevate(Player player, String target) {
		List<String> tempPerms = new ArrayList<String>();

		if (wpm != null) {
			// bPermissions does things a bit differently - we copy the target's group(s)
			// to the player, not permission nodes
			PermissionSet ps = wpm.getPermissionSet(player.getWorld());
			for (String grp : ps.getGroups(target)) {
				if (!isInGroup(player, grp)) {
					ps.addGroup(player, grp);
					tempPerms.add(grp);
				}
			}
			return tempPerms;
		} else {
			for (String perm : getPermissionNodes(target, player.getWorld())) {
				if (!isAllowedTo(player, perm))
					tempPerms.add(perm);
			}
		}

		for (String perm: tempPerms) {
			Debugger.getDebugger().debug("grant perm " + perm);
			if (permissionsBukkit != null) {
				// PermissionsBukkit has no API call to modify permissions - use a console command to do it
				String cmd = String.format("permissions player setperm %s %s true", player.getName(), perm);
				QuietConsoleCommandSender console = new QuietConsoleCommandSender(Bukkit.getServer());
				Bukkit.getServer().dispatchCommand(console, cmd);
			} else if (permissionHandler != null) {
				com.nijiko.permissions.User user = permissionHandler.getUserObject(player.getWorld().getName(), player.getName());
				if (user != null) {
					user.addPermission(perm);
				}
			} else if (permissionManager != null) {
				PermissionUser user = permissionManager.getUser(player.getName());
				if (user != null) {
					user.addPermission(perm, player.getWorld().getName());
				}
			} else if (wpm != null) {
				PermissionSet ps = wpm.getPermissionSet(player.getWorld());
				for (String grp : tempPerms) {
					ps.addGroup(player, grp);
				}
			}
		}

		return tempPerms;
	}

	/**
	 * De-elevate a player's permissions.
	 * 
	 * @param player	The player to de-elevate
	 * @param tempPerms	The list of permission nodes to remove
	 */
	public static void deElevate(Player player, List<String> tempPerms) {
		if (tempPerms == null)
			return;

		QuietConsoleCommandSender console = new QuietConsoleCommandSender(Bukkit.getServer());
		for (String perm : tempPerms) {

			Debugger.getDebugger().debug("withdraw perm " + perm);

			if (permissionsBukkit != null) {		
				String cmd = String.format("permissions player unsetperm %s %s true", player.getName(), perm);
				Bukkit.getServer().dispatchCommand(console, cmd);
			} else if (permissionHandler != null) {
				com.nijiko.permissions.User user = permissionHandler.getUserObject(player.getWorld().getName(), player.getName());
				if (user != null) {
					user.removePermission(perm);
				}
			} else if (permissionManager != null) {
				PermissionUser user = permissionManager.getUser(player.getName());
				if (user != null) {
					user.removePermission(perm, player.getWorld().getName());
				}
			} else if (wpm != null) {
				// bPermissions does things a bit differently -
				// we need to remove groups from the player rather than permission nodes
				PermissionSet ps = wpm.getPermissionSet(player.getWorld());
				for (String grp : tempPerms) {
					ps.removeGroup(player, grp);
				}
			}
		}
	}

	/**
	 * Get a full list of the player's permission nodes.
	 * 
	 * @param playerName	Name of the player to check for
	 * @param w				Player's world (use first known world if null is passed)
	 * @return				List of permission strings
	 */
	public static List<String> getPermissionNodes(String playerName, World w) {
		if (w == null)
			w = Bukkit.getServer().getWorlds().get(0);

		List<String> res = null;
		if (permissionsBukkit != null) {
			Map<String, Boolean> perms;
			PermissionInfo info = permissionsBukkit.getPlayerInfo(playerName);
			try {
				// this call currently throws an NPE if no explicit permissions defined
				perms = info.getPermissions();
			} catch (NullPointerException e) {
				perms = new HashMap<String, Boolean>();
			}
			for (Group grp : info.getGroups()) {
				PermissionInfo gInfo = grp.getInfo();
				try {
					// this call currently throws an NPE if no explicit permissions defined
					Map<String, Boolean> gPerms = gInfo.getPermissions();
					for (Entry<String, Boolean> e : gPerms.entrySet()) {
						perms.put(e.getKey(), e.getValue());
					}
				} catch (NullPointerException e) {
				}
			}
			res = new ArrayList<String>(perms.keySet());
		} else if (permissionHandler != null) {
			com.nijiko.permissions.User user = permissionHandler.getUserObject(w.getName(), playerName);
			if (user != null) {
				res = new ArrayList<String>();
				for (String s : user.getAllPermissions())
					res.add(s);
			}
		} else if (permissionManager != null) {
			PermissionUser user = permissionManager.getUser(playerName);
			if (user != null) {
				res = Arrays.asList(user.getPermissions(w.getName()));
			}
		} else if (wpm != null) {
			res = wpm.getPermissionSet(w).getPlayerNodes(playerName);
		}

		return res;
	}

	@SuppressWarnings("unchecked")
	public static Set<String> grantOpStatus(Player player) {
		Field opsSetField = null;
		Set<String> opsSet = null;

		try {
			// The private .h field in ServerConfigurationManager represents a 
			// set of player names who have Op status
			opsSetField = ServerConfigurationManager.class.getDeclaredField("h");
			opsSetField.setAccessible(true);
			opsSet = (Set<String>) opsSetField.get(((CraftServer)player.getServer()).getHandle());
		} catch (Exception e) {
			MiscUtil.log(Level.WARNING, "Exception thrown when finding opsSet: " + e.getMessage()); 
			return null;
		}

		if (opsSet != null) {
			if (opsSet.contains(player.getName().toLowerCase())) {
				// player is already an Op
				opsSet = null;
			} else {
				opsSet.add(player.getName().toLowerCase());
			}
		}
		player.recalculatePermissions();
		Debugger.getDebugger().debug("granted op to " + player.getName());
		return opsSet;
	}
	
	public static void revokeOpStatus(Player player, Set<String> opsSet) {
		if (opsSet != null) {
            opsSet.remove(player.getName().toLowerCase());
        }
		player.recalculatePermissions();
		Debugger.getDebugger().debug("revoked op from " + player.getName());
	}
}
