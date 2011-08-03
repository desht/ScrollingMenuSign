package me.desht.scrollingmenusign;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import me.desht.scrollingmenusign.ScrollingMenuSign.MenuRemoveAction;

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
    			} else if (partialMatch(args[0], "deb")) {	// debug
    				debugCommand(player, args);
    			} else {
    				return false;
    			}
    		} catch (SMSNoSuchMenuException e) {
    			SMSUtils.errorMessage(player, e.getMessage());
    		} catch (SMSException e) {
    			SMSUtils.errorMessage(player, e.getMessage());
    		} catch (IllegalArgumentException e) {
    			SMSUtils.errorMessage(player, e.getMessage());
    		}
    	}
		return true;
	}

	private void createSMSMenu(Player player, String[] args) throws SMSNoSuchMenuException, SMSException {
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
		SMSUtils.statusMessage(player, "Created new menu &e" + menuName + "&- " + (loc == null ? " with no signs" : " with sign @ &f" + formatLoc(loc)));
		menu.autosave();
	}

	private void deleteSMSMenu(Player player, String[] args) throws SMSNoSuchMenuException, SMSException {
		SMSPermissions.requirePerms(player, "scrollingmenusign.commands.delete");
		
		SMSMenu menu = null;
		if (args.length >= 2) {
			menu = SMSMenu.getMenu(args[1]);
		} else {
			if (onConsole(player)) return;
			menu = SMSMenu.getMenu(SMSMenu.getTargetedMenuSign(player, true));
		}
		menu.deletePermanent();
		menu.autosave();
		SMSUtils.statusMessage(player, "Deleted menu &e" + menu.getName());
	}

	private void breakSMSSign(Player player, String[] args) throws SMSNoSuchMenuException, SMSException {
		SMSPermissions.requirePerms(player, "scrollingmenusign.commands.break");
		
		Location loc = null;
		if (args.length < 2) {
			if (onConsole(player)) return;
			Block b = player.getTargetBlock(null, 3);
			loc = b.getLocation();
		} else {
			loc = SMSUtils.parseLocation(args[1], player);
		}
		SMSMenu menu = SMSMenu.getMenuAt(loc);
		menu.removeSign(loc, MenuRemoveAction.BLANK_SIGN);
		SMSUtils.statusMessage(player, "Sign @ &f" + SMSUtils.formatLocation(loc) +
		                       "&- was removed from menu &e" + menu.getName() + "&-");
		menu.autosave();
	}
	
	private void syncSMSSign(Player player, String[] args) throws SMSNoSuchMenuException, SMSException {
		SMSPermissions.requirePerms(player, "scrollingmenusign.commands.sync");
		
		if (onConsole(player)) return;
		
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
		menu.autosave();

		SMSUtils.statusMessage(player, "Sign @ &f" + SMSUtils.formatLocation(b.getLocation()) +
		                       "&- was added to menu &e" + menu.getName() + "&-");
	}

	private void listSMSMenus(Player player, String[] args) throws SMSNoSuchMenuException, SMSException {
		SMSPermissions.requirePerms(player, "scrollingmenusign.commands.list");
		
		messageBuffer.clear();
		
		if (args.length >= 2) {
			SMSMenu menu = SMSMenu.getMenu(args[1]);
			listMenu(menu);
		} else {
			List<SMSMenu> menus = SMSMenu.listMenus(true);
			if (menus.size() == 0) {
				SMSUtils.statusMessage(player, "No menu signs exist.");
			} else {
				for (SMSMenu menu : menus) {
					listMenu(menu);
				}
			}
		}
		pagedDisplay(player, 1);
	}

	private void listMenu(SMSMenu menu) {
		Map<Location,Integer> locs = menu.getLocations();
		ChatColor signCol = locs.size() > 0 ? ChatColor.YELLOW : ChatColor.RED;
		String message = ChatColor.YELLOW + menu.getName() +
			ChatColor.GREEN + " \"" + menu.getTitle() + ChatColor.GREEN + "\"" +
			ChatColor.YELLOW + " [" + menu.getNumItems() + " items]" +
			signCol + " [" + locs.size() + " signs]";
		messageBuffer.add(message);
		for (Location loc: locs.keySet()) {
			messageBuffer.add(" - " + formatLoc(loc));
		}
	}

	private void showSMSMenu(Player player, String[] args) throws SMSNoSuchMenuException, SMSException {
		SMSPermissions.requirePerms(player, "scrollingmenusign.commands.show");
		
		SMSMenu menu = null;
		if (args.length >= 2) {
			menu = SMSMenu.getMenu(args[1]);
		} else {
			if (onConsole(player)) return;
			menu = SMSMenu.getMenu(SMSMenu.getTargetedMenuSign(player, true));
		}
		messageBuffer.clear();
		messageBuffer.add(ChatColor.YELLOW + "Menu '" + menu.getName() + "': title '" + menu.getTitle() + "'");
		List<SMSMenuItem> items = menu.getItems();
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
			SMSUtils.errorMessage(player, "Usage: /sms title <menu-name> <new-title>");
			return;
		}
		SMSMenu menu = SMSMenu.getMenu(args[1]);
		menu.setTitle(combine(args, 2));
		menu.autosave();
	}

	private void addSMSItem(Player player, String[] args) throws SMSNoSuchMenuException, SMSException {	
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
			menu.autosave();
		} else {
			SMSUtils.errorMessage(player, "You do not have permission to add that kind of command.");
		}
	}

	private void removeSMSItem(Player player, String[] args) throws SMSNoSuchMenuException, SMSException {
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
			menu.autosave();
			SMSUtils.statusMessage(player, "Menu entry &f#" + item + "&- removed from &e" + menuName);
		} catch (IndexOutOfBoundsException e) {
			SMSUtils.errorMessage(player, "Item index " + item + " out of range");
		} catch (IllegalArgumentException e) {
			SMSUtils.errorMessage(player, e.getMessage());
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
		
		messageBuffer.clear();
		if (args.length < 2) {
			for (String line : SMSConfig.getConfigList()) {
				messageBuffer.add(line);
			}
			pagedDisplay(player, 1);
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
			messageBuffer.clear();
			int i = 1;
			if (args.length < 3) {
				Set<String> macros = plugin.macroHandler.getCommands();
				messageBuffer.add(ChatColor.YELLOW + "" + macros.size() + " macros");
				for (String m : macros) {
					messageBuffer.add(ChatColor.YELLOW + "" + i++ + ") " + ChatColor.WHITE + m);
				}
			} else {
				List<String> cmds = plugin.macroHandler.getCommands(args[2]);
				messageBuffer.add(ChatColor.YELLOW + "" + cmds.size() + " macro entries");
				for (String c : cmds) {
					messageBuffer.add(ChatColor.YELLOW + "" + i++ + ") " + ChatColor.WHITE + c);
				}
			}
			pagedDisplay(player, 1);
		} else if (partialMatch(args[1], "a")) {	// add
			if (args.length >= 4) {
				String s = combine(args, 3);
				plugin.macroHandler.addCommand(args[2], s);
				SMSUtils.statusMessage(player, "Added command to macro &e" + args[2] + "&-.");
				needSave = true;
			}
		} else if (partialMatch(args[1], "r")) {	// remove
			if (args.length < 4) {
				plugin.macroHandler.removeCommand(args[2]);
				SMSUtils.statusMessage(player, "Removed macro &e" + args[2] + "&-.");
				needSave = true;
			} else {
				try { 
					int index = Integer.parseInt(args[3]);
					plugin.macroHandler.removeCommand(args[2], index - 1);
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

	private void pagedDisplay(Player player, String[] args) throws SMSException {
		
		int pageNum = 1;
		if (args.length < 2) return;
		try {
			pageNum = Integer.parseInt(args[1]);
			pagedDisplay(player, pageNum);
		} catch (NumberFormatException e) {
			SMSUtils.errorMessage(player, "invalid argument: " + args[1]);
		}
	}
	
	private void pagedDisplay(Player player, int pageNum) throws SMSException {
		SMSPermissions.requirePerms(player, "scrollingmenusign.commands.page");
		
		if (player != null) {
			// pretty paged display
			int nMessages = messageBuffer.size();
			SMSUtils.statusMessage(player, ChatColor.GREEN + "" +  nMessages +
					" lines (page " + pageNum + "/" + ((nMessages-1) / pageSize + 1) + ")");
			SMSUtils.statusMessage(player, ChatColor.GREEN + "---------------");
			for (int i = (pageNum -1) * pageSize; i < nMessages && i < pageNum * pageSize; i++) {
				SMSUtils.statusMessage(player, messageBuffer.get(i));
			}
			SMSUtils.statusMessage(player, ChatColor.GREEN + "---------------");
			String footer = (nMessages > pageSize * pageNum) ? "Use /sms page [page#] to see more" : "";
			SMSUtils.statusMessage(player, ChatColor.GREEN + footer);
		} else {
			// just dump the whole message buffer to the console
			for (String s: messageBuffer) {
				SMSUtils.statusMessage(null, SMSUtils.deColourise(s));
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

	private static Boolean partialMatch(String str, String match) {
		int l = match.length();
		if (str.length() < l) return false;
		return str.substring(0, l).equalsIgnoreCase(match);
	}

	private boolean onConsole(Player player) {
		if (player == null) {
			SMSUtils.errorMessage(player, "This command cannot be run from the console.");
			return true;
		} else {
			return false;
		}
	}
}
