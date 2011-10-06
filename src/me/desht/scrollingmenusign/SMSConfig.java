package me.desht.scrollingmenusign;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.scrollingmenusign.views.SMSMapView;
import me.desht.scrollingmenusign.views.SMSView;
import me.desht.util.MiscUtil;
import me.desht.util.PermissionsUtils;

import org.bukkit.entity.Player;
import org.bukkit.util.config.Configuration;

public class SMSConfig {
	private static ScrollingMenuSign plugin = null;

	private static File pluginDir;
	private static File dataDir, menusDir, viewsDir, macrosDir;
	private static File commandFile;
	
	private static final String dataDirName = "data";
	private static final String menusDirName = "menus";
	private static final String viewsDirName = "views";
	private static final String macrosDirName = "macros";
	private static final String commandFileName = "commands.yml";

	private static final String SAMPLE_NODE = "a.sample.permission.node";
	
	@SuppressWarnings("serial")
	private static final Map<String, Object> configDefaults = new HashMap<String, Object>() {{
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
		put("sms.actions.spout.up", "key_up");
		put("sms.actions.spout.down", "key_down");
		put("sms.actions.spout.execute", "key_return");
		List<String>samplePerm = new ArrayList<String>();
		samplePerm.add(SAMPLE_NODE);
		put("sms.elevation.nodes", samplePerm);
		put("sms.elevation.grant_op", false);
		put("sms.use_any_view", true);
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
		macrosDir = new File(dataDir, macrosDirName);

		createDirectory(pluginDir);
		createDirectory(dataDir);
		createDirectory(menusDir);
		createDirectory(viewsDir);
		createDirectory(macrosDir);
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

		if (config.getStringList("sms.elevation.nodes", null).get(0).equals(SAMPLE_NODE)) {
			// initialise default nodes from the &SMS user
			String user = getConfiguration().getString("sms.elevation_user", "&SMS");
			List<String> nodes = PermissionsUtils.getPermissionNodes(user, null);
			getConfiguration().setProperty("sms.elevation.nodes", nodes);
			MiscUtil.log(Level.INFO, "Migrated " + nodes.size() + " permissions nodes from " + user + " to  elevation.nodes config item");
			saveNeeded = true;
		}
		
		if (saveNeeded) {
			config.save();
		}
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

	public static File getMacrosFolder() {
		return macrosDir;
	}

	public static File getViewsFolder() {
		return viewsDir;
	}

	public static void setConfigItem(Player player, String key, String val) throws SMSException {
		if (!key.startsWith("sms.")) {
			key = "sms." + key;
		}
		if (configDefaults.get(key) == null) {
			throw new SMSException("No such config key '" + key + "'");
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
		} else if (configDefaults.get(key) instanceof List<?>) {
			List<String>list = new ArrayList<String>(1);
			list.add(val);
			handleListValue(key, list);
		} else {
			getConfiguration().setProperty(key, val);
		}

		// special hooks
		 
		if (key.equalsIgnoreCase("sms.use_any_view")) {
			// redraw map views
			for (SMSView v : SMSView.listViews()) {
				if (v instanceof SMSMapView)
					v.update(v.getMenu(), SMSMenuAction.REPAINT);
			}
		}
		
		if (key.startsWith("sms.actions.spout") && plugin.isSpoutEnabled()) {
			// reload & re-cache spout key definitions
			SpoutUtils.loadKeyDefinitions();
		}
		
		getConfiguration().save();
	}

	public static void setConfigItem(Player player, String key, List<String> list) throws SMSException {
		if (!key.startsWith("sms.")) {
			key = "sms." + key;
		}
		if (configDefaults.get(key) == null) {
			throw new SMSException("No such config key '" + key + "'");
		}
		if (!(configDefaults.get(key) instanceof List<?>))
			throw new SMSException("Config item '" + key + "' does not accept a list of values");

		handleListValue(key, list);

		getConfiguration().save();
	}

	private static void handleListValue(String key, List<String> list) {
		HashSet<String> current;
		
		if (list.get(0).equals("-")) {
			// remove specifed item from list
			list.remove(0);
			current = new HashSet<String>(getConfiguration().getStringList(key, null));
			current.removeAll(list);
		} else if (list.get(0).equals("=")) {
			// replace list
			list.remove(0);
			current = new HashSet<String>(list);
		} else if (list.get(0).equals("+")) {
			// append to list
			list.remove(0);
			current = new HashSet<String>(getConfiguration().getStringList(key, null));
			current.addAll(list);
		} else {
			// append to list
			current = new HashSet<String>(getConfiguration().getStringList(key, null));
			current.addAll(list);
		}
		
		getConfiguration().setProperty(key, new ArrayList<String>(current));
	}

	// return a sorted list of all config keys
	public static List<String> getConfigList() {
		ArrayList<String> res = new ArrayList<String>();
		for (String k : configDefaults.keySet()) {
			res.add(k + " = '&e" + getConfiguration().getString(k) + "&-'");
		}
		Collections.sort(res);
		return res;
	}

	public static Configuration getConfiguration() {
		return plugin.getConfiguration();
	}

	public static String getConfigItem(String key) {
		if (!key.startsWith("sms.")) {
			key = "sms." + key;
		}
		return getConfiguration().getString(key);
	}
}
