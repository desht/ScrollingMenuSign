package me.desht.scrollingmenusign.enums;

import me.desht.dhutils.LogUtils;
import me.desht.dhutils.PermissionUtils;
import me.desht.scrollingmenusign.ScrollingMenuSign;

import org.bukkit.entity.Player;

public enum SMSAccessRights {
	OWNER, GROUP, ANY;

	public boolean isAllowedToUse(Player player, String owner) {
		if (this == ANY || player.getName().equalsIgnoreCase(owner) || PermissionUtils.isAllowedTo(player, "scrollingmenusign.access.any")) {
			return true;
		}
		if (this == GROUP) {
			if (ScrollingMenuSign.permission == null) {
				return false;
			}
			String group = ScrollingMenuSign.permission.getPrimaryGroup(player.getWorld().getName(), owner);
			boolean inGroup = ScrollingMenuSign.permission.playerInGroup(player.getWorld(), player.getName(), group);
			LogUtils.fine("group access check: owner = " + owner + ", primary group = " + group + ", " + player.getName() + " in group: " + inGroup);
			return inGroup;
		}
		return false;
	}
}
