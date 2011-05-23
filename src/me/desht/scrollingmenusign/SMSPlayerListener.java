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
		String menuName = plugin.getMenuName(b.getLocation());
		if (menuName == null) {
			return;
		}
		
		// ok, it's a sign, and there's a menu on it
		SMSMenu menu = plugin.getMenu(menuName);
		Player player = event.getPlayer();
		
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
