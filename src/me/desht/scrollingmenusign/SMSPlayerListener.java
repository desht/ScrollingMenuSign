package me.desht.scrollingmenusign;

import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.util.config.Configuration;

public class SMSPlayerListener extends PlayerListener {
	private ScrollingMenuSign plugin;
	private static enum ScrollDirection { SCROLL_UP, SCROLL_DOWN };
	
	public SMSPlayerListener(ScrollingMenuSign plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.isCancelled()) {
			return;
		}
		Block b = event.getClickedBlock();
		if (b == null) {
			return;
		}
		if (!(b.getState() instanceof Sign)) {
			return;
		}
		Player player = event.getPlayer();
		
		String menuName = plugin.getMenuName(b.getLocation());
		
		if (menuName == null) {
			// No menu attached to this sign, but a left-click could create a new menu if the sign's
			// text is in the right format...
			if (event.getAction() == Action.LEFT_CLICK_BLOCK && player.getItemInHand().getTypeId() == 0) {
				tryToActivateSign(b, player); 
			}
			return;
		}
		
		// ok, it's a sign, and there's a menu on it
		SMSMenu menu = plugin.getMenu(menuName);
		
		String sneak = player.isSneaking() ? "sneak" : "normal";
		Configuration config = plugin.getConfiguration();
		String action = "none";
		if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
			action = config.getString("sms.actions.leftclick." + sneak);
		} else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			action = config.getString("sms.actions.rightclick." + sneak);
		}
		processAction(action, player, menu);
	}

	private void processAction(String action, Player p, SMSMenu menu) {
		if (action.equalsIgnoreCase("execute")) {
			executeMenu(p, menu);
		} else if (action.equalsIgnoreCase("scrolldown")) {
			scrollMenu(p, menu, ScrollDirection.SCROLL_DOWN);
		} else if (action.equalsIgnoreCase("scrollup")) {
			scrollMenu(p, menu, ScrollDirection.SCROLL_UP);
		}
	}
	
	private void scrollMenu(Player player, SMSMenu menu, ScrollDirection dir) {
		if (!plugin.isAllowedTo(player, "scrollingmenusign.scroll", true)) {
			plugin.error_message(player, "You are not allowed to scroll through menu signs");
			return;
		}
		if (dir == ScrollDirection.SCROLL_DOWN) {
			menu.nextItem();
		} else if (dir == ScrollDirection.SCROLL_UP) {
			menu.prevItem();
		}
		menu.updateSign();
	}

	private void executeMenu(Player player, SMSMenu menu) {
		if (!plugin.isAllowedTo(player, "scrollingmenusign.execute", true)) {
			plugin.error_message(player, "You are not allowed to execute menu sign commands");
			return;
		}
		SMSMenuItem item = menu.getCurrentItem();
		if (item != null) {
			player.chat(item.getCommand());
			if (item.getMessage() != null && item.getMessage().length() > 0) {
				player.sendMessage(ChatColor.YELLOW + item.getMessage());
			}
		}
	}

	private void tryToActivateSign(Block b, Player player) {
		Sign sign = (Sign) b.getState();
		if (sign.getLine(0).equals("scrollingmenu")) {
			if (!plugin.isAllowedTo(player, "scrollingmenusign.commands.create")) {
				plugin.error_message(player, "You are not allowed to create scrolling menu signs.");
				return;
			}
			String name = sign.getLine(1);
			String title = plugin.parseColourSpec(player, sign.getLine(2));
			if (name.length() > 0 && title.length() > 0) {
				if (plugin.getMenu(name) != null) {
					plugin.error_message(player, "A menu called '" + name + "' already exists.");
				} else 	if (plugin.getMenuName(b.getLocation()) != null) {
					plugin.error_message(player, "There is already a menu attached to that sign.");
					return;
				} else {
					SMSMenu menu = new SMSMenu(name, title, player.getName(), b.getLocation());
					plugin.addMenu(name, menu, true);
					plugin.status_message(player, "Created new menu sign: " + name);
				}
			}
		}
	}
	
	@Override
	public void onItemHeldChange(PlayerItemHeldEvent event) {
		Player p = event.getPlayer();

		String menuName = plugin.getTargetedMenuSign(p, false);
		if (menuName == null) return;		
		SMSMenu menu = plugin.getMenu(menuName);
		if (menu == null) {
			plugin.log(Level.WARNING, "can't get the menu for '" + menuName + "'?");
			return;
		}
		
		int delta = event.getNewSlot() - event.getPreviousSlot();
		String sneak = p.isSneaking() ? "sneak" : "normal";
		String action = "none";
		Configuration config = plugin.getConfiguration();
		if (delta == -1 || delta == 8) {
			action = config.getString("sms.actions.wheelup." + sneak);
		} else if (delta == 1 || delta == -8) {
			action = config.getString("sms.actions.wheeldown." + sneak);
		}
		processAction(action, p, menu);
	}
	
}
