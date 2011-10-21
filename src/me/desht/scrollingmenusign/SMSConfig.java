package me.desht.scrollingmenusign;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;

import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.scrollingmenusign.spout.SpoutUtils;
import me.desht.scrollingmenusign.views.SMSMapView;
import me.desht.scrollingmenusign.views.SMSView;
import me.desht.util.MiscUtil;
import me.desht.util.PermissionsUtils;

import org.bukkit.configuration.Configuration;

public class SMSConfig {
	private static File pluginDir;
	private static File dataDir, menusDir, viewsDir, macrosDir;
	private static File commandFile;
	
	private static final String dataDirName = "data";
	private static final String menusDirName = "menus";
	private static final String viewsDirName = "views";
	private static final String macrosDirName = "macros";
	private static final String commandFileName = "commands.yml";

	private static final String SAMPLE_NODE = "a.sample.permission.node";

	static void init(ScrollingMenuSign plugin) {
		setupDirectoryStructure();
		initConfigFile();
	}

	private static void setupDirectoryStructure() {
		pluginDir = ScrollingMenuSign.getInstance().getDataFolder();

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
		boolean saveNeeded = false;
		getConfig().options().copyDefaults(true);
		Configuration config = ScrollingMenuSign.getInstance().getConfig();
		
		for (String k : getConfig().getDefaults().getKeys(true)) {
			if (!config.contains(k)) {
				saveNeeded = true;
			}
		}

		if (config.getString("sms.menuitem_separator").equals("\\|")) {
			// special case - convert from v0.3 or older where it was a regexp
			config.set("sms.menuitem_separator", "|");
			saveNeeded = true;
		}

		@SuppressWarnings("unchecked")
		List<String> nodeList = config.getList("sms.elevation.nodes");
		if (nodeList.size() == 1 && nodeList.get(0).equals(SAMPLE_NODE)) {
			// initialise default nodes from the &SMS user
			String user = getConfig().getString("sms.elevation_user", "&SMS");
			List<String> nodes = PermissionsUtils.getPermissionNodes(user, null);
			getConfig().set("sms.elevation.nodes", nodes);
			MiscUtil.log(Level.INFO, "Migrated " + nodes.size() + " permissions nodes from " + user + " to elevation.nodes config item");
			saveNeeded = true;
		}
		
		if (config.contains("sms.use_any_view")) {
			// migrate the old & confusingly-named "use_any_view" setting
			config.set("sms.ignore_view_ownership", config.getBoolean("sms.use_any_view"));
			config.set("sms.use_any_view", null);
			saveNeeded = true;
		}
		
		if (saveNeeded) {
			ScrollingMenuSign.getInstance().saveConfig();
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

	public static void setPluginConfiguration(String key, String val) throws SMSException {
		if (!key.startsWith("sms.")) {
			key = "sms." + key;
		}

		setConfigItem(getConfig(), key, val);
		
		// special hooks
		 
		if (key.equalsIgnoreCase("sms.ignore_view_ownership")) {
			// redraw map views
			for (SMSView v : SMSView.listViews()) {
				if (v instanceof SMSMapView)
					v.update(v.getMenu(), SMSMenuAction.REPAINT);
			}
		}
		
		if (key.startsWith("sms.actions.spout") && ScrollingMenuSign.getInstance().isSpoutEnabled()) {
			// reload & re-cache spout key definitions
			SpoutUtils.loadKeyDefinitions();
		}
		
		ScrollingMenuSign.getInstance().saveConfig();
	}

	public static void setPluginConfiguration(String key, List<String> list) throws SMSException {
		if (!key.startsWith("sms.")) {
			key = "sms." + key;
		}
		
		setConfigItem(getConfig(), key, list);
		
		ScrollingMenuSign.getInstance().saveConfig();
	}

	@SuppressWarnings("unchecked")
	private static void handleListValue(Configuration config, String key, List<String> list) {
		HashSet<String> current;
		
		if (list.get(0).equals("-")) {
			// remove specifed item from list
			list.remove(0);
			current = new HashSet<String>(config.getList(key));
			current.removeAll(list);
		} else if (list.get(0).equals("=")) {
			// replace list
			list.remove(0);
			current = new HashSet<String>(list);
		} else if (list.get(0).equals("+")) {
			// append to list
			list.remove(0);
			current = new HashSet<String>(config.getList(key));
			current.addAll(list);
		} else {
			// append to list
			current = new HashSet<String>(config.getList(key));
			current.addAll(list);
		}
		
		config.set(key, new ArrayList<String>(current));
	}

	// return a sorted list of all config keys
	public static List<String> getConfigList() {
		ArrayList<String> res = new ArrayList<String>();
		for (String k : getConfig().getDefaults().getKeys(true)) {
			if (getConfig().isConfigurationSection(k))
				continue;
			res.add(k + " = '&e" + getConfig().get(k) + "&-'");
		}
		Collections.sort(res);
		return res;
	}

	public static Configuration getConfig() {
		return ScrollingMenuSign.getInstance().getConfig();
	}

	public static Object getConfigItem(String key) {
		if (!key.startsWith("sms.")) {
			key = "sms." + key;
		}
		return getConfig().get(key);
	}
	
	public static void setConfigItem(Configuration config, String key, String val) throws SMSException {
		Configuration defaults = config.getDefaults();
		if (!defaults.contains(key)) {
			throw new SMSException("No such config key '" + key + "'");
		}
		if (defaults.get(key) instanceof Boolean) {
			config.set(key, Boolean.valueOf(val));
		} else if (defaults.get(key) instanceof Integer) {
			try {
				int nVal = Integer.parseInt(val);
				config.set(key, nVal);
			} catch (NumberFormatException e) {
				throw new SMSException("Invalid numeric value: " + val);
			}
		} else if (defaults.get(key) instanceof List<?>) {
			List<String>list = new ArrayList<String>(1);
			list.add(val);
			handleListValue(config, key, list);
		} else if (defaults.get(key) instanceof String) {
			config.set(key, val);
		} else {
			// the class we're converting to needs to have a constructor taking a single String argument
			try {
				Class<?> c = defaults.get(key).getClass();
				Constructor<?> ctor = c.getDeclaredConstructor(String.class);
				config.set(key, ctor.newInstance(val));
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void setConfigItem(Configuration config, String key, List<String> list) throws SMSException {
		if (config.getDefaults().get(key) == null) {
			throw new SMSException("No such key '" + key + "'");
		}
		if (!(config.getDefaults().get(key) instanceof List<?>)) {
			throw new SMSException("Key '" + key + "' does not accept a list of values");
		}
		handleListValue(config, key, list);
	}
}
