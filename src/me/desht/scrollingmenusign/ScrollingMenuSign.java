package me.desht.scrollingmenusign;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.*;

import com.edwardhand.commandsigns.CommandSigns;
import com.edwardhand.commandsigns.CommandSignsHandler;
import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;

@SuppressWarnings("serial")
public class ScrollingMenuSign extends JavaPlugin {
	static enum MenuRemoveAction { DESTROY_SIGN, BLANK_SIGN, DO_NOTHING };
	static PluginDescriptionFile description;
	static final String directory = "plugins" + File.separator + "ScrollingMenuSign";

	PermissionHandler permissionHandler;
	CommandSignsHandler csHandler;
	SMSCommandFile commandFile;
	
	private final SMSPlayerListener signListener = new SMSPlayerListener(this);
	private final SMSBlockListener blockListener = new SMSBlockListener(this);
	private final SMSCommandExecutor commandExecutor = new SMSCommandExecutor(this);
	private final SMSPersistence persistence = new SMSPersistence(this);
	private final SMSEntityListener entityListener = new SMSEntityListener(this);
	
	final Logger logger = Logger.getLogger("Minecraft");
	final SMSDebugger debugger = new SMSDebugger(this);

	private static final Map<String, Object> configItems = new HashMap<String, Object>() {{
		put("sms.always_use_commandsigns", true);
		put("sms.autosave", true);
		put("sms.no_physics", false);
		put("sms.no_explosions", false);
		put("sms.item_prefix.not_selected", "  ");
		put("sms.item_prefix.selected", "> ");
		put("sms.item_justify", "left");
		put("sms.menuitem_separator", "|");
		put("sms.actions.leftclick.normal", "execute");
		put("sms.actions.leftclick.sneak", "none");
		put("sms.actions.rightclick.normal", "scrolldown");
		put("sms.actions.rightclick.sneak", "scrollup");
		put("sms.actions.wheelup.normal", "none");
		put("sms.actions.wheelup.sneak", "scrollup");
		put("sms.actions.wheeldown.normal", "none");
		put("sms.actions.wheeldown.sneak", "scrolldown");
	}};
	
	@Override
	public void onEnable() {
		description = this.getDescription();

		configInitialise();

		setupPermissions();
		setupCommandSigns();
		setupCommandFile();
		
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Event.Type.PLAYER_INTERACT, signListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_ITEM_HELD, signListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_DAMAGE, blockListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_BREAK, blockListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_PHYSICS, blockListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.ENTITY_EXPLODE, entityListener, Event.Priority.Normal, this);

		getCommand("sms").setExecutor(commandExecutor);
		
		if (!getDataFolder().exists()) getDataFolder().mkdir();

		logger.info(description.getName() + " version " + description.getVersion() + " is enabled!" );
		
