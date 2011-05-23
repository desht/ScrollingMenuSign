package me.desht.scrollingmenusign;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;

public class SMSPlayerListener extends PlayerListener {
	private ScrollingMenuSign plugin;
	
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
			if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
				Sign sign = (Sign) b.getState();
				if (sign.getLine(0).equals("scrollingmenu")) {
					if (plugin.isAllowedTo(player, "scrollingmenusign.commands.create")) {
						String name = sign.getLine(1);
						String title = sign.getLine(2);
						if (name.length() > 0 && title.length() > 0) {
							if (plugin.getMenu(name) != null) {
								plugin.error_message(player, "A menu called '" + name + "' already exists.");
								return;
							}
							if (plugin.getMenuName(b.getLocation()) != null) {
								plugin.error_message(player, "There is already a menu attached to that sign.");
								return;
							}
							SMSMenu menu = new SMSMenu(name, title, player.getName(), b.getLocation());
							plugin.addMenu(name, menu, true);
							plugin.status_message(player, "Created new menu sign: " + name);
						} 
					} else {
						plugin.error_message(player, "You are not allowed to do that.");
					}	
				} 
			}
			return;
		}
		
		// ok, it's a sign, and there's a menu on it
		SMSMenu menu = plugin.getMenu(menuName);
		
		if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
			// left-click selects the current menu entry and runs the associated command
			if (!plugin.isAllowedTo(player, "scrollingmenusign.execute")) {
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
		} else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			if (!plugin.isAllowedTo(player, "scrollingmenusign.scroll")) {
				plugin.error_message(player, "You are not allowed to scroll through menu signs");
				return;
			}
			// right-click cycles to the next entry in the menu
			menu.nextItem();
			menu.updateSign();
		}
		
	}
	
}
