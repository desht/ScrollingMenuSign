package me.desht.scrollingmenusign.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.desht.dhutils.Debugger;
import me.desht.dhutils.ItemGlow;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.PermissionUtils;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSMenuItem;
import me.desht.scrollingmenusign.SMSValidate;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.enums.SMSUserAction;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.google.common.base.Joiner;

/**
 * Represents an item linked to a SMS menu.  Not a traditional view, since all the
 * information needed is held in the item's metadata, not in a SMSView subclassed object.
 */
public class ActiveItem extends CommandTrigger {
	private static final String MENU_MARKER = ChatColor.BLACK + "\u2637";
	private static final String SEPARATOR = " \u2237 " + ChatColor.RESET;
	private static final String SUBMENU_SEPARATOR = " \u25b6 ";
	private static final String NO_ITEMS = ChatColor.ITALIC + "\u223c no entries";

	private final ItemStack stack;
	private final List<MenuPos> menus = new ArrayList<MenuPos>();

	/**
	 * Get the active item object from an item with existing active item metadata.
	 *
	 * @param stack the item to retrieve the active item for
	 * @throws SMSException if the item's metadata does not point to an active item
	 */
	public ActiveItem(ItemStack stack) {
		SMSValidate.isTrue(stack.getType() != Material.AIR, "You can't create an active item from air!");
		this.stack = stack;
		ItemMeta meta = stack.getItemMeta();
		SMSValidate.notNull(meta, "There was a problem getting item metadata for your " + stack.getType());
		List<String> lore = meta.getLore();

		SMSValidate.isTrue(lore != null && !lore.isEmpty() && meta.getDisplayName() != null, "Item is not an SMS active item");
		String last = lore.get(lore.size() - 1);
		SMSValidate.isTrue(last.startsWith(MENU_MARKER) && last.length() > MENU_MARKER.length(), "Item is not an SMS active item");
		SMSValidate.isTrue(meta.getDisplayName().contains(SEPARATOR), "Item name is not correctly formed");

		String[] menuPath = last.substring(MENU_MARKER.length()).split(SUBMENU_SEPARATOR);
		for (String menuName : menuPath) {
			String[] f = menuName.split(":");
			SMSValidate.isTrue(f.length == 2 && StringUtils.isNumeric(f[1]), "Item lore is not correctly formed");
			menus.add(new MenuPos(SMSMenu.getMenu(f[0]), Integer.parseInt(f[1])));
		}
	}

	/**
	 * Create a new active item object for the given itemstack and SMS menu.
	 *
	 * @param stack the item to turn into an active item
	 * @param menu  the menu to associate the item with
	 */
	public ActiveItem(ItemStack stack, SMSMenu menu) {
		SMSValidate.isTrue(stack.getType() != Material.AIR, "You can't create an active item from air!");
		this.stack = stack;
		this.menus.add(new MenuPos(menu, 1));
		buildItemStack();
	}

	private void buildItemStack() {
		ItemMeta meta = stack.getItemMeta();
		SMSValidate.notNull(meta, "There was a problem getting item metadata for your " + stack.getType());
		SMSMenuItem menuItem = getActiveMenuItemAt(null, getSelectedItem());
		List<String> lore = new ArrayList<String>();
		if (menuItem != null) {
			meta.setDisplayName(variableSubs(getActiveMenuTitle(null)) + SEPARATOR + variableSubs(menuItem.getLabel()));
			Collections.addAll(lore, menuItem.getLore());
		} else {
			meta.setDisplayName(getActiveMenuTitle(null) + SEPARATOR + NO_ITEMS);
		}
		List<String> names = new ArrayList<String>(menus.size());
		for (MenuPos menu : menus) {
			names.add(menu.menu.getName() + ':' + menu.pos);
		}
		lore.add(MENU_MARKER + Joiner.on(SUBMENU_SEPARATOR).join(names));
		meta.setLore(lore);
		stack.setItemMeta(meta);
		if (ScrollingMenuSign.getInstance().isProtocolLibEnabled()) {
			ItemGlow.setGlowing(stack, true);
		}
	}

	/* (non-Javadoc)
	 * @see me.desht.scrollingmenusign.CommandTrigger#getActiveMenu(java.lang.String)
	 */
	@Override
	public SMSMenu getActiveMenu(String playerName) {
		return menus.get(menus.size() - 1).menu;
	}

	/* (non-Javadoc)
	 * @see me.desht.scrollingmenusign.views.CommandTrigger#getNativeMenu()
	 */
	@Override
	public SMSMenu getNativeMenu() {
		return menus.get(0).menu;
	}

	/**
	 * Get the active menu for this ActiveItem object.
	 *
	 * @return the active menu
	 */
	public SMSMenu getActiveMenu() {
		return menus.get(menus.size() - 1).menu;
	}

	/**
	 * Get the currently selected item index for the active menu.
	 *
	 * @return the item index
	 */
	public int getSelectedItem() {
		return menus.get(menus.size() - 1).pos;
	}

	/**
	 * Change the currenty selected item index for the active menu.
	 *
	 * @param idx the new item index
	 */
	public void setSelectedItem(int idx) {
		menus.get(menus.size() - 1).pos = idx;
	}