		// delayed loading of saved menu files to ensure all worlds are loaded first
		if (getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
			@Override
			public void run() {
				loadMenus();
			}
		})==-1) {
			SMSUtils.log(Level.WARNING, "Couldn't schedule menu loading - multiworld support might not work.");
			loadMenus();
		}
		loadMacros();
	}

	@Override
	public void onDisable() {
		saveMenus();
		saveMacros();
		logger.info(description.getName() + " version " + description.getVersion() + " is disabled!" );
	}

	private void configInitialise() {
		Boolean saveNeeded = false;
		Configuration config = getConfiguration();
		for (String k : configItems.keySet()) {
			if (config.getProperty(k) == null) {
				saveNeeded = true;
				config.setProperty(k, configItems.get(k));
			}
		}
		
		if (config.getString("sms.menuitem_separator").equals("\\|")) {
			// special case - convert from v0.3 or older where it was a regexp
			config.setProperty("sms.menuitem_separator", "|");
			saveNeeded = true;
		}
		
		if (saveNeeded) config.save();
	}

	private void setupPermissions() {
		Plugin permissionsPlugin = this.getServer().getPluginManager().getPlugin("Permissions");

		if (permissionHandler == null) {
			if (permissionsPlugin != null) {
				permissionHandler = ((Permissions) permissionsPlugin).getHandler();
				SMSUtils.log(Level.INFO, "Permissions detected");
			} else {
				SMSUtils.log(Level.INFO, "Permissions not detected, using ops");
			}
		}
	}

	private void setupCommandSigns() {
		Plugin csPlugin = this.getServer().getPluginManager().getPlugin("CommandSigns");
		if (csHandler == null) {
			if (csPlugin != null) {
				csHandler = ((CommandSigns) csPlugin).getHandler();
				SMSUtils.log(Level.INFO, "CommandSigns API integration enabled");
			} else {
				SMSUtils.log(Level.INFO, "CommandSigns API not available");
			}
		}

	}

	private void setupCommandFile() {
		if (commandFile == null) {
			commandFile = new SMSCommandFile(this);
		}
	}

	Boolean isAllowedTo(Player player, String node) {
		if (player == null) return true;
		// if Permissions is in force, then it overrides op status
		if (permissionHandler != null) {
			return permissionHandler.has(player, node);
		} else {
			return player.isOp();
		}
	}
	
	Boolean isAllowedTo(Player player, String node, Boolean okNotOp) {
		if (player == null) return true;
		// if Permissions is in force, then it overrides op status
		if (permissionHandler != null) {
			return permissionHandler.has(player, node);
		} else {
			return okNotOp ? true : player.isOp();
		}
	}
	
	void loadMenus() {
		persistence.load();
	}
	
	void loadMacros() {
		commandFile.loadCommands();
	}
	
	void saveMenus() {
		persistence.save();
	}
	
	void saveMacros() {
		commandFile.saveCommands();
	}

	String parseColourSpec(Player player, String spec) {
		if (player == null ||
				isAllowedTo(player, "scrollingmenusign.coloursigns") || 
				isAllowedTo(player, "scrollingmenusign.colorsigns"))
		{
			String res = spec.replaceAll("&(?<!&&)(?=[0-9a-fA-F])", "\u00A7");
			return res.replace("&&", "&");
		} else {
			return spec;
		}		
	}
		
	static String unParseColourSpec(String spec) {
		return spec.replaceAll("\u00A7", "&");
	}
	
	// Return the name of the menu sign that the player is looking at, if any
	String getTargetedMenuSign(Player player, Boolean complain) {
		Block b = player.getTargetBlock(null, 3);
		if (b.getType() != Material.SIGN_POST && b.getType() != Material.WALL_SIGN) {
			if (complain) SMSUtils.errorMessage(player, "You are not looking at a sign.");
			return null;
		}
		String name = SMSMenu.getMenuNameAt(b.getLocation());
		if (name == null && complain)
			SMSUtils.errorMessage(player, "There is no menu associated with that sign.");
		return name;
	}

	void setConfigItem(Player player, String key, String val) {
		if (key.length() < 5 || !key.substring(0, 4).equals("sms.")) {
			key = "sms." + key;
		}
		if (configItems.get(key) == null) {
			SMSUtils.errorMessage(player, "No such config key " + key);
			SMSUtils.errorMessage(player, "Use /sms getcfg to list all valid keys");
			return;
		}
		if (configItems.get(key) instanceof Boolean) {
			Boolean bVal = false;
			if (val.equals("false") || val.equals("no")) {
				bVal = false;
			} else if (val.equals("true") || val.equals("yes")) {
				bVal = true;
			} else {
				SMSUtils.errorMessage(player, "Invalid boolean value " + val + " - use true/yes or false/no.");
				return;
			}
			getConfiguration().setProperty(key, bVal);
		} else if (configItems.get(key) instanceof Integer) {
			try {
				int nVal = Integer.parseInt(val);
				getConfiguration().setProperty(key, nVal);
			} catch (NumberFormatException e) {
				SMSUtils.errorMessage(player, "Invalid numeric value: " + val);
			}
		} else {
			getConfiguration().setProperty(key, val);
		}
		SMSUtils.statusMessage(player, key + " is now set to " + val);
		getConfiguration().save();
	}
	
	// return a sorted list of all config keys
	List<String> getConfigList() {
		ArrayList<String> res = new ArrayList<String>();
		for (String k : configItems.keySet()) {
			res.add(k + " = '" + getConfiguration().getString(k) + "'");
		}
		Collections.sort(res);
		return res;
	}

	void setTitle(Player player, String menuName, String newTitle) throws SMSNoSuchMenuException {
		SMSMenu menu = SMSMenu.getMenu(menuName);
		menu.setTitle(parseColourSpec(player, newTitle));
		SMSUtils.statusMessage(player, "title for '" + menuName + "' is now '" + newTitle + "'");
		menu.updateSigns();
	}

	static String deColourise(String s) {
		return s.replaceAll("\u00A7.", "");
	}
	
	void debug(String message) {
		debugger.debug(message);
	}
	
	void maybeSaveMenus() {
		if (getConfiguration().getBoolean("sms.autosave", false)) {
			saveMenus();
		}
	}
	

}
