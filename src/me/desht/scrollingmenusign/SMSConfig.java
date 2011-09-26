package me.desht.scrollingmenusign;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import me.desht.util.MiscUtil;

import org.bukkit.entity.Player;
import org.bukkit.util.config.Configuration;

public class SMSConfig {
	private static ScrollingMenuSign plugin = null;

	private static File pluginDir;
	private static File dataDir, menusDir, viewsDir;
	private static File commandFile;
	
	private static final String dataDirName = "data";
	private static final String menusDirName = "menus";
	private static final String viewsDirName = "views";
	private static final String commandFileName = "commands.yml";
	
	@SuppressWarnings("serial")
	private static final Map<String, Object> configDefaults = new HashMap<String, Object>() {{
		put("sms.always_use_commandsigns", true);
		put("sms.autosave", true);
		put("sms.no_physics", false);
		put("sms.no_explosions", false);
		put("sms.no_destroy_signs", false);
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
		put("sms.elevation_user", "&SMS");
	}};
	
	static void init(ScrollingMenuSign plugin) {
		SMSConfig.plugin = plugin;
		if (plugin != null) {
			pluginDir = plugin.getDataFolder();
		}
		
		setupDirectoryStructure();
		
		initConfigFile();
	}

	private static void setupDirectoryStructure() {
		commandFile = new File(pluginDir, commandFileName);
		dataDir = new File(pluginDir, dataDirName);
		menusDir = new File(dataDir, menusDirName);
		viewsDir = new File(dataDir, viewsDirName);
		
		createDirectory(pluginDir);
		createDirectory(dataDir);
		createDirectory(menusDir);
		createDirectory(viewsDir);
	}

	private static void createDirectory(File dir) {
		if (dir.isDirectory()) {
			return;
		}
		if (!dir.mkdir()) {
			MiscUtil.log(Level.WARNING, "Can't make directory " + dir.getName()); //$NON-NLS-1$
		}
	}

	private static void initConfigFile() {
		Boolean saveNeeded = false;
		Configuration config = plugin.getConfiguration();
		for (String k : configDefaults.keySet()) {
			if (config.getProperty(k) == null) {
				saveNeeded = true;
				config.setProperty(k, configDefaults.get(k));
			}
		}
		
		if (config.getString("sms.menuitem_separator").equals("\\|")) {
			// special case - convert from v0.3 or older where it was a regexp
			config.setProperty("sms.menuitem_separator", "|");
			saveNeeded = true;
		}
		
		if (saveNeeded) config.save();
	}
	
	public static File getCommandFile() {
		return commandFile;
	}
	
	public static File getPluginFolder() {
		return pluginDir;
	}
	
	public static File getDataFolder() {
		return dataDir;
	}
	
	public static File getMenusFolder() {
		return menusDir;
	}

	public static File getViewsFolder() {
		return viewsDir;
	}
	
	public static void setConfigItem(Player player, String key, String val) throws SMSException {
		if (!key.startsWith("sms.")) {
			key = "sms." + key;
		}
		if (configDefaults.get(key) == null) {
			throw new SMSException("No such config key " + key);
		}
		if (configDefaults.get(key) instanceof Boolean) {
			Boolean bVal = false;
			if (val.equalsIgnoreCase("false") || val.equalsIgnoreCase("no")) {
				bVal = false;
			} else if (val.equalsIgnoreCase("true") || val.equalsIgnoreCase("yes")) {
				bVal = true;
			} else {
				MiscUtil.errorMessage(player, "Invalid boolean value " + val + " - use true/yes or false/no.");
				return;
			}
			getConfiguration().setProperty(key, bVal);
		} else if (configDefaults.get(key) instanceof Integer) {
			try {
				int nVal = Integer.parseInt(val);
				getConfiguration().setProperty(key, nVal);
			} catch (NumberFormatException e) {
				throw new SMSException("Invalid numeric value: " + val);
			}
		} else {
			getConfiguration().setProperty(key, val);
		}
		
		getConfiguration().save();
	}
	
	// return a sorted list of all config keys
	public static List<String> getConfigList() {
		ArrayList<String> res = new ArrayList<String>();
		for (String k : configDefaults.keySet()) {
			res.add(k + " = '" + getConfiguration().getString(k) + "'");
		}
		Collections.sort(res);
		return res;
	}
	
	public static Configuration getConfiguration() {
		return plugin.getConfiguration();
	}
}
