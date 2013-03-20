package me.desht.scrollingmenusign.views.icon;

import me.desht.dhutils.LogUtils;
import me.desht.scrollingmenusign.SMSMenuItem;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.enums.ViewJustification;
import me.desht.scrollingmenusign.views.SMSInventoryView;
import me.desht.scrollingmenusign.views.SMSPopup;
import me.desht.scrollingmenusign.views.SMSView;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class IconMenu implements Listener, SMSPopup {
	private static final int INVENTORY_WIDTH = 9;
	private static final int MAX_INVENTORY_ROWS = 6;

	private final SMSInventoryView view;

	private int size = 0;
	private ItemStack[] optionIcons;
	private String[] optionNames;

	public IconMenu(SMSInventoryView view) {
		this.view = view;
		Bukkit.getPluginManager().registerEvents(this, ScrollingMenuSign.getInstance());
	}

	public int getSlots() {
		return optionIcons.length;
	}
	
	@Override
	public SMSView getView() {
		return view;
	}

	@Override
	public boolean isPoppedUp(Player p) {
		return p.getOpenInventory().getTitle().equals(getView().getActiveMenuTitle(p.getName()));		
	}

	@Override
	public void repaint() {
		getView().setDirty(true);
		for (Player p : Bukkit.getOnlinePlayers()) {
			if (isPoppedUp(p)) {
				popdown(p);
				popup(p);
			}
		}
	}

	@Override
	public void popup(Player p) {
		if (!isPoppedUp(p)) {
			String title = getView().variableSubs(getView().getActiveMenuTitle(p.getName()));
			if (size == 0 || getView().isDirty(p.getName())) {
				buildMenu(p);
			}
			Inventory inventory = Bukkit.createInventory(p, size, title);
			getView().setDirty(p.getName(), false);
			for (int i = 0; i < size; i++) {
				inventory.setItem(i, optionIcons[i]);
			}
			p.openInventory(inventory);
		}
	}

	@Override
	public void popdown(Player p) {
		if (isPoppedUp(p)) {
			p.closeInventory();
		}
	}

	private void buildMenu(Player p) {
		int width = (Integer) getView().getAttribute(SMSInventoryView.WIDTH);
		int nItems = getView().getActiveMenuItemCount(p.getName());
		int nRows = Math.min(MAX_INVENTORY_ROWS, ((nItems - 1) / width) + 1);
	
		size = INVENTORY_WIDTH * nRows;
		optionIcons = new ItemStack[size];
		optionNames = new String[size];
	
		int xOff = getXOffset(width);
	
		for (int i = 0; i < nItems; i++) {
			int row = i / width;
			int pos = row * INVENTORY_WIDTH + xOff + i % width;
			SMSMenuItem menuItem = getView().getActiveMenuItemAt(p.getName(), i + 1);
			ItemStack icon = menuItem.getIconMaterial().makeItemStack();
			String label = getView().variableSubs(menuItem.getLabel());
			ItemMeta im = icon.getItemMeta();
			im.setDisplayName(ChatColor.RESET + label);
			im.setLore(menuItem.getLoreAsList());
			icon.setItemMeta(im);
			optionIcons[pos] = icon;
			optionNames[pos] = menuItem.getLabel();
		}
		
		LogUtils.fine("built icon menu inventory for " + p.getName() + ": " + size + " slots");
	}

	private int getMenuIndexForSlot(int invSlot) {
		int width = (Integer) getView().getAttribute(SMSInventoryView.WIDTH);

		int row = invSlot / INVENTORY_WIDTH;
		int col = invSlot % INVENTORY_WIDTH;

		return row * width + (col - getXOffset(width)) + 1;
	}

	private int getXOffset(int width) {
		ViewJustification ij = view.getItemJustification();
		switch (ij) {
		case LEFT: return 0;
		case RIGHT: return INVENTORY_WIDTH - width;
		default: return (INVENTORY_WIDTH - width) / 2;
		}
	}
	
//	@EventHandler
//	void onInventoryClose(InventoryCloseEvent event) {
//		String playerName = event.getPlayer().getName();
//		LogUtils.fine("InventoryCloseEvent: player = " + playerName + ", view = " + getView().getName() +
//		              ", inventory name = " + event.getInventory().getTitle());
//	}

	@EventHandler(priority=EventPriority.MONITOR)
	void onInventoryClick(InventoryClickEvent event) {
		String playerName = event.getWhoClicked().getName();
		String name = getView().variableSubs(getView().getActiveMenuTitle(playerName));
		
		if (event.getInventory().getTitle().equals(name)) {
			LogUtils.fine("InventoryClickEvent: player = " + playerName + ", view = " + getView().getName() +
			              ", inventory name = " + event.getInventory().getTitle());

			event.setCancelled(true);
			int slot = event.getRawSlot();
			if (slot >= 0 && slot < size && optionNames[slot] != null) {
				OptionClickEvent optionEvent = new OptionClickEvent((Player)event.getWhoClicked(), getMenuIndexForSlot(slot), optionNames[slot]);
				view.onOptionClick(optionEvent);
				if (optionEvent.willClose()) {
					final Player p = (Player)event.getWhoClicked();
					Bukkit.getScheduler().scheduleSyncDelayedTask(ScrollingMenuSign.getInstance(), new Runnable() {
						public void run() {
							p.closeInventory();
						}
					}, 1);
				}
				if (optionEvent.willDestroy()) {
					destroy();
				}
			}
		}
	}

	public void destroy() {
		HandlerList.unregisterAll(this);
	}

	public interface OptionClickEventHandler {
		public void onOptionClick(OptionClickEvent event);       
	}

	public class OptionClickEvent {
		private final Player player;
		private final int index;
		private final String name;

		private boolean close;
		private boolean destroy;

		public OptionClickEvent(Player player, int index, String name) {
			this.player = player;
			this.index = index;
			this.name = name;
			this.close = true;
			this.destroy = false;
		}

		public Player getPlayer() {
			return player;
		}

		public int getIndex() {
			return index;
		}

		public String getName() {
			return name;
		}

		public boolean willClose() {
			return close;
		}

		public boolean willDestroy() {
			return destroy;
		}

		public void setWillClose(boolean close) {
			this.close = close;
		}

		public void setWillDestroy(boolean destroy) {
			this.destroy = destroy;
		}
	}
}
