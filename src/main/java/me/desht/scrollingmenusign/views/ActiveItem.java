package me.desht.scrollingmenusign.views;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.desht.dhutils.LogUtils;
import me.desht.dhutils.PermissionUtils;
import me.desht.scrollingmenusign.ItemGlow;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSMenuItem;
import me.desht.scrollingmenusign.SMSValidate;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.enums.SMSUserAction;

import org.bukkit.ChatColor;
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

	private ItemStack stack;
	private final List<SMSMenu> menus = new ArrayList<SMSMenu>();
	private final List<Integer> selected = new ArrayList<Integer>();

	/**
	 * Get the active item object from an item with existing active item metadata.
	 *
	 * @param item the item to retrieve the active item for
	 * @throws SMSException if the item's metadata does not point to an active item
	 */
	public ActiveItem(ItemStack item) {
		this.stack = item;
		ItemMeta meta = item.getItemMeta();
		List<String> lore = meta.getLore();
		if (!lore.isEmpty() && meta.getDisplayName() != null) {
			String last = lore.get(lore.size() - 1);
			if (last.startsWith(MENU_MARKER) && last.length() > MENU_MARKER.length()) {
				SMSValidate.isTrue(meta.getDisplayName().contains(SEPARATOR), "Item name is not correctly formed");
				String[] menuPath = last.substring(MENU_MARKER.length()).split(SUBMENU_SEPARATOR);
				for (String menuName : menuPath) {
					String[] f = menuName.split(":");
					menus.add(SMSMenu.getMenu(f[0]));
					selected.add(Integer.parseInt(f[1]));
				}
			} else {
				throw new SMSException("Item is not an SMS active item");
			}
		} else {
			throw new SMSException("Item is not an SMS active item");
		}
	}

	/**
	 * Create a new active item object for the given itemstack and SMS menu.
	 *
	 * @param stack the item to turn into an active item
	 * @param menu the menu to associate the item with
	 */
	public ActiveItem(ItemStack stack, SMSMenu menu) {
		this.stack = stack;
		this.menus.add(menu);
		this.selected.add(1);
		buildItemStack();
	}

	private void buildItemStack() {
		ItemMeta meta = stack.getItemMeta();
		SMSMenuItem menuItem = getActiveMenuItemAt(null, getSelectedItem());
		List<String> lore = new ArrayList<String>();
		if (menuItem != null) {
			meta.setDisplayName(variableSubs(getActiveMenuTitle(null)) + SEPARATOR + variableSubs(menuItem.getLabel()));
			for (String l : menuItem.getLore()) {
				lore.add(l);
			}
		} else {
			meta.setDisplayName(getActiveMenuTitle(null) + SEPARATOR + NO_ITEMS);
		}
		List<String> names = new ArrayList<String>(menus.size());
		for (int i = 0; i < menus.size(); i++) {
			names.add(menus.get(i).getName() + ':' + selected.get(i));
		}
		lore.add(MENU_MARKER + Joiner.on(SUBMENU_SEPARATOR).join(names));
		meta.setLore(lore);
		stack.setItemMeta(meta);
		if (ScrollingMenuSign.getInstance().isProtocolLibEnabled()) {
			ItemGlow.addGlow(stack);
		}
	}

	/* (non-Javadoc)
	 * @see me.desht.scrollingmenusign.CommandTrigger#getActiveMenu(java.lang.String)
	 */
	@Override
	public SMSMenu getActiveMenu(String playerName) {
		return menus.get(menus.size() - 1);
	}

	@Override
	public SMSMenu getNativeMenu() {
		return menus.get(0);
	}

	public SMSMenu getActiveMenu() {
		return menus.get(menus.size() - 1);
	}

	public int getSelectedItem() {
		return selected.get(selected.size() - 1);
	}

	public void setSelectedItem(int idx) {
		selected.set(selected.size() - 1, idx);
	}

	public void execute(Player player) {
		if (getActiveMenuItemCount(null) == 0) {
			return;
		}
		PermissionUtils.requirePerms(player, "scrollingmenusign.use.item");
		if (!getActiveMenu().hasOwnerPermission(player)) {
			throw new SMSException("This menu is owned by someone else");
		}
		SMSMenuItem item = getActiveMenuItemAt(null, getSelectedItem());
		LogUtils.fine("ActiveItem: about to execute: " + item);
		if (item != null) {
			item.executeCommand(player, this);
		} else  {
			LogUtils.warning("index " + getSelectedItem() + " out of range for " + getActiveMenu().getName());
		}
	}

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

	public ItemStack toItemStack() {
		return stack;
	}

	public void deactivate() {
		ItemMeta meta = stack.getItemMeta();
		meta.setDisplayName(null);
		meta.setLore(null);
		stack.setItemMeta(meta);
		if (ScrollingMenuSign.getInstance().isProtocolLibEnabled()) {
			ItemGlow.removeGlow(stack);
		}
	}

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
	 * Check if the given item stack is an active item.
	 *
	 * @param the item to check
	 * @return true if the item is an active item, false otherwise
	 */
	public static boolean isActiveItem(ItemStack stack) {
		ItemMeta meta = stack.getItemMeta();
		if (meta == null || meta.getDisplayName() == null || !meta.getDisplayName().contains(SEPARATOR)) {
			return false;
		}
		List<String> lore = meta.getLore();
		if (lore == null || !lore.get(lore.size() - 1).startsWith(MENU_MARKER)) {
			return false;
		}
		return true;
	}

	@Override
	public void pushMenu(String playerName, SMSMenu newActive) {
		menus.add(newActive);
		selected.add(1);
		buildItemStack();
	}

	@Override
	public SMSMenu popMenu(String playerName) {
		SMSMenu popped = getActiveMenu();
		menus.remove(menus.size() - 1);
		selected.remove(selected.size() - 1);
		buildItemStack();
		return popped;
	}

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
}
