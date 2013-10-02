package me.desht.scrollingmenusign.views;

import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSMenuItem;
import me.desht.scrollingmenusign.ScrollingMenuSign;

/**
 * Represents any object which can cause a SMS command to be triggered, e.g. a view or an active item.
 */
public abstract class CommandTrigger {
	public abstract void pushMenu(String playerName, SMSMenu newActive);

	public abstract SMSMenu popMenu(String playerName);

	public abstract SMSMenu getNativeMenu();

	public abstract SMSMenu getActiveMenu(String playerName);

	public abstract String getName();

	/**
	 * Get the player context for operations such as view scrolling, active submenu etc.  For
	 * views which have a per-player context (e.g. maps), this is just the player name. For views
	 * with a global context (e.g. signs), a global pseudo-player handle can be used.
	 * <p/>
	 * Subclasses should override this as needed.
	 *
	 * @param playerName name of the player to check for
	 * @return the player context string
	 */
	protected String getPlayerContext(String playerName) {
		return playerName;
	}

	/**
	 * Get the title for the given player's currently active menu.
	 *
	 * @param playerName name of the player to check
	 * @return title of the active menu
	 */
	public String getActiveMenuTitle(String playerName) {
		playerName = getPlayerContext(playerName);

		SMSMenu activeMenu = getActiveMenu(playerName);
		String prefix = activeMenu == getNativeMenu() ? "" : ScrollingMenuSign.getInstance().getConfig().getString("sms.submenus.title_prefix");
		return prefix + activeMenu.getTitle();
	}

	/**
	 * Get the number of items in the given player's currently active menu.  Note that for non-native menus,
	 * this will be one greater than the actual menu size, because a synthetic "BACK" button is added.
	 *
	 * @param playerName name of the player to check
	 * @return the number of items in the active menu
	 */
	public int getActiveMenuItemCount(String playerName) {
		playerName = getPlayerContext(playerName);

		SMSMenu activeMenu = getActiveMenu(playerName);
		int count = activeMenu.getItemCount();
		if (activeMenu != getNativeMenu()) count++;    // adding a synthetic entry for the BACK item
		return count;
	}

	/**
	 * Get the menu item at the given position for the given player's currently active menu.
	 *
	 * @param playerName name of the player to check
	 * @param pos        position in the active menu
	 * @return the active menu item
	 */
	public SMSMenuItem getActiveMenuItemAt(String playerName, int pos) {
		playerName = getPlayerContext(playerName);

		SMSMenu activeMenu = getActiveMenu(playerName);
		if (activeMenu != getNativeMenu() && pos == activeMenu.getItemCount() + 1) {
			String label = ScrollingMenuSign.getInstance().getConfig().getString("sms.submenus.back_item.label", "&l<- BACK");
			String mat = ScrollingMenuSign.getInstance().getConfig().getString("sms.submenus.back_item.material", "irondoor");
			return new SMSMenuItem(activeMenu, MiscUtil.parseColourSpec(label), "BACK", "", mat);
		} else {
			return activeMenu.getItemAt(pos);
		}
	}

	/**
	 * Get the label for the menu item at the given position for the given player's currently active menu.  View variable
	 * substitution will have been performed on the returned label.
	 *
	 * @param playerName name of the player to check
	 * @param pos        position in the active menu
	 * @return the label of the active menu item
	 */
	public String getActiveItemLabel(String playerName, int pos) {
		return getActiveMenuItemAt(getPlayerContext(playerName), pos).getLabel();
	}

}