	/**
	 * Execute the currently selected menu item for this ActiveItem object.
	 *
	 * @param player the player who is executing the menu item
	 */
	public void execute(Player player) {
		if (getActiveMenuItemCount(null) == 0) {
			return;
		}
		PermissionUtils.requirePerms(player, "scrollingmenusign.use.item");
		SMSValidate.isTrue(getActiveMenu().hasOwnerPermission(player), "This menu is owned by someone else");
		SMSMenuItem item = getActiveMenuItemAt(null, getSelectedItem());
		Debugger.getInstance().debug("ActiveItem: about to execute: " + item);
		if (item != null) {
			item.executeCommand(player, this);
		} else {
			LogUtils.warning("index " + getSelectedItem() + " out of range for " + getActiveMenu().getName());
		}
	}

	/**
	 * Scroll the menu for this ActiveItem object.
	 *
	 * @param player the player who is scrolling the menu
	 * @param delta  the number of slots to scroll forward or backward by
	 */
	public void scroll(Player player, int delta) {
		PermissionUtils.requirePerms(player, "scrollingmenusign.use.item");
		if (!getActiveMenu().hasOwnerPermission(player)) {
			throw new SMSException("This menu is owned by someone else");
		}
		setSelectedItem(getSelectedItem() + delta);
		if (getSelectedItem() > getActiveMenuItemCount(player.getName())) {
			setSelectedItem(1);
		} else if (getSelectedItem() < 1) {
			setSelectedItem(getActiveMenuItemCount(player.getName()));
		}
		buildItemStack();
	}

	/**
	 * Retrieve the ItemStack for this ActiveItem
	 *
	 * @return the item stack
	 */
	public ItemStack toItemStack() {
		return stack;
	}

	/**
	 * Deactivate this ActiveItem.  Subequent calls to {@link #toItemStack()} will
	 * return an ItemStack with no special meta data.
	 */
	public void deactivate() {
		ItemMeta meta = stack.getItemMeta();
		meta.setDisplayName(null);
		meta.setLore(null);
		stack.setItemMeta(meta);
		if (ScrollingMenuSign.getInstance().isProtocolLibEnabled()) {
			ItemGlow.setGlowing(stack, false);
		}
	}

	/**
	 * Process the given action on this ActiveItem for the given player.
	 *
	 * @param player the player who is carrying out the action
	 * @param action the action to carry out
	 */
	public void processAction(Player player, SMSUserAction action) {
		switch (action) {
			case EXECUTE:
				execute(player);
				break;
			case SCROLLDOWN:
				scroll(player, 1);
				player.setItemInHand(toItemStack());
				break;
			case SCROLLUP:
				scroll(player, -1);
				player.setItemInHand(toItemStack());
				break;
			default:
				break;
		}
	}

	@Override
	public String toString() {
		return "[" + stack.getType() + ":" + getActiveMenu().getName() + "/" + getSelectedItem() + "]";
	}

	/**
	 * Check if the given item stack is an ActiveItem.
	 *
	 * @param stack the item to check
	 * @return true if the item is an active item, false otherwise
	 */
	public static boolean isActiveItem(ItemStack stack) {
		return isActiveItem(stack.getItemMeta());
	}

	public static boolean isActiveItem(ItemMeta meta) {
		if (meta == null || meta.getDisplayName() == null || !meta.getDisplayName().contains(SEPARATOR)) {
			return false;
		}
		List<String> lore = meta.getLore();
		return !(lore == null || !lore.get(lore.size() - 1).startsWith(MENU_MARKER));
	}

	/* (non-Javadoc)
	 * @see me.desht.scrollingmenusign.views.CommandTrigger#pushMenu(java.lang.String, me.desht.scrollingmenusign.SMSMenu)
	 */
	@Override
	public void pushMenu(String playerName, SMSMenu newActive) {
		menus.add(new MenuPos(newActive, 1));
		buildItemStack();
	}

	/* (non-Javadoc)
	 * @see me.desht.scrollingmenusign.views.CommandTrigger#popMenu(java.lang.String)
	 */
	@Override
	public SMSMenu popMenu(String playerName) {
		SMSMenu popped = getActiveMenu();
		menus.remove(menus.size() - 1);
		buildItemStack();
		return popped;
	}

	/* (non-Javadoc)
	 * @see me.desht.scrollingmenusign.views.CommandTrigger#getName()
	 */
	@Override
	public String getName() {
		return "Active:" + stack.getType();
	}

	private static final Pattern viewVarSubPat = Pattern.compile("<\\$v:([A-Za-z0-9_\\.]+)=(.*?)>");

	private String variableSubs(String text) {
		Matcher m = viewVarSubPat.matcher(text);
		StringBuffer sb = new StringBuffer(text.length());
		while (m.find()) {
			String repl = m.group(2);
			m.appendReplacement(sb, Matcher.quoteReplacement(repl));
		}
		m.appendTail(sb);
		return sb.toString();
	}

	private class MenuPos {
		private final SMSMenu menu;
		private int pos;

		private MenuPos(SMSMenu menu, int pos) {
			this.menu = menu;
			this.pos = pos;
		}
	}
}
