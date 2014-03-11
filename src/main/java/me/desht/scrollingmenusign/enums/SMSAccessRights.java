package me.desht.scrollingmenusign.enums;

import me.desht.dhutils.Debugger;
import me.desht.dhutils.PermissionUtils;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import org.bukkit.entity.Player;

public enum SMSAccessRights {
	OWNER, GROUP, OWNER_GROUP, ANY;

	/**
	 * Check if the given player is allowed to use an access-controlled object owned by the given owner.
	 *
	 * @param player the player to check for
	 * @param owner  the name of the owner of the object
	 * @return true if the player may use it, false otherwise
	 */
	public boolean isAllowedToUse(Player player, String owner, String group) {
		if (player.getName().equalsIgnoreCase(owner) || PermissionUtils.isAllowedTo(player, "scrollingmenusign.access.any")) {
			return true;
		}

		boolean inGroup;
		switch (this) {
			case ANY:
				return true;
			case OWNER_GROUP:
				String primaryGroup = ScrollingMenuSign.permission.getPrimaryGroup(player.getWorld().getName(), owner);
				inGroup = checkGroupMembership(player, primaryGroup);
				Debugger.getInstance().debug("OWNER_GROUP access check: owner = [" + owner + "], primary group = [" + primaryGroup + "], player ["
						+ player.getName() + "] in group: " + inGroup);
				return inGroup;
			case GROUP:
				inGroup = checkGroupMembership(player, group);
				Debugger.getInstance().debug("GROUP access check: group = [" + group + "], player [" + player.getName() + "] in group: " + inGroup);
				return inGroup;
			default:
				return false;
		}
	}

	private boolean checkGroupMembership(Player player, String group) {
		return group != null &&
				!group.isEmpty() &&
				ScrollingMenuSign.permission != null &&
				ScrollingMenuSign.permission.playerInGroup(player.getWorld(), player.getName(), group);
	}
}
