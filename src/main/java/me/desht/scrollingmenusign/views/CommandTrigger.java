package me.desht.scrollingmenusign.views;

import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSMenuItem;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Represents any object which can cause a SMS command to be triggered, e.g. a view or an active item.
 */
public abstract class CommandTrigger implements Comparable<CommandTrigger> {
    public abstract void pushMenu(Player player, SMSMenu newActive);

    public abstract SMSMenu popMenu(Player player);

    public abstract SMSMenu getNativeMenu();

    public abstract SMSMenu getActiveMenu(Player player);

    public abstract String getName();

    /**
     * Get the player context for operations such as view scrolling, active submenu etc.  For
     * views which have a per-player context (e.g. maps), this is just the player name. For views
     * with a global context (e.g. signs), a global pseudo-player handle can be used.
     * <p/>
     * Subclasses should override this as needed.
     *
     * @param player the player to check for
     * @return the player context ID
     */
    protected UUID getPlayerContext(Player player) {
        return player.getUniqueId();
    }

    /**
     * Get the title for the given player's currently active menu.
     *
     * @param player the player to check
     * @return title of the active menu
     */
    public String getActiveMenuTitle(Player player) {
        SMSMenu activeMenu = getActiveMenu(player);
        String prefix = activeMenu == getNativeMenu() ? "" : ScrollingMenuSign.getInstance().getConfig().getString("sms.submenus.title_prefix");
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
     * @param pos    position in the active menu
     * @return the active menu item
     */
    public SMSMenuItem getActiveMenuItemAt(Player player, int pos) {
        SMSMenu activeMenu = getActiveMenu(player);
        if (activeMenu != getNativeMenu() && pos == activeMenu.getItemCount() + 1) {
            String label = ScrollingMenuSign.getInstance().getConfig().getString("sms.submenus.back_item.label", "&l<- BACK");
            String backIcon = ScrollingMenuSign.getInstance().getConfig().getString("sms.submenus.back_item.material", "irondoor");
            return new SMSMenuItem.Builder(activeMenu, MiscUtil.parseColourSpec(label))
                    .withCommand("BACK")
                    .withIcon(backIcon)
                    .build();
        } else {
            return activeMenu.getItemAt(pos);
        }
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
        return getActiveMenuItemAt(player, pos).getLabel();
    }

    @Override
    public int compareTo(CommandTrigger other) {
        return this.getName().compareTo(other.getName());
    }
}
