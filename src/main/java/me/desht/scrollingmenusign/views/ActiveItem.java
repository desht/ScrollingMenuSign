package me.desht.scrollingmenusign.views;

import java.util.ArrayList;
import java.util.List;

import me.desht.dhutils.LogUtils;
import me.desht.dhutils.PermissionUtils;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSMenuItem;
import me.desht.scrollingmenusign.SMSValidate;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.enums.SMSUserAction;

import org.bukkit.ChatColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Represents an item linked to a SMS menu.  Not a traditional view, since all the
 * information needed is held in the item's metadata, not in a SMSView subclassed object.
 */
public class ActiveItem {
	private static final String MENU_MARKER = ChatColor.BLACK + "\u2637";
	private static final String SEPARATOR = " \u2237 " + ChatColor.RESET;

	private ItemStack stack;
	private final SMSMenu menu;
	private int selectedItem;

	private ActiveItem(ItemStack item) {
		this.stack = item;
		ItemMeta meta = item.getItemMeta();
		List<String> lore = meta.getLore();
		if (!lore.isEmpty() && meta.getDisplayName() != null) {
			String last = lore.get(lore.size() - 1);
			if (last.startsWith(MENU_MARKER)) {
				menu = SMSMenu.getMenu(last.substring(MENU_MARKER.length()));
				String[] fields = meta.getDisplayName().split(SEPARATOR);
				SMSValidate.isTrue(fields.length == 2, "Item name is not correctly formed");
				int sel = menu.indexOfItem(fields[1]);
				selectedItem = sel == -1 ? 0 : sel;
			} else {
				throw new SMSException("Item is not linked to a SMS Menu");
			}
		} else {
			throw new SMSException("Item is not linked to a SMS Menu");
		}
	}

	private ActiveItem(ItemStack stack, SMSMenu menu) {
		this.stack = stack;
		this.menu = menu;
		this.selectedItem = 1;
		buildItemStack();
	}

	private void buildItemStack() {
		ItemMeta meta = stack.getItemMeta();
		SMSMenuItem menuItem = menu.getItemAt(selectedItem);
		meta.setDisplayName(menu.getTitle() + SEPARATOR + menuItem.getLabel());
		List<String> lore = new ArrayList<String>();
		for (String l : menuItem.getLore()) {
			lore.add(l);
		}
		lore.add(MENU_MARKER + menu.getName());
		meta.setLore(lore);
		stack.setItemMeta(meta);
		if (ScrollingMenuSign.getInstance().isProtocolLibEnabled()) {
			// fake glow; we'll use a flag enchantment here to signify that
			stack.addUnsafeEnchantment(Enchantment.SILK_TOUCH, 32);
		}
	}

	public SMSMenu getMenu() {
		return menu;
	}

	public int getSelectedItemIndex() {
		return selectedItem;
	}

	public void execute(Player player) {
		PermissionUtils.requirePerms(player, "scrollingmenusign.use.item");
		if (!menu.hasOwnerPermission(player)) {
			throw new SMSException("This menu is owned by someone else");
		}
		SMSMenuItem item = menu.getItemAt(selectedItem);
		LogUtils.fine("ActiveItem: about to execute: " + item);
		if (item != null) {
			item.executeCommand(player);
		} else {
			LogUtils.warning("index " + selectedItem + " out of range for " + menu.getName());
		}
	}

	public void scroll(Player player, int delta) {
		PermissionUtils.requirePerms(player, "scrollingmenusign.use.item");
		if (!menu.hasOwnerPermission(player)) {
			throw new SMSException("This menu is owned by someone else");
		}
		selectedItem += delta;
		if (selectedItem > menu.getItemCount()) {
			selectedItem = 1;
		} else if (selectedItem < 1) {
			selectedItem = menu.getItemCount();
		}
		buildItemStack();
	}

	public ItemStack toItemStack() {
		return stack;
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
		return "[" + stack.getType() + ":" + menu.getName() + "/" + selectedItem + "]";
	}

	/**
	 * Check if the player is holding an active item object.
	 *
	 * @param player the player to check
	 * @return true if the player is holding an active item, false otherwise
	 */
	public static boolean holdingActiveItem(Player player) {
		ItemStack stack = player.getItemInHand();
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

	/**
	 * Get the active item currently held by the player.
	 *
	 * @param player the player holding the item
	 * @return the ActiveItem object
	 * @throws SMSException if the player is not holding an active item
	 */
	public static ActiveItem getActiveItem(Player player) {
		return new ActiveItem(player.getItemInHand());
	}

	public static ActiveItem makeActiveItem(Player player, SMSMenu menu) {
		ActiveItem item = new ActiveItem(player.getItemInHand(), menu);
		player.setItemInHand(item.toItemStack());
		return item;
	}

	public static boolean deactivateItem(Player player) {
		if (!holdingActiveItem(player)) {
			return false;
		}
		ItemStack stack = player.getItemInHand();
		ItemMeta meta = stack.getItemMeta();
		meta.setDisplayName(null);
		meta.setLore(null);
		stack.setItemMeta(meta);
		if (stack.getEnchantmentLevel(Enchantment.SILK_TOUCH) == 32) {
			stack.removeEnchantment(Enchantment.SILK_TOUCH);
		}
		return true;
	}
}
