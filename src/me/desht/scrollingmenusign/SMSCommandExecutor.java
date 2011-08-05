package me.desht.scrollingmenusign;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SMSCommandExecutor implements CommandExecutor {
	private ScrollingMenuSign plugin;
	
	SMSCommandExecutor(ScrollingMenuSign plugin) {
		this.plugin = plugin;
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
    		try {
    			if (partialMatch(args[0], "c")) { 			// create
    				createSMSMenu(player, args);
    			} else if (partialMatch(args[0], "sy")) {	// sync
    				syncSMSSign(player, args);
    			} else if (partialMatch(args[0], "b")) {	// break
    				breakSMSSign(player, args);
    			} else if (partialMatch(args[0], "del")) {	// delete
    				deleteSMSMenu(player, args);
    			} else if (partialMatch(args[0], "l")) {	// list
    				listSMSMenus(player, args);
    			} else if (partialMatch(args[0], "sh")) {	// show
    				showSMSMenu(player, args);
    			} else if (partialMatch(args[0], "so")) { 	// sort
    				sortSMSMenu(player, args);
    			} else if (partialMatch(args[0], "u")) { 	// uses
    				setItemUseCount(player, args);
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
    				pageCommand(player, args);
    			} else if (partialMatch(args[0], "m")) {	// macro
    				doMacroCommand(player, args);
    			} else if (partialMatch(args[0], "deb")) {	// debug
    				debugCommand(player, args);
    			} else {
    				return false;
    			}
    		} catch (SMSException e) {
    			SMSUtils.errorMessage(player, e.getMessage());
    		} catch (IllegalArgumentException e) {
    			SMSUtils.errorMessage(player, e.getMessage());
    		}
    	}
		return true;
	}

	private void createSMSMenu(Player player, String[] args) throws SMSException {
		SMSPermissions.requirePerms(player, "scrollingmenusign.commands.create");
		if (args.length < 3) {
			SMSUtils.errorMessage(player, "Usage: sms create <menu-name> <title>");
			SMSUtils.errorMessage(player, "   or: sms create <menu-name> from <other-menu-name>");
			return;
		}
		String menuName = args[1];
		if (SMSMenu.checkForMenu(menuName)) {
			SMSUtils.errorMessage(player, "A menu called '" + menuName + "' already exists.");
			return;
		}
		
		Location loc = null;
		String owner = "&console";	// dummy owner if menu created from console
		
		if (player != null) {
			Block b = player.getTargetBlock(null, 3);
			if (b.getType() == Material.SIGN_POST || b.getType() == Material.WALL_SIGN) {
				if (SMSMenu.getMenuNameAt(b.getLocation()) != null) {
					SMSUtils.errorMessage(player, "There is already a menu attached to that sign.");
					return;
				}
				owner = player.getName();
				loc = b.getLocation();
			}			
		}
		
		SMSMenu menu = null;
		if (args.length == 4 && args[2].equals("from")) {
			SMSMenu otherMenu = SMSMenu.getMenu(args[3]);
			menu = new SMSMenu(plugin, otherMenu, menuName, owner, loc);
		} else if (args.length >= 3) {
			String menuTitle = SMSUtils.parseColourSpec(player, combine(args, 2));
			menu = new SMSMenu(plugin, menuName, menuTitle, owner, loc);
		}
		SMSMenu.addMenu(menuName, menu, true);
		SMSUtils.statusMessage(player, "Created new menu &e" + menuName + "&- " +
		                       (loc == null ? " with no signs" : " with sign @ &f" + SMSUtils.formatLocation(loc)));
	}

	private void deleteSMSMenu(Player player, String[] args) throws SMSException {
		SMSPermissions.requirePerms(player, "scrollingmenusign.commands.delete");
		
		SMSMenu menu = null;
		if (args.length >= 2) {
			menu = SMSMenu.getMenu(args[1]);
		} else {
			notFromConsole(player);
			menu = SMSMenu.getMenu(SMSMenu.getTargetedMenuSign(player, true));
		}
		menu.deletePermanent();
		SMSUtils.statusMessage(player, "Deleted menu &e" + menu.getName());
	}

	private void breakSMSSign(Player player, String[] args) throws SMSException {
		SMSPermissions.requirePerms(player, "scrollingmenusign.commands.break");
		
		Location loc = null;
		if (args.length < 2) {
			notFromConsole(player);
			Block b = player.getTargetBlock(null, 3);
			loc = b.getLocation();
		} else {
			loc = SMSUtils.parseLocation(args[1], player);
		}
		SMSMenu menu = SMSMenu.getMenuAt(loc);
		menu.removeSign(loc, MenuRemovalAction.BLANK_SIGN);
		SMSUtils.statusMessage(player, "Sign @ &f" + SMSUtils.formatLocation(loc) +
		                       "&- was removed from menu &e" + menu.getName() + "&-");
	}
	
	private void syncSMSSign(Player player, String[] args) throws SMSException {
		SMSPermissions.requirePerms(player, "scrollingmenusign.commands.sync");
		notFromConsole(player);
		
		if (args.length < 2) {
			SMSUtils.errorMessage(player, "Usage: sms sync <menu-name>");
			return;
		}

		Block b = player.getTargetBlock(null, 3);		
		if (b.getType() != Material.SIGN_POST && b.getType() != Material.WALL_SIGN) {
			throw new SMSException("You are not looking at a sign.");
		}
		SMSMenu menu = SMSMenu.getMenu(args[1]);
		menu.addSign(b.getLocation(), true);
		menu.updateSign(b.getLocation());

		SMSUtils.statusMessage(player, "Sign @ &f" + SMSUtils.formatLocation(b.getLocation()) +
		                       "&- was added to menu &e" + menu.getName() + "&-");
	}

	private void listSMSMenus(Player player, String[] args) throws SMSException {
		SMSPermissions.requirePerms(player, "scrollingmenusign.commands.list");
		
		MessageBuffer.clear(player);
		
		if (args.length >= 2) {
			SMSMenu menu = SMSMenu.getMenu(args[1]);
			listMenu(player, menu);
		} else {
			List<SMSMenu> menus = SMSMenu.listMenus(true);
			if (menus.size() == 0) {
				SMSUtils.statusMessage(player, "No menu signs exist.");
			} else {
				for (SMSMenu menu : menus) {
					listMenu(player, menu);
				}
			}
		}
		MessageBuffer.showPage(player);
	}

	private void listMenu(Player player, SMSMenu menu) {
		Map<Location,Integer> locs = menu.getLocations();
		ChatColor signCol = locs.size() > 0 ? ChatColor.YELLOW : ChatColor.RED;
		String message = String.format("&e%s &2\"%s&2\" &e[%d items] %s[%d signs]",
		                               menu.getName(), menu.getTitle(), menu.getItemCount(),
		                               signCol.toString(), locs.size());
		List<String> l = new ArrayList<String>();
		l.add(message);
		for (Location loc: locs.keySet()) {
			l.add(" &5*&- " + SMSUtils.formatLocation(loc));
		}
		MessageBuffer.add(player, l);
	}

	private void showSMSMenu(Player player, String[] args) throws SMSException {
		SMSPermissions.requirePerms(player, "scrollingmenusign.commands.show");
		
		SMSMenu menu = null;
		if (args.length >= 2) {
			menu = SMSMenu.getMenu(args[1]);
		} else {
			notFromConsole(player);
			menu = SMSMenu.getMenu(SMSMenu.getTargetedMenuSign(player, true));
		}
		MessageBuffer.clear(player);
		MessageBuffer.add(player, "Menu &e" + menu.getName() + "&-: title &f" + menu.getTitle());
		List<SMSMenuItem> items = menu.getItems();
		int n = 1;
		for (SMSMenuItem item : items) {
			String s = String.format("&e%2d)" +
					" &f%s " + "&f[%s] \"%s\"&f &c%s",
					n, item.getLabel(), item.getCommand(), item.getMessage(), item.formatUses(player));
			n++;
			MessageBuffer.add(player, s);
		}
		MessageBuffer.showPage(player);
	}

	private void sortSMSMenu(Player player, String[] args) throws SMSException {
		SMSPermissions.requirePerms(player, "scrollingmenusign.commands.sort");
		
		SMSMenu menu = null;
		if (args.length >= 2) {
			menu = SMSMenu.getMenu(args[1]);
		} else {
			notFromConsole(player);
			menu = SMSMenu.getMenu(SMSMenu.getTargetedMenuSign(player, true));
		}
		
		if (partialMatch(args, 2, "a")) {	// autosort
			menu.setAutosort(true);
			menu.sortItems();
			SMSUtils.statusMessage(player, "Menu &e" + menu.getName() + "&- has been sorted (autosort enabled)");
		} else {
			menu.setAutosort(false);
			menu.sortItems();
			SMSUtils.statusMessage(player, "Menu &e" + menu.getName() + "&- has been sorted (autosort disabled)");
		}
		menu.updateSigns();
	}

	private void setMenuTitle(Player player, String[] args) throws SMSException {
		if (args.length < 3) {
			SMSUtils.errorMessage(player, "Usage: /sms title <menu-name> <new-title>");
			return;
		}
		SMSMenu menu = SMSMenu.getMenu(args[1]);
		String title = combine(args, 2);
		menu.setTitle(SMSUtils.parseColourSpec(player, title));
		menu.updateSigns();
		
		SMSUtils.statusMessage(player, "Title for menu &e" + menu.getName() + "&- has been set to &f" + title + "&-.");
	}

	private void addSMSItem(Player player, String[] args) throws SMSException {	
		SMSPermissions.requirePerms(player, "scrollingmenusign.commands.add");
		
		if (args.length < 3) {
			SMSUtils.errorMessage(player, "Usage: /sms add <menu-name> <menu-entry>");
			return;
		}
			
		String menuName = args[1];
		String sep = plugin.getConfiguration().getString("sms.menuitem_separator", "|");
		String[] entry_args = combine(args, 2).split(Pattern.quote(sep));		
		if (entry_args.length < 2) {
			SMSUtils.errorMessage(player, "menu-entry must include at least entry label & command");
			return;
		}
		
		SMSMenu menu = SMSMenu.getMenu(menuName);				
		String label = SMSUtils.parseColourSpec(player, entry_args[0]);
		String cmd = entry_args[1];
		String msg = entry_args.length >= 3 ? entry_args[2] : "";

		if (!SMSCommandSigns.isActive() || player == null || SMSCommandSigns.hasEnablingPermissions(player, cmd)) {
			menu.addItem(label, cmd, msg);
			menu.updateSigns();
			SMSUtils.statusMessage(player, "Menu entry &f" + label + "&- added to: &e" + menuName);
		} else {
			SMSUtils.errorMessage(player, "You do not have permission to add that kind of command.");
		}
	}

	private void removeSMSItem(Player player, String[] args) throws SMSException {
		SMSPermissions.requirePerms(player, "scrollingmenusign.commands.remove");
		
		if (args.length < 3) {
			SMSUtils.errorMessage(player, "Usage: /sms remove <menu-name> <item-index>");
			return;
		}
		String menuName = args[1];
		String item = args[2];

		try {
			SMSMenu menu = SMSMenu.getMenu(menuName);
			menu.removeItem(item);
			menu.updateSigns();
			SMSUtils.statusMessage(player, "Menu entry &f#" + item + "&- removed from &e" + menuName);
		} catch (IndexOutOfBoundsException e) {
			SMSUtils.errorMessage(player, "Item index " + item + " out of range");
		} catch (IllegalArgumentException e) {
			SMSUtils.errorMessage(player, e.getMessage());
		}
	}

	private void setItemUseCount(Player player, String[] args) throws SMSException {
		SMSPermissions.requirePerms(player, "scrollingmenusign.commands.uses");
		
		if (args.length < 4) {
			SMSUtils.errorMessage(player, "Usage: /sms uses <menu> <item> <count> [global|clear]");
			return;
		}
		
		SMSMenu menu = SMSMenu.getMenu(args[1]);
		int idx = menu.indexOfItem(args[2]);
		if (idx <= 0) {
			throw new SMSException("Unknown menu item '" + args[2] + "'");
		}
		SMSMenuItem item = menu.getItem(idx);
		
		if (partialMatch(args, 3, "c")) {
			item.clearUses();
			SMSUtils.statusMessage(player, "Unset all usage limits for item &e" + item.getLabel());			
		} else {
			int count;
			try {
				count = Integer.parseInt(args[3]);
			} catch (NumberFormatException e) {
				throw new SMSException("Invalid numeric argument '" + args[3] + "'");
			}

			if (partialMatch(args, 4, "g")) {
				item.setGlobalUses(count);
				SMSUtils.statusMessage(player, "Set GLOBAL use limit for item &e" + item.getLabel()
						+ "&- to " + count + ".");
			} else {
				SMSUtils.statusMessage(player, "Set PER-PLAYER use limit for item &e" + item.getLabel()
						+ "&- to " + count + ".");
				item.setUses(count);
			}
		}
	}

	private void setConfig(Player player, String[] args) throws SMSException {
		SMSPermissions.requirePerms(player, "scrollingmenusign.commands.setcfg");
		
		if (args.length < 3) {
			SMSUtils.errorMessage(player, "Usage: /sms setcfg <key> <value>");
			return;
		}
		SMSConfig.setConfigItem(player, args[1], combine(args, 2));
		if (args[1].matches("item_(justify|prefix.*)")) {
			SMSMenu.updateAllMenus();
		}
	}

	private void getConfig(Player player, String[] args) throws SMSException {
		SMSPermissions.requirePerms(player, "scrollingmenusign.commands.getcfg");
		
		MessageBuffer.clear(player);
		if (args.length < 2) {
			for (String line : SMSConfig.getConfigList()) {
				MessageBuffer.add(player, line);
			}
			MessageBuffer.showPage(player);
		} else {
			String res = plugin.getConfiguration().getString(args[1]);
			if (res != null) {
				SMSUtils.statusMessage(player, args[1] + " = '" + res + "'");
			} else {
				SMSUtils.errorMessage(player, "No such config item " + args[1]);
			}
		}
	}

	private void saveCommand(Player player, String[] args) throws SMSException {
		SMSPermissions.requirePerms(player, "scrollingmenusign.commands.save");
		
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
		SMSUtils.statusMessage(player, "Save complete.");
	}

	private void loadCommand(Player player, String[] args) throws SMSException {
		SMSPermissions.requirePerms(player, "scrollingmenusign.commands.reload");
		
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
				SMSMenu.updateAllMenus();
		}
		if (loadAll || loadMenus) plugin.loadMenus();
		if (loadAll || loadMacros) plugin.loadMacros();
		SMSUtils.statusMessage(player, "Reload complete.");
	}

	private void doMacroCommand(Player player, String[] args) throws SMSException {
		SMSPermissions.requirePerms(player, "scrollingmenusign.commands.macro");
		
		if (args.length < 2) {
			SMSUtils.errorMessage(player, "Usage: /sms macro list");
			SMSUtils.errorMessage(player, "       /sms macro list <macro-name>");
			SMSUtils.errorMessage(player, "       /sms macro add <macro-name> <command>");
			SMSUtils.errorMessage(player, "       /sms macro remove <macro-name> <index>");
			return;
		}
		Boolean needSave = false;
		if (partialMatch(args[1], "l")) {			// list
			MessageBuffer.clear(player);
			int i = 1;
			if (args.length < 3) {
				Set<String> macros = SMSMacro.getCommands();
				MessageBuffer.add(player, "&e" + macros.size() + " macros");
				for (String m : macros) {
					MessageBuffer.add(player, "&e" + i++ + ") &f" + m);
				}
			} else {
				List<String> cmds = SMSMacro.getCommands(args[2]);
				MessageBuffer.add(player, "&e" + cmds.size() + " macro entries");
				for (String c : cmds) {
					MessageBuffer.add(player, "&e" + i++ + ") &f" + c);
				}
			}
			MessageBuffer.showPage(player);
		} else if (partialMatch(args[1], "a")) {	// add
			if (args.length >= 4) {
				String s = combine(args, 3);
				SMSMacro.addCommand(args[2], s);
				SMSUtils.statusMessage(player, "Added command to macro &e" + args[2] + "&-.");
				needSave = true;
			}
		} else if (partialMatch(args[1], "r")) {	// remove
			if (args.length < 4) {
				SMSMacro.removeCommand(args[2]);
				SMSUtils.statusMessage(player, "Removed macro &e" + args[2] + "&-.");
				needSave = true;
			} else {
				try { 
					int index = Integer.parseInt(args[3]);
					SMSMacro.removeCommand(args[2], index - 1);
					SMSUtils.statusMessage(player, "Removed command from macro &e" + args[2] + "&-.");
					needSave = true;
				} catch (NumberFormatException e) {
					SMSUtils.errorMessage(player, "invalid index: " + args[3]);
				} catch (IndexOutOfBoundsException e) {
					SMSUtils.errorMessage(player, "invalid index: " + args[3]);	
				}
			}
		}
		if (needSave) plugin.saveMacros();
	}

	private void debugCommand(Player player, String[] args) throws SMSException {
		SMSPermissions.requirePerms(player, "scrollingmenusign.commands.debug");
		
		plugin.debugger.toggleDebug(player);
		int level = plugin.debugger.getDebugLevel(player);
		if (level > 0) {
			SMSUtils.statusMessage(player, "Debugging enabled.");
		} else {
			SMSUtils.statusMessage(player, "Debugging disabled.");
		}
	}

	private void pageCommand(Player player, String[] args) {
		if (args.length < 2) {
			// default is to advance one page and display
			MessageBuffer.nextPage(player);
			MessageBuffer.showPage(player);
		} else if (partialMatch(args, 1, "n")) { //$NON-NLS-1$
			MessageBuffer.nextPage(player);
			MessageBuffer.showPage(player);
		} else if (partialMatch(args, 1, "p")) { //$NON-NLS-1$
			MessageBuffer.prevPage(player);
			MessageBuffer.showPage(player);
		} else {
			try {
				int pageNum = Integer.parseInt(args[1]);
				MessageBuffer.showPage(player, pageNum);
			} catch (NumberFormatException e) {
				SMSUtils.errorMessage(player, "Invalid page number " + args[1]);
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
	
	private static boolean partialMatch(String[] args, int index, String match) {
		if (index >= args.length) {
			return false;
		}
		return partialMatch(args[index], match);
	}
	
	private static Boolean partialMatch(String str, String match) {
		int l = match.length();
		if (str.length() < l) return false;
		return str.substring(0, l).equalsIgnoreCase(match);
	}

	private void notFromConsole(Player player) throws SMSException {
		if (player == null) {
			throw new SMSException("This command can't be run from the console.");
		}	
	}
}
