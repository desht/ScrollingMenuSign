package me.desht.scrollingmenusign;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.entity.Player;
import org.bukkit.util.config.Configuration;

public class SMSConfig {
	private static ScrollingMenuSign plugin = null;

	private static File pluginDir;
	private static File dataDir, menusDir;
	private static File commandFile;
	
	private static final String dataDirName = "data";
	private static final String menusDirName = "menus";
	private static final String commandFileName = "commands.yml";
	
	@SuppressWarnings("serial")
	private static final Map<String, Object> configItems = new HashMap<String, Object>() {{
		put("sms.always_use_commandsigns", true);
		put("sms.autosave", true);
		put("sms.no_physics", false);
		put("sms.no_explosions", false);
		put("sms.item_prefix.not_selected", "  ");
		put("sms.item_prefix.selected", "> ");
		put("sms.item_justify", "left");
		put("sms.legacy_sms_add", false);
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
		
		createDirectory(pluginDir);
		createDirectory(dataDir);
		createDirectory(menusDir);
	}

	private static void createDirectory(File dir) {
		if (dir.isDirectory()) {
			return;
		}
		if (!dir.mkdir()) {
			SMSUtils.log(Level.WARNING, "Can't make directory " + dir.getName()); //$NON-NLS-1$
		}
	}

	private static void initConfigFile() {
		Boolean saveNeeded = false;
		Configuration config = plugin.getConfiguration();
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
	
	static File getCommandFile() {
		return commandFile;
	}
	
	static File getPluginFolder() {
		return pluginDir;
	}
	
	static File getDataFolder() {
		return dataDir;
	}
	
	static File getMenusFolder() {
		return menusDir;
	}

	static void setConfigItem(Player player, String key, String val) {
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
		
		getConfiguration().save();
	}
	
	// return a sorted list of all config keys
	static List<String> getConfigList() {
		ArrayList<String> res = new ArrayList<String>();
		for (String k : configItems.keySet()) {
			res.add(k + " = '" + getConfiguration().getString(k) + "'");
		}
		Collections.sort(res);
		return res;
	}
	
	static Configuration getConfiguration() {
		return plugin.getConfiguration();
	}
}
