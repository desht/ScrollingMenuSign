package me.desht.scrollingmenusign.enums;

import me.desht.dhutils.LogUtils;
import me.desht.dhutils.PermissionUtils;
import me.desht.scrollingmenusign.ScrollingMenuSign;

import org.bukkit.entity.Player;

public enum SMSAccessRights {
	OWNER, GROUP, ANY;

	public boolean isAllowedToUse(Player player, String owner) {
		switch (this) {
		case ANY:
			return true;
		case OWNER:
			return player.getName().equalsIgnoreCase(owner) || PermissionUtils.isAllowedTo(player, "scrollingmenusign.access.any");
		case GROUP:
			if (ScrollingMenuSign.permission == null) {
				return false;
			}
			if (PermissionUtils.isAllowedTo(player, "scrollingmenusign.access.any")) {
				return true;
			}
			String group = ScrollingMenuSign.permission.getPrimaryGroup(player.getWorld().getName(), owner);
			boolean inGroup = ScrollingMenuSign.permission.playerInGroup(player.getWorld(), player.getName(), group);
			LogUtils.fine("group access check: owner = " + owner + ", primary group = " + group + ", " + player.getName() + " in group: " + inGroup);
			return inGroup;
		}
		return false;
	}
}
