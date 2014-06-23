package me.desht.scrollingmenusign.views;

import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PermissionUtils;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSMenuItem;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Represents any object which can cause a SMS command to be triggered, e.g. a view or an active item.
 */
public abstract class CommandTrigger implements Comparable<CommandTrigger> {
    public static final UUID GLOBAL_PLAYER_UUID = UUID.fromString("90e73940-ba41-11e3-a5e2-0800200c9a66");

    public abstract void pushMenu(Player player, SMSMenu newActive);

    public abstract SMSMenu popMenu(Player player);

    public abstract SMSMenu getNativeMenu();

    public abstract SMSMenu getActiveMenu(Player player);

    public abstract String getName();

    /**
     * Get the player context for operations such as view scrolling, active submenu etc.  For
     * views which have a per-player context (e.g. maps), this is just the player's UUID. For views
     * with a global context (e.g. signs), a global pseudo-player handle is used.
     *
     * @param player the player to check for
     * @return the player context ID
     */
    protected UUID getPlayerContext(Player player) {
        return player == null? GLOBAL_PLAYER_UUID : player.getUniqueId();
    }

    /**
     * Get the title for the given player's currently active menu.
     *
     * @param player the player to check
     * @return title of the active menu
     */
    public String getActiveMenuTitle(Player player) {
        SMSMenu activeMenu = getActiveMenu(player);
        String prefix = activeMenu == getNativeMenu() ? "" : ScrollingMenuSign.getInstance().getConfigCache().getSubmenuTitlePrefix();
        return prefix + activeMenu.getTitle();
    }

    /**
     * Get the number of items in the given player's currently active menu.  Note that for non-native menus,
     * this will be one greater than the actual menu size, because a synthetic "BACK" button is added.
     *
     * @param player the player to check
     * @return the number of items in the active menu
     */
    public int getActiveMenuItemCount(Player player) {
        SMSMenu activeMenu = getActiveMenu(player);
        int count = activeMenu.getItemCount();
        if (activeMenu != getNativeMenu())
            count++;    // adding a synthetic entry for the BACK item
        return count;
    }

    /**
     * Get the menu item at the given position for the given player's currently active menu.
     *
     * @param player the player to check
     * @param pos position in the active menu
     * @return the active menu item
     */
    public SMSMenuItem getActiveMenuItemAt(Player player, int pos) {
        SMSMenu activeMenu = getActiveMenu(player);
        if (activeMenu != getNativeMenu() && pos == activeMenu.getItemCount() + 1) {
            return makeSpecialBackItem(activeMenu);
        } else {
            return activeMenu.getItemAt(pos);
        }
    }

    /**
     * Get the menu item of the given label for the given player's currently active menu.
     *
     * @param player the player to check
     * @param label label of the desired item
     * @return the active menu item
     */
    public SMSMenuItem getActiveMenuItemByLabel(Player player, String label) {
        SMSMenu activeMenu = getActiveMenu(player);
        if (label.equals(ScrollingMenuSign.getInstance().getConfigCache().getSubmenuBackLabel())) {
            return makeSpecialBackItem(activeMenu);
        } else {
            return activeMenu.getItem(label);
        }
    }

    private SMSMenuItem makeSpecialBackItem(SMSMenu menu) {
        String label = ScrollingMenuSign.getInstance().getConfigCache().getSubmenuBackLabel();
        ItemStack backIcon = ScrollingMenuSign.getInstance().getConfigCache().getSubmenuBackIcon();
        return new SMSMenuItem.Builder(menu, label)
                .withCommand("BACK")
                .withIcon(backIcon)
                .build();
    }

    /**
     * Get the label for the menu item at the given position for the given player's currently active menu.  View variable
     * substitution will have been performed on the returned label.
     *
     * @param player the player to check
     * @param pos    position in the active menu
     * @return the label of the active menu item
     */
    public String getActiveItemLabel(Player player, int pos) {
        SMSMenuItem item = getActiveMenuItemAt(player, pos);
        if (!item.hasPermission(player)) {
            return MiscUtil.parseColourSpec(ScrollingMenuSign.getInstance().getConfig().getString("sms.hidden_menuitem_text", "???"));
        }
        return item.getLabel();
    }

    @Override
    public int compareTo(CommandTrigger other) {
        return this.getName().compareTo(other.getName());
    }
}
