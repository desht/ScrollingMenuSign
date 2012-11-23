package me.desht.scrollingmenusign.views.icon;

import java.lang.ref.WeakReference;

import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.views.SMSInventoryView;
import me.desht.scrollingmenusign.views.SMSPopup;
import me.desht.scrollingmenusign.views.SMSView;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
 
public class IconMenu implements Listener, SMSPopup {
	private final SMSInventoryView view;
	private final WeakReference<Player> player;

	private int size;
	private ItemStack[] optionIcons;
	private String[] optionNames;
	
    public IconMenu(Player p, SMSInventoryView view) {
    	this.player = new WeakReference<Player>(p);
    	this.view = view;
    	
    	buildMenu();
    	
    	Bukkit.getPluginManager().registerEvents(this, ScrollingMenuSign.getInstance());
    }
    
    private void buildMenu() {
    	SMSMenu menu = getView().getMenu();
    	
    	int width = (Integer) getView().getAttribute(SMSInventoryView.WIDTH);
    	int nItems = menu.getItemCount();
    	int nRows = (nItems / width) + 1;
    	
    	size = Math.min(54, nRows * 9);
    	optionIcons = new ItemStack[size];
    	optionNames = new String[size];
    	
    	int xOff = (9 - width) / 2;
    	
    	for (int i = 1; i <= nItems; i++) {
    		int y = (i - 1) / width;
    		int pos = y * 9 + xOff + (i - 1) % width;
    		String label = menu.getItemAt(i).getLabel();
    		optionIcons[pos] = MiscUtil.setItemNameAndLore(new ItemStack(Material.APPLE), label, null); // TODO: sort this out
    		optionNames[pos] = label;
    	}
    }

    public Player getPlayer() {
    	return player.get();
    }
    
	@Override
	public SMSView getView() {
		return view;
	}

	@Override
	public boolean isPoppedUp() {
		return getPlayer() != null && getPlayer().getOpenInventory().getTitle().equals(getView().getMenu().getTitle());		
	}

	@Override
	public void repaint() {
		boolean up = isPoppedUp();
		popdown();
		buildMenu();
		if (up) {
			popup();
		}
	}

	@Override
	public void popup() {
		if (getPlayer() != null && !isPoppedUp()) {
			Inventory inventory = Bukkit.createInventory(getPlayer(), size, getView().getMenu().getTitle());
			for (int i = 0; i < size; i++) {
				inventory.setItem(i, optionIcons[i]);
			}
			getPlayer().openInventory(inventory);
		}
	}

	@Override
	public void popdown() {
		if (getPlayer() != null && isPoppedUp()) {
			getPlayer().closeInventory();
		}
	}

	@Override
	public void updateTitleJustification() {
		// do nothing here
	}
    
	private int getMenuIndex(int invSlot) {
		int width = (Integer) getView().getAttribute(SMSInventoryView.WIDTH);
		int xOff = (9 - width) / 2;
		
		int y = invSlot / 9;
		int x = invSlot % 9;
		
		return y * width + (x - xOff);
	}
	
    @EventHandler(priority=EventPriority.MONITOR)
    void onInventoryClick(InventoryClickEvent event) {
    	String name = getView().getMenu().getTitle();
    	
        if (event.getInventory().getTitle().equals(name)) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot >= 0 && slot < size && optionNames[slot] != null) {
                OptionClickEvent optionEvent = new OptionClickEvent((Player)event.getWhoClicked(), getMenuIndex(slot), optionNames[slot]);
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
   
    private void destroy() {
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
       
        public int getPosition() {
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
