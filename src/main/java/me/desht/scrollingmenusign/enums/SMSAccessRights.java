package me.desht.scrollingmenusign.enums;

import me.desht.dhutils.Debugger;
import me.desht.dhutils.PermissionUtils;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public enum SMSAccessRights {
    OWNER, GROUP, OWNER_GROUP, ANY;

    /**
     * Check if the given player is allowed to use an access-controlled object owned by the given owner.
     *
     * @param player    the player to check for
     * @param ownerId   the UUID of the object's owner
     * @param ownerName the name of the object's owner
     * @return true if the player may use it, false otherwise
     */
    public boolean isAllowedToUse(Player player, UUID ownerId, String ownerName, String group) {
        if (this == ANY || player.getUniqueId().equals(ownerId) || PermissionUtils.isAllowedTo(player, "scrollingmenusign.access.any")) {
            return true;
        }

        boolean inGroup;
        switch (this) {
            case OWNER_GROUP:
                String primaryGroup = ScrollingMenuSign.getInstance().isVaultLegacyMode() ?
                        ScrollingMenuSign.permission.getPrimaryGroup(player.getWorld(), ownerName) :
                        ScrollingMenuSign.permission.getPrimaryGroup(player.getWorld().getName(), Bukkit.getOfflinePlayer(ownerId));
                inGroup = checkGroupMembership(player, primaryGroup);
                Debugger.getInstance().debug("OWNER_GROUP access check: owner = [" + ownerName + "], primary group = [" + primaryGroup + "], player ["
                        + player.getDisplayName() + "] in group: " + inGroup);
                return inGroup;
            case GROUP:
                inGroup = checkGroupMembership(player, group);
                Debugger.getInstance().debug("GROUP access check: group = [" + group + "], player [" + player.getDisplayName() + "] in group: " + inGroup);
                return inGroup;
            default:
                return false;
        }
    }

    private boolean checkGroupMembership(Player player, String group) {
        if (group == null || group.isEmpty() || ScrollingMenuSign.permission == null) {
            return false;
        } else if (ScrollingMenuSign.getInstance().isVaultLegacyMode()) {
            return ScrollingMenuSign.permission.playerInGroup(player.getWorld(), player.getName(), group);
        } else {
            return ScrollingMenuSign.permission.playerInGroup(player.getWorld().getName(), player, group);
        }
    }
}
