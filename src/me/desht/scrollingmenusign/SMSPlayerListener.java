package me.desht.scrollingmenusign;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
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
		
		String menuName = plugin.getMenuNameAt(b.getLocation());
		if (menuName == null) {
			// No menu attached to this sign, but a left-click could create a new menu if the sign's
			// text is in the right format...
			if (event.getAction() == Action.LEFT_CLICK_BLOCK && player.getItemInHand().getTypeId() == 0) {
				tryToActivateSign(b, player); 
			}
			return;
		}
		
		// ok, it's a sign, and there's a menu on it
		try {
			plugin.debug("player interact event @ " + b.getLocation() + ", " + player.getName() + " did " + event.getAction() + ", menu=" + menuName);
			SMSMenu menu = plugin.getMenu(menuName);		
			String sneak = player.isSneaking() ? "sneak" : "normal";
			Configuration config = plugin.getConfiguration();
			String action = "none";
			if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
				action = config.getString("sms.actions.leftclick." + sneak);
			} else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
				action = config.getString("sms.actions.rightclick." + sneak);
			}
			processAction(action, player, menu, b.getLocation());
		} catch (SMSNoSuchMenuException e) {
			plugin.error_message(player, e.getError());
		}
	}

	private void processAction(String action, Player p, SMSMenu menu, Location l) {
		if (action.equalsIgnoreCase("execute")) {
			executeMenu(p, menu, l);
		} else if (action.equalsIgnoreCase("scrolldown")) {
			scrollMenu(p, menu, l, ScrollDirection.SCROLL_DOWN);
		} else if (action.equalsIgnoreCase("scrollup")) {
			scrollMenu(p, menu, l, ScrollDirection.SCROLL_UP);
		}
	}
	
	private void scrollMenu(Player player, SMSMenu menu, Location l, ScrollDirection dir) {
		if (!plugin.isAllowedTo(player, "scrollingmenusign.scroll", true)) {
			plugin.error_message(player, "You are not allowed to scroll through menu signs");
			return;
		}
		if (dir == ScrollDirection.SCROLL_DOWN) {
			menu.nextItem(l);
		} else if (dir == ScrollDirection.SCROLL_UP) {
			menu.prevItem(l);
		}
		menu.updateSign(l);
	}

	private void executeMenu(Player player, SMSMenu menu, Location l) {
		if (!plugin.isAllowedTo(player, "scrollingmenusign.execute", true)) {
			plugin.error_message(player, "You are not allowed to execute menu sign commands");
			return;
		}
		SMSMenuItem item = menu.getCurrentItem(l);
		if (item != null) {
			String command = item.getCommand();
			plugin.getMacroHandler().executeCommand(command, player);
			sendFeedback(player, item.getMessage(), new HashSet<String>());
		}
	}
	
	private void sendFeedback(Player player, String message, Set<String> history) {
		if (message == null || message.length() == 0)
			return;
		if (message.length() > 1 && message.startsWith("%")) {
			// macro expansion
			String macro = message.substring(1);
			if (history.contains(macro)) {
				plugin.log(Level.WARNING, "sendFeedback [" + macro + "]: recursion detected");
				plugin.error_message(player, "Recursive loop detected in macro " + macro + "!");
				return;
			} else if (plugin.getMacroHandler().hasCommand(macro)) {
				history.add(macro);
				sendFeedback(player, plugin.getMacroHandler().getCommands(macro), history);
			} else {
				plugin.error_message(player, "No such macro '" + macro + "'.");
			}
		} else {
			player.sendMessage(ChatColor.YELLOW + plugin.parseColourSpec(null, message));
		}	
	}

	private void sendFeedback(Player player, List<String> messages, Set<String> history) {
		for (String m : messages) {
			sendFeedback(player, m, history);
		}
	}
	
	private void tryToActivateSign(Block b, Player player) {
		Sign sign = (Sign) b.getState();
		if (!sign.getLine(0).equals("[sms]"))
		return;

		String name = sign.getLine(1);
		String title = plugin.parseColourSpec(player, sign.getLine(2));
		if (name.length() > 0) {
			if (plugin.checkForMenu(name)) {
				if (title.length() == 0) {
					if (!plugin.isAllowedTo(player, "scrollingmenusign.commands.sync")) {
						plugin.error_message(player, "You are not allowed to add signs to scrolling menus.");
						return;
					}
					try {
						plugin.syncMenu(name, b.getLocation());
					} catch (SMSNoSuchMenuException e) {
						plugin.error_message(player, e.getError());
					}
					plugin.status_message(player, "Added sign to existing menu: " + name);
				} else {
					plugin.error_message(player, "A menu called '" + name + "' already exists.");
				}
			} else if (plugin.getMenuNameAt(b.getLocation()) != null) {
				plugin.error_message(player, "There is already a menu attached to that sign.");
				return;
			} else if (title.length() > 0) {
				if (!plugin.isAllowedTo(player, "scrollingmenusign.commands.create")) {
					plugin.error_message(player, "You are not allowed to create scrolling menu signs.");
					return;
				}
				SMSMenu menu = new SMSMenu(plugin, name, title, player.getName(), b.getLocation());
				plugin.addMenu(name, menu, true);
				plugin.status_message(player, "Created new menu sign: " + name);
			}
			plugin.maybeSaveMenus();
		}
	}
	
	@Override
	public void onItemHeldChange(PlayerItemHeldEvent event) {
		Player p = event.getPlayer();

		String menuName = null;
		SMSMenu menu = null;
		try {
			menuName = plugin.getTargetedMenuSign(p, false);
			if (menuName == null) return;		
			menu = plugin.getMenu(menuName);
		} catch (SMSNoSuchMenuException e) {
			plugin.log(Level.WARNING, e.getError());
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
		Block b = p.getTargetBlock(null, 3);
		if (b.getType() == Material.SIGN_POST || b.getType() == Material.WALL_SIGN) {
			processAction(action, p, menu, b.getLocation());
		}
	}
	
}
