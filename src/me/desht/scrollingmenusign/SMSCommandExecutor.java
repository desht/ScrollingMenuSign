package me.desht.scrollingmenusign;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import me.desht.scrollingmenusign.ScrollingMenuSign.MenuRemoveAction;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SMSCommandExecutor implements CommandExecutor {
	private ScrollingMenuSign plugin;
	private ArrayList<String> messageBuffer;
	private static int pageSize = 16;
	
	public SMSCommandExecutor(ScrollingMenuSign plugin) {
		this.plugin = plugin;
		messageBuffer = new ArrayList<String>();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Player player = null;
		if (sender instanceof Player) {
			player = (Player) sender;
		}
		
    	if (label.equalsIgnoreCase("sms")) {
    		if (args.length == 0) {
    			return false;
    		}
    		if (!plugin.isAllowedTo(player, "scrollingmenusign.commands." + args[0])) {
    			plugin.error_message(player, "You are not allowed to do that.");
    			return true;
    		}
    		try {
    			if (partialMatch(args[0], "c")) { 			// create
    				createSMSMenu(player, args);
    			} else if (partialMatch(args[0], "sy")) {	// sync
    				syncSMSSign(player, args);
    			} else if (partialMatch(args[0], "b")) {	// break
    				breakSMSSign(player, args);
    			} else if (partialMatch(args[0], "d")) {	// delete
    				deleteSMSMenu(player, args);
    			} else if (partialMatch(args[0], "l")) {	// list
    				listSMSMenus(player, args);
    			} else if (partialMatch(args[0], "sh")) {	// show
    				showSMSMenu(player, args);
    			} else if (partialMatch(args[0], "a")) {	// add
    				addSMSItem(player, args);
    			} else if (partialMatch(args[0], "rem")) {	// remove
    				removeSMSItem(player, args);
    			} else if (partialMatch(args[0], "sa")) { 	// save
    				saveCommand(player, args);
    			} else if (partialMatch(args[0], "rel")) {	// reload
    				loadCommand(player, args);
    			} else if (partialMatch(args[0], "g")) {	// getcfg
    				getConfig(player, args);
    			} else if (partialMatch(args[0], "se")) {	// setcfg
    				setConfig(player, args);
    			} else if (partialMatch(args[0], "t")) {	// title
    				setMenuTitle(player, args);
    			} else if (partialMatch(args[0], "p")) {	// page
    				pagedDisplay(player, args);
    			} else if (partialMatch(args[0], "m")) {	// macro
    				doMacroCommand(player, args);
    			} else {
    				return false;
    			}
    		} catch (SMSNoSuchMenuException e) {
    			plugin.error_message(player, e.getError());
    		} catch (IllegalArgumentException e) {
    			plugin.error_message(player, e.getMessage());
    		}
    	}
		return true;
	}

	private void createSMSMenu(Player player, String[] args) throws SMSNoSuchMenuException {
		if (args.length < 3) {
			plugin.error_message(player, "Usage: sms create <menu-name> <title>");
			plugin.error_message(player, "   or: sms create <menu-name> from <other-menu-name>");
			return;
		}
		String menuName = args[1];
		if (plugin.checkForMenu(menuName)) {
			plugin.error_message(player, "A menu called '" + menuName + "' already exists.");
			return;
		}
		
		Location loc = null;
		String owner = "&console";	// dummy owner if menu created from console
		
		if (player != null) {
			Block b = player.getTargetBlock(null, 3);
			if (b.getType() == Material.SIGN_POST || b.getType() == Material.WALL_SIGN) {
				if (plugin.getMenuNameAt(b.getLocation()) != null) {
					plugin.error_message(player, "There is already a menu attached to that sign.");
					return;
				}
				owner = player.getName();
				loc = b.getLocation();
			}			
		}
		
		SMSMenu menu = null;
		if (args.length == 4 && args[2].equals("from")) {
			SMSMenu otherMenu = plugin.getMenu(args[3]);
			menu = new SMSMenu(plugin, otherMenu, menuName, owner, loc);
		} else if (args.length >= 3) {
			String menuTitle = plugin.parseColourSpec(player, combine(args, 2));
			menu = new SMSMenu(plugin, menuName, menuTitle, owner, loc);
		}
		plugin.addMenu(menuName, menu, true);
		plugin.status_message(player, "Added new scrolling menu: " + menuName);
		maybeSaveMenus();
	}

	private void deleteSMSMenu(Player player, String[] args) throws SMSNoSuchMenuException {
		String menuName = null;
		if (args.length >= 2) {
			menuName = args[1];
			plugin.removeMenu(menuName, ScrollingMenuSign.MenuRemoveAction.BLANK_SIGN);
		} else {
			if (onConsole(player)) return;
			menuName = plugin.getTargetedMenuSign(player, true);
			if (menuName != null) {
				plugin.removeMenu(menuName, ScrollingMenuSign.MenuRemoveAction.BLANK_SIGN);
			} else {
				plugin.error_message(player, "You are not looking at a sign.");
				return;
			}
		}
		plugin.status_message(player, "Deleted scrolling menu: " + menuName);
		maybeSaveMenus();
	}

	private void breakSMSSign(Player player, String[] args) throws SMSNoSuchMenuException {
		Location l = null;
		String menuName = null;
		if (args.length < 2) {
			if (onConsole(player)) return;
			menuName = plugin.getTargetedMenuSign(player, true);
			if (menuName == null)
				return;
			Block b = player.getTargetBlock(null, 3);
			l = b.getLocation();
		} else {
			l = parseLocation(args[1], player);
			menuName = plugin.getMenuNameAt(l);
			if (menuName == null) {
				plugin.error_message(player, "There is no menu at that location.");
				return;
			}
		}
		plugin.removeSignFromMenu(l, MenuRemoveAction.BLANK_SIGN);
		plugin.status_message(player, "Sign @ " +
				l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ() +
				" was removed from menu '" + menuName + "'");
		maybeSaveMenus();
	}
	
	private void syncSMSSign(Player player, String[] args) throws SMSNoSuchMenuException {
		if (onConsole(player)) return;
		
		if (args.length < 2) {
			plugin.error_message(player, "Usage: sms sync <menu-name>");
			return;
		}

		Block b = player.getTargetBlock(null, 3);		
		if (b.getType() != Material.SIGN_POST && b.getType() != Material.WALL_SIGN) {
			plugin.error_message(player, "You are not looking at a sign.");
			return;
		}
		String existingMenu = plugin.getMenuNameAt(b.getLocation());
		if (existingMenu != null) {
			plugin.error_message(player, "That sign already belongs to menu '" + existingMenu + "'.");
			return;
		}
		String menuName = args[1];
		plugin.syncMenu(menuName, b.getLocation());
		plugin.status_message(player, "Added sign to scrolling menu: " + menuName);
		maybeSaveMenus();
	}

	private void listSMSMenus(Player player, String[] args) {
		Map<String, SMSMenu> menus = plugin.getMenus();	
		if (menus.size() == 0) {
			plugin.status_message(player, "No menu signs exist.");
			return;
		}
		
		messageBuffer.clear();
		
		SortedSet<String> sorted = new TreeSet<String>(menus.keySet());
		for (String k : sorted) {
			SMSMenu menu = menus.get(k);
			Map<Location,Integer> locs = menu.getLocations();
			ChatColor signCol = locs.size() > 0 ? ChatColor.YELLOW : ChatColor.RED;
			String message = ChatColor.YELLOW + k +
				ChatColor.GREEN + " \"" + menu.getTitle() + ChatColor.GREEN + "\"" +
				ChatColor.YELLOW + " [" + menu.getNumItems() + " items]" +
				signCol + " [" + locs.size() + " signs]";
			messageBuffer.add(message);
			for (Location loc: locs.keySet()) {
				messageBuffer.add(" - " + formatLoc(loc));
			}
		}
		pagedDisplay(player, 1);
	}

	private void showSMSMenu(Player player, String[] args) throws SMSNoSuchMenuException {
		String menuName;
		if (args.length >= 2) {
			menuName = args[1];
		} else {
			if (onConsole(player)) return;
			menuName = plugin.getTargetedMenuSign(player, true);
			if (menuName == null)
				return;
		}
		SMSMenu menu = plugin.getMenu(menuName);
		messageBuffer.clear();
		messageBuffer.add(ChatColor.YELLOW + "Menu '" + menuName + "': title '" + menu.getTitle() + "'");
		ArrayList<SMSMenuItem> items = menu.getItems();
		int n = 1;
		for (SMSMenuItem item : items) {
			String s = String.format(ChatColor.YELLOW + "%2d)" +
					ChatColor.WHITE + " %s " + ChatColor.WHITE + "[%s] \"%s\"",
					n, item.getLabel(), item.getCommand(), item.getMessage());
			n++;
			messageBuffer.add(s);
		}
		pagedDisplay(player, 1);
	}

	private void setMenuTitle(Player player, String[] args) throws SMSNoSuchMenuException {
		if (args.length < 3) {
			plugin.error_message(player, "Usage: /sms title <menu-name> <new-title>");
			return;
		}
		plugin.setTitle(player, args[1], combine(args, 2));
		maybeSaveMenus();
	}

	private void addSMSItem(Player player, String[] args) throws SMSNoSuchMenuException {	
		if (args.length < 3) {
			plugin.error_message(player, "Usage: /sms add <menu-name> <menu-entry>");
			return;
		}
			
		String menuName = args[1];
		String sep = plugin.getConfiguration().getString("sms.menuitem_separator", "\\|");
		String[] entry_args = combine(args, 2).split(sep);		
		if (entry_args.length < 2) {
			plugin.error_message(player, "menu-entry must include at least entry label & command");
			return;
		}
		
		SMSMenu menu = plugin.getMenu(menuName);				
		String label = plugin.parseColourSpec(player, entry_args[0]);
		String cmd = entry_args[1];
		String msg = entry_args.length >= 3 ? entry_args[2] : "";

		if (plugin.csHandler == null || plugin.csHandler.hasEnablingPermissions(player, cmd)) {
			menu.addItem(label, cmd, msg);
			menu.updateSigns();
			plugin.status_message(player, "Menu entry [" + label + "] added to: " + menuName);
		} else {
			plugin.error_message(player, "You do not have permission to add that kind of command.");
		}
		maybeSaveMenus();
	}

	private void removeSMSItem(Player player, String[] args) throws SMSNoSuchMenuException {
		if (args.length < 3) {
			plugin.error_message(player, "Usage: /sms remove <menu-name> <item-index>");
			return;
		}
		String menuName = args[1];
		String item = args[2];

		try {
			SMSMenu menu = plugin.getMenu(menuName);
			menu.removeItem(item);
			menu.updateSigns();
			plugin.status_message(player, "Menu entry #" + item + " removed from: " + menuName);
		} catch (IndexOutOfBoundsException e) {
			plugin.error_message(player, "Item index " + item + " out of range");
		} catch (IllegalArgumentException e) {
			plugin.error_message(player, e.getMessage());
		}
		maybeSaveMenus();
	}

	private void setConfig(Player player, String[] args) {
		if (args.length < 3) {
			plugin.error_message(player, "Usage: /sms setcfg <key> <value>");
			return;
		}
		plugin.setConfigItem(player, args[1], combine(args, 2));
		if (args[1].matches("item_(justify|prefix.*)")) {
			plugin.updateAllMenus();
		}
	}

	private void getConfig(Player player, String[] args) {
		messageBuffer.clear();
		if (args.length < 2) {
			for (String line : plugin.getConfigList()) {
				messageBuffer.add(line);
			}
			pagedDisplay(player, 1);
		} else {
			String res = plugin.getConfiguration().getString(args[1]);
			if (res != null) {
				plugin.status_message(player, args[1] + " = '" + res + "'");
			} else {
				plugin.error_message(player, "No such config item " + args[1]);
			}
		}
	}

	private void saveCommand(Player player, String[] args) {
		Boolean saveMenus = false;
		Boolean saveMacros = false;
		Boolean saveAll = false;
		if (args.length < 2) {
			saveAll = true;
		} else {
			for (int i = 1 ; i < args.length; i++) {
				if (args[i].equalsIgnoreCase("menus")) {
					saveMenus = true;
				} else if (args[i].equalsIgnoreCase("macros")) {
					saveMacros = true;
				}
			}
		}
		if (saveAll || saveMenus) plugin.saveMenus();
		if (saveAll || saveMacros) plugin.saveMacros();
		plugin.status_message(player, "Save complete.");
	}

	private void loadCommand(Player player, String[] args) {
		Boolean loadMenus = false;
		Boolean loadMacros = false;
		Boolean loadConfig = false;
		Boolean loadAll = false;
		if (args.length < 2) {
			loadAll = true;
		} else {
			for (int i = 1 ; i < args.length; i++) {
				if (args[i].equalsIgnoreCase("menus")) {
					loadMenus = true;
				} else if (args[i].equalsIgnoreCase("macros")) {
					loadMacros = true;
				} else if (args[i].equalsIgnoreCase("config")) {
					loadConfig = true;
				}
			}
		}
		if (loadAll || loadConfig) {
				plugin.getConfiguration().load();
				plugin.updateAllMenus();
		}
		if (loadAll || loadMenus) plugin.loadMenus();
		if (loadAll || loadMacros) plugin.loadMacros();
		plugin.status_message(player, "Reload complete.");
	}

	private void doMacroCommand(Player player, String[] args) {
		if (args.length < 2) {
			plugin.error_message(player, "Usage: /sms macro list");
			plugin.error_message(player, "       /sms macro list <macro-name>");
			plugin.error_message(player, "       /sms macro add <macro-name> <command>");
			plugin.error_message(player, "       /sms macro remove <macro-name> <index>");
			return;
		}
		Boolean needSave = false;
		if (partialMatch(args[1], "l")) {			// list
			messageBuffer.clear();
			int i = 1;
			if (args.length < 3) {
				Set<String> macros = plugin.commandFile.getCommands();
				messageBuffer.add(ChatColor.YELLOW + "" + macros.size() + " macros");
				for (String m : macros) {
					messageBuffer.add(ChatColor.YELLOW + "" + i++ + ") " + ChatColor.WHITE + m);
				}
			} else {
				List<String> cmds = plugin.commandFile.getCommands(args[2]);
				messageBuffer.add(ChatColor.YELLOW + "" + cmds.size() + " commands");
				for (String c : cmds) {
					messageBuffer.add(ChatColor.YELLOW + "" + i++ + ") " + ChatColor.WHITE + c);
				}
			}
			pagedDisplay(player, 1);
		} else if (partialMatch(args[1], "a")) {	// add
			if (args.length >= 4) {
				String s = combine(args, 3);
				plugin.commandFile.addCommand(args[2], s);
				plugin.status_message(player, "Added command to macro '" + args[2] + "'.");
				needSave = true;
			}
		} else if (partialMatch(args[1], "r")) {	// remove
			if (args.length < 4) {
				plugin.commandFile.removeCommand(args[2]);
				plugin.status_message(player, "Removed macro '" + args[2] + "'.");
				needSave = true;
			} else {
				try { 
					int index = Integer.parseInt(args[3]);
					plugin.commandFile.removeCommand(args[2], index - 1);
					plugin.status_message(player, "Removed command from macro '" + args[2] + "'.");
					needSave = true;
				} catch (NumberFormatException e) {
					plugin.error_message(player, "invalid index " + args[3]);
				} catch (IndexOutOfBoundsException e) {
					plugin.error_message(player, "invalid index " + args[3]);	
				}
			}
		}
		if (needSave) plugin.saveMacros();
	}

	private String formatLoc(Location loc) {
		StringBuilder str = new StringBuilder(ChatColor.WHITE + "@ " +
			loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + "," +
			loc.getWorld().getName());
		Block b = plugin.getServer().getWorld(loc.getWorld().getName()).getBlockAt(loc);
		if (b.getType() != Material.SIGN_POST && b.getType() != Material.WALL_SIGN) {
			str.append(ChatColor.RED + " [ NO SIGN ]");
		}
		return str.toString();
	}

	private void pagedDisplay(Player player, String[] args) {
		int pageNum = 1;
		if (args.length < 2) return;
		try {
			pageNum = Integer.parseInt(args[1]);
			pagedDisplay(player, pageNum);
		} catch (NumberFormatException e) {
			plugin.error_message(player, "invalid argument '" + args[1] + "'");
		}
	}
	
	private void pagedDisplay(Player player, int pageNum) {
		if (player != null) {
			// pretty paged display
			int nMessages = messageBuffer.size();
			plugin.status_message(player, ChatColor.GREEN + "" +  nMessages +
					" lines (page " + pageNum + "/" + ((nMessages-1) / pageSize + 1) + ")");
			plugin.status_message(player, ChatColor.GREEN + "---------------");
			for (int i = (pageNum -1) * pageSize; i < nMessages && i < pageNum * pageSize; i++) {
				plugin.status_message(player, messageBuffer.get(i));
			}
			plugin.status_message(player, ChatColor.GREEN + "---------------");
			String footer = (nMessages > pageSize * pageNum) ? "Use /sms page [page#] to see more" : "";
			plugin.status_message(player, ChatColor.GREEN + footer);
		} else {
			// just dump the whole message buffer to the console
			for (String s: messageBuffer) {
				plugin.status_message(null, ScrollingMenuSign.deColourise(s));
			}
		}
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

	private Boolean partialMatch(String str, String match) {
		int l = match.length();
		if (str.length() < l) return false;
		return str.substring(0, l).equalsIgnoreCase(match);
	}

	private boolean onConsole(Player player) {
		if (player == null) {
			plugin.error_message(player, "This command cannot be run from the console.");
			return true;
		} else {
			return false;
		}
	}

	private void maybeSaveMenus() {
		if (plugin.getConfiguration().getBoolean("sms.autosave", false)) {
			plugin.saveMenus();
		}
	}

	private Location parseLocation(String arglist, Player player) {
		String s = player == null ? ",worldname" : "";
		String args[] = arglist.split(",");
		try {
			int x = Integer.parseInt(args[0]);
			int y = Integer.parseInt(args[1]);
			int z = Integer.parseInt(args[2]);
			World w = (player == null) ?
					plugin.getServer().getWorld(args[3]) :
					player.getWorld();
			if (w == null) throw new IllegalArgumentException("Unknown world: " + args[3]);
			return new Location(w, x, y, z);
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new IllegalArgumentException("You must specify all of x,y,z" + s + ".");
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid location: x,y,z" + s + ".");
		}
	}
}
