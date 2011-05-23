package me.desht.scrollingmenusign;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SMSCommandExecutor implements CommandExecutor {
	private ScrollingMenuSign plugin;
	
	public SMSCommandExecutor(ScrollingMenuSign plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label,
			String[] args) {
		if (!(sender instanceof Player)) {
			return true;
		}
    	Player player = ((Player) sender);

    	if (label.equalsIgnoreCase("sms")) {
    		if (args.length == 0) {
    			return false;
    		}
    		if (!plugin.isAllowedTo(player, "scrollingmenusign.commands." + args[0])) {
    			plugin.error_message(player, "You are not allowed to do that.");
    			return true;
    		}
    		if (args[0].equalsIgnoreCase("make")) {
    			makeSMSSign(player, args);
    		} else if (args[0].equalsIgnoreCase("break")) {
    	    	breakSMSSign(player, args);
    		} else if (args[0].equalsIgnoreCase("list")) {
    			listSMSSigns(player, args);
    		} else if (args[0].equalsIgnoreCase("show")) {
    			showSMSInfo(player, args);
    		} else if (args[0].equalsIgnoreCase("add")) {
    			addSMSItem(player, args);
    		} else if (args[0].equalsIgnoreCase("remove")) {
    			removeSMSItem(player, args);
    		} else if (args[0].equalsIgnoreCase("save")) {
    			saveSigns(player, args);
    		} else {
    			return false;
    		}
    	}
		return true;
	}

	private void saveSigns(Player player, String[] args) {
		plugin.save();
	}

	private void removeSMSItem(Player player, String[] args) {
		if (args.length < 3) {
			plugin.error_message(player, "Usage: /sms remove <menu-name> <item-index>");
			return;
		}
		String menuName = args[1];
		int index;
		try {
			index = Integer.parseInt(args[2]);
		} catch (NumberFormatException e) {
			plugin.error_message(player, "item index must be numeric");
			return;
		}
		
		SMSMenu menu = plugin.getMenu(menuName);
		if (menu == null) {
			plugin.error_message(player, "Unknown menu name: " + menuName);
			return;
		}
		menu.remove(index);
		menu.updateSign();
		plugin.status_message(player, "Menu entry #" + index + " removed from: " + menuName);
	}

	
	private void addSMSItem(Player player, String[] args) {	
		if (args.length < 3) {
			plugin.error_message(player, "Usage: /sms add <menu-name> <menu-entry>");
			return;
		}
			
		String menuName = args[1];
		String rest = combine(args, 2);
		String[] entry_args = rest.split("\\|");
		
		if (entry_args.length < 2) {
			plugin.error_message(player, "menu-entry must include at least entry label & command");
			return;
		}
		
		SMSMenu menu = plugin.getMenu(menuName);
		if (menu == null) {
			plugin.error_message(player, "Unknown menu name: " + menuName);
			return;
		}
		
		String msg = "";
		if (entry_args.length >= 3) {
			msg = entry_args[2];
		}
		menu.add(entry_args[0], entry_args[1], msg);
		menu.updateSign();
		plugin.status_message(player, "Menu entry [" + entry_args[0] + "] added to: " + menuName);
	}

	private void listSMSSigns(Player player, String[] args) {
		HashMap<String, SMSMenu> menus = plugin.getMenus();
	
		if (menus.size() == 0) {
			plugin.status_message(player, "No menu signs exist.");
			return;
		}
		
		Iterator<String> iter = menus.keySet().iterator();
		while (iter.hasNext()) {
			String k = iter.next();
			SMSMenu menu = menus.get(k);
			Location loc = menu.getLocation();
			String where = loc.getWorld().getName() + ", " + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
			plugin.status_message(player, k + ": " + menu.getTitle() + ", " + menu.getNumItems() + " items @" + where);
		}
	}

	private void showSMSInfo(Player player, String[] args) {
		String menuName;
		SMSMenu menu;
		if (args.length >= 2) {
			menuName = args[1];
		} else {
			menuName = getTargetedMenuSign(player);
			if (menuName == null)
				return;
		}
		menu = plugin.getMenu(menuName);
		if (menu == null) {
			plugin.error_message(player, "Unknown menu name: " + menuName);
			return;
		}
		plugin.status_message(player, "name = " + menu.getName() + ", title = " + menu.getTitle() +
				", item count = " + menu.getNumItems());
		ArrayList<SMSMenuItem> items = menu.getItems();
		int n = 1;
		for (SMSMenuItem item : items) {
			plugin.status_message(player, String.format("%2d) %s [%s] \"%s\"",
					n, item.getLabel(), item.getCommand(), item.getMessage()));
			n++;
		}
	}

	private void makeSMSSign(Player player, String[] args) {
		Block b = player.getTargetBlock(null, 3);
		if (args.length < 2) {
			plugin.error_message(player, "Usage: sms make <menu-name> <title>");
			plugin.error_message(player, "   or: sms make <menu-name> from <other-menu-name>");
			return;
		}
		String menuName = args[1];
		String menuTitle = combine(args, 2);
		if (b.getType() != Material.SIGN_POST && b.getType() != Material.WALL_SIGN) {
			plugin.error_message(player, "You are not looking at a sign");
			return;
		}
		if (plugin.getMenu(menuName) != null) {
			plugin.error_message(player, "A menu called '" + menuName + "' already exists.");
			return;
		}
		if (plugin.getMenuName(b.getLocation()) != null) {
			plugin.error_message(player, "There is already a menu attached to that sign.");
			return;
		}
		SMSMenu menu = null;
		if (args.length == 3) {
			menu = new SMSMenu(menuName, menuTitle, player.getName(), b.getLocation());
		} else if (args.length == 4 && args[2].equals("from")) {
			SMSMenu otherMenu = plugin.getMenu(args[3]);
			menu = new SMSMenu(otherMenu, player.getName(), b.getLocation());
		}
		plugin.addMenu(menuName, menu, true);
		plugin.status_message(player, "Added new menu sign: " + menuName);
	}

	private void breakSMSSign(Player player, String[] args) {
		if (args.length >= 2) {
			String menuName = args[1];
			if (plugin.getMenu(menuName) == null) {
				plugin.error_message(player, "Unknown menu name: " + menuName);
				return;
			}
			plugin.removeMenu(menuName, false);
		} else {
			String menuName = getTargetedMenuSign(player);
			if (menuName != null) {
				plugin.removeMenu(menuName, false);
				plugin.status_message(player, "Removed menu sign: " + menuName);
			}
		}
	}


	// Return the name of the menu sign that the player is looking at, if any
	private String getTargetedMenuSign(Player player) {
		Block b = player.getTargetBlock(null, 3);
		if (b.getType() != Material.SIGN_POST && b.getType() != Material.WALL_SIGN) {
			plugin.error_message(player, "You are not looking at a sign");
			return null;
		}
		String name = plugin.getMenuName(b.getLocation());
		if (name == null)
			plugin.error_message(player, "There is no menu associated with that sign.");
		return name;
	} 
	
	private static String combine(String[] args, int idx) {
		return combine(args, idx, args.length - 1);
	}
	
	private static String combine(String[] args, int idx1, int idx2) {
		StringBuilder result = new StringBuilder();
		for (int i = idx1; i <= idx2; i++) {
			result.append(args[i]);
			if (i < idx2) {
				result.append(" ");
			}
		}
		return result.toString();
	}
}
