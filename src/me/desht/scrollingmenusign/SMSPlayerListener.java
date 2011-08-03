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
		
		String menuName = SMSMenu.getMenuNameAt(b.getLocation());
		try {
			if (menuName == null) {
				// No menu attached to this sign, but a left-click could create a new menu if the sign's
				// text is in the right format...
				if (event.getAction() == Action.LEFT_CLICK_BLOCK && player.getItemInHand().getTypeId() == 0) {
					tryToActivateSign(b, player); 
				}
				return;
			}
		} catch (SMSException e) {
			SMSUtils.errorMessage(player, e.getMessage());
		}
		
		// ok, it's a sign, and there's a menu on it
		try {
			plugin.debug("player interact event @ " + b.getLocation() + ", " + player.getName() + " did " + event.getAction() + ", menu=" + menuName);
			SMSMenu menu = SMSMenu.getMenu(menuName);		
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
			SMSUtils.errorMessage(player, e.getError());
		} catch (SMSException e) {
			SMSUtils.errorMessage(player, e.getMessage());
		}
	}

	private void processAction(String action, Player p, SMSMenu menu, Location l) throws SMSException {
		if (action.equalsIgnoreCase("execute")) {
			executeMenu(p, menu, l);
		} else if (action.equalsIgnoreCase("scrolldown")) {
			scrollMenu(p, menu, l, ScrollDirection.SCROLL_DOWN);
		} else if (action.equalsIgnoreCase("scrollup")) {
			scrollMenu(p, menu, l, ScrollDirection.SCROLL_UP);
		}
	}
	
	private void scrollMenu(Player player, SMSMenu menu, Location l, ScrollDirection dir) throws SMSException {
		SMSPermissions.requirePerms(player, "scrollingmenusign.scroll");
		if (dir == ScrollDirection.SCROLL_DOWN) {
			menu.nextItem(l);
		} else if (dir == ScrollDirection.SCROLL_UP) {
			menu.prevItem(l);
		}
		menu.updateSign(l);
	}

	private void executeMenu(Player player, SMSMenu menu, Location l) throws SMSException {
		SMSPermissions.requirePerms(player, "scrollingmenusign.execute");
		
		SMSMenuItem item = menu.getCurrentItem(l);
		if (item != null) {
			String command = item.getCommand();
			plugin.macroHandler.executeCommand(command, player);
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
				SMSUtils.log(Level.WARNING, "sendFeedback [" + macro + "]: recursion detected");
				SMSUtils.errorMessage(player, "Recursive loop detected in macro " + macro + "!");
				return;
			} else if (plugin.macroHandler.hasCommand(macro)) {
				history.add(macro);
				sendFeedback(player, plugin.macroHandler.getCommands(macro), history);
			} else {
				SMSUtils.errorMessage(player, "No such macro '" + macro + "'.");
			}
		} else {
			player.sendMessage(ChatColor.YELLOW + SMSUtils.parseColourSpec(null, message));
		}	
	}

	private void sendFeedback(Player player, List<String> messages, Set<String> history) {
		for (String m : messages) {
			sendFeedback(player, m, history);
		}
	}
	
	private void tryToActivateSign(Block b, Player player) throws SMSException {
		Sign sign = (Sign) b.getState();
		if (!sign.getLine(0).equals("[sms]"))
			return;

		String name = sign.getLine(1);
		String title = SMSUtils.parseColourSpec(player, sign.getLine(2));
		if (name.length() > 0) {
			if (SMSMenu.checkForMenu(name)) {
				if (title.length() == 0) {
					SMSPermissions.requirePerms(player, "scrollingmenusign.commands.sync");
					try {
						SMSMenu menu = SMSMenu.getMenu(name);
						menu.addSign(b.getLocation());
						menu.autosave();
					} catch (SMSNoSuchMenuException e) {
						SMSUtils.errorMessage(player, e.getError());
					}
					SMSUtils.statusMessage(player, "Added sign to existing menu: " + name);
				} else {
					SMSUtils.errorMessage(player, "A menu called '" + name + "' already exists.");
				}
			} else if (SMSMenu.getMenuNameAt(b.getLocation()) != null) {
				SMSUtils.errorMessage(player, "There is already a menu attached to that sign.");
				return;
			} else if (title.length() > 0) {
				SMSPermissions.requirePerms(player, "scrollingmenusign.commands.create");
				SMSMenu menu = new SMSMenu(plugin, name, title, player.getName(), b.getLocation());
				SMSMenu.addMenu(name, menu, true);
				menu.autosave();

				SMSUtils.statusMessage(player, "Created new menu sign: " + name);
			}
		}
	}
	
	@Override
	public void onItemHeldChange(PlayerItemHeldEvent event) {
		Player player = event.getPlayer();

		String menuName = null;
		SMSMenu menu = null;
		try {
			menuName = SMSMenu.getTargetedMenuSign(player, false);
			if (menuName == null) return;		
			menu = SMSMenu.getMenu(menuName);
		} catch (SMSNoSuchMenuException e) {
			SMSUtils.log(Level.WARNING, e.getError());
			return;
		} catch (SMSException e) {
			SMSUtils.log(Level.WARNING, e.getMessage());
		}
		
		int delta = event.getNewSlot() - event.getPreviousSlot();
		String sneak = player.isSneaking() ? "sneak" : "normal";
		String action = "none";
		Configuration config = plugin.getConfiguration();
		if (delta == -1 || delta == 8) {
			action = config.getString("sms.actions.wheelup." + sneak);
		} else if (delta == 1 || delta == -8) {
			action = config.getString("sms.actions.wheeldown." + sneak);
		}
		Block b = player.getTargetBlock(null, 3);
		if (b.getType() == Material.SIGN_POST || b.getType() == Material.WALL_SIGN) {
			try {
				processAction(action, player, menu, b.getLocation());
			} catch (SMSException e) {
				SMSUtils.errorMessage(player, e.getMessage());
			}
		}
	}
	
}
