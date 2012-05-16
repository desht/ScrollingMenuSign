package me.desht.scrollingmenusign;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.scrollingmenusign.parser.CommandParser;
import me.desht.scrollingmenusign.spout.SpoutUtils;
import me.desht.scrollingmenusign.util.SMSLogger;
import me.desht.scrollingmenusign.views.SMSSpoutView;
import me.desht.scrollingmenusign.views.SMSView;

import org.bukkit.configuration.Configuration;

public class SMSConfig {
	private static File pluginDir;
	private static File dataDir, menusDir, viewsDir, macrosDir, imgCacheDir;
	private static File commandFile;

	private static final String dataDirName = "data";
	private static final String menusDirName = "menus";
	private static final String viewsDirName = "views";
	private static final String macrosDirName = "macros";
	private static final String imgCacheDirName = "imagecache";
	private static final String commandFileName = "commands.yml";

	static void init(ScrollingMenuSign plugin) {
		setupDirectoryStructure();
		initConfigFile();
		SMSLogger.setLogLevel(getConfig().getString("sms.log_level", "INFO"));
	}

	private static void setupDirectoryStructure() {
		pluginDir = ScrollingMenuSign.getInstance().getDataFolder();

		commandFile = new File(pluginDir, commandFileName);
		dataDir = new File(pluginDir, dataDirName);
		menusDir = new File(dataDir, menusDirName);
		viewsDir = new File(dataDir, viewsDirName);
		macrosDir = new File(dataDir, macrosDirName);
		imgCacheDir = new File(pluginDir, imgCacheDirName);

		createDirectory(pluginDir);
		createDirectory(dataDir);
		createDirectory(menusDir);
		createDirectory(viewsDir);
		createDirectory(macrosDir);
		createDirectory(imgCacheDir);
	}

	private static void createDirectory(File dir) {
		if (dir.isDirectory()) {
			return;
		}
		if (!dir.mkdir()) {
			SMSLogger.warning("Can't make directory " + dir.getName()); //$NON-NLS-1$
		}
	}

	private static void initConfigFile() {
		boolean saveNeeded = false;
		Configuration config = getConfig();

		// check if there is anything in the defaults which isn't in our live config file
		for (String k : config.getDefaults().getKeys(true)) {
			if (!config.isSet(k)) {
				saveNeeded = true;
			}
		}

		if (config.getString("sms.menuitem_separator").equals("\\|")) {
			// special case - convert from v0.3 or older where it was a regexp
			config.set("sms.menuitem_separator", "|");
			saveNeeded = true;
		}

		if (config.contains("sms.use_any_view")) {
			// migrate the old & confusingly-named "use_any_view" setting
			config.set("sms.ignore_view_ownership", config.getBoolean("sms.use_any_view"));
			config.set("sms.use_any_view", null);
			saveNeeded = true;
		}

		if (saveNeeded) {
			getConfig().options().copyDefaults(true);
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

	public static File getImgCacheFolder() {
		return imgCacheDir;
	}
	
	public static Configuration getConfig() {
		return ScrollingMenuSign.getInstance().getConfig();
	}

	public static void setPluginConfiguration(String key, String val) throws SMSException {
		if (!key.startsWith("sms.")) {
			key = "sms." + key;
		}

		setConfigItem(getConfig(), key, val);

		// special hooks

		if (key.equalsIgnoreCase("sms.ignore_view_ownership")) {
			// redraw map views
			repaintViews("map");
		} else if (key.startsWith("sms.actions.spout") && ScrollingMenuSign.getInstance().isSpoutEnabled()) {
			// reload & re-cache spout key definitions
			SpoutUtils.loadKeyDefinitions();
		} else if (key.startsWith("sms.spout.") && ScrollingMenuSign.getInstance().isSpoutEnabled()) {
			// catch-all for any setting which affects how spout views are drawn
			repaintViews("spout");
		} else if (key.equalsIgnoreCase("sms.command_log_file")) {
			CommandParser.setLogFile(val);
		} else if (key.equalsIgnoreCase("sms.log_level")) {
			SMSLogger.setLogLevel(val);
		} else if (key.startsWith("sms.item_prefix.") || key.endsWith("_justify")) {
			repaintViews(null);
		}

		ScrollingMenuSign.getInstance().saveConfig();
	}

	private static void repaintViews(String type) {
		for (SMSView v : SMSView.listViews()) {
			if (type == null || v.getType().equals(type)) {
				v.update(v.getMenu(), SMSMenuAction.REPAINT);
			}
			if (v instanceof SMSSpoutView) {
				((SMSSpoutView) v).rejustify();
			}
		}
	}
	
	public static <T> void setPluginConfiguration(String key, List<T> list) throws SMSException {
		if (!key.startsWith("sms.")) {
			key = "sms." + key;
		}

		setConfigItem(getConfig(), key, list);

		ScrollingMenuSign.getInstance().saveConfig();
	}

	/**
	 * Get a formatted list of all plugin configuration items.
	 * 
	 * @return	A list of strings, each representing a key/value pair.
	 */
	public static List<String> getPluginConfiguration() {
		ArrayList<String> res = new ArrayList<String>();
		Configuration config = getConfig();
		for (String k : config.getDefaults().getKeys(true)) {
			if (config.isConfigurationSection(k))
				continue;
			res.add("&f" + k.replaceAll("^sms\\.", "") + "&- = '&e" + config.get(k) + "&-'");
		}
		Collections.sort(res);
		return res;
	}

	public static Object getPluginConfiguration(String key) throws SMSException {
		if (!key.startsWith("sms.")) {
			key = "sms." + key;
		}
		if (!SMSConfig.getConfig().contains(key)) {
			throw new SMSException("No such config item: " + key);
		}

		return getConfig().get(key);
	}

	/**
	 * Sets a configuration item in the given config object.  The key and value are both strings; the value
	 * will be converted into an object of the correct type, if possible (where the type is discovered from
	 * the config's default object).  The type's class must provide a constructor which takes a single string
	 * or an exception will be thrown.
	 * 
	 * @param config	The configuration object
	 * @param key		The configuration key
	 * @param val		The value
	 * @throws SMSException	if the key is unknown or a bad numeric value is passed
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void setConfigItem(Configuration config, String key, String val) throws SMSException {
		Configuration defaults = config.getDefaults();
		if (!defaults.contains(key)) {
			throw new SMSException("No such config key '" + key + "'");
		}
		if (defaults.get(key) instanceof List<?>) {
			List<String>list = new ArrayList<String>(1);
			list.add(val);
			handleListValue(config, key, list);
		} else if (defaults.get(key) instanceof String) {
			// should be marginally quicker than going through the default method below...
			config.set(key, val);
		} else if (defaults.get(key) instanceof Enum<?>) {
			// this really isn't very pretty, but as far as I can tell there's no way to
			// do this with a parameterised Enum type
			Class<?> c0 = defaults.get(key).getClass();		
			Class<? extends Enum> c = c0.asSubclass(Enum.class);
			try {
				config.set(key, Enum.valueOf(c, val.toUpperCase()));
			} catch (IllegalArgumentException e) {
				throw new SMSException("'" + val + "' is not a valid value for '" + key + "'");
			}
		} else {
			// the class we're converting to needs to have a constructor taking a single String argument
			Class<?> c = null;
			try {
				c = defaults.get(key).getClass();
				Constructor<?> ctor = c.getDeclaredConstructor(String.class);
				config.set(key, ctor.newInstance(val));
			} catch (NoSuchMethodException e) {
				throw new SMSException("Don't know how to convert '" + val + "' into a " + c.getName());
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				if (e.getCause() instanceof NumberFormatException) {
					throw new SMSException("Invalid numeric value: " + val);
				} else {
					e.printStackTrace();
				}
			}
		}
	}

	public static <T> void setConfigItem(Configuration config, String key, List<T> list) throws SMSException {
		if (config.getDefaults().get(key) == null) {
			throw new SMSException("No such key '" + key + "'");
		}
		if (!(config.getDefaults().get(key) instanceof List<?>)) {
			throw new SMSException("Key '" + key + "' does not accept a list of values");
		}
		handleListValue(config, key, list);
	}

	@SuppressWarnings("unchecked")
	private static <T> void handleListValue(Configuration config, String key, List<T> list) {
		HashSet<T> current = new HashSet<T>((List<T>)config.getList(key));
		
		if (list.get(0).equals("-")) {
			// remove specifed item from list
			list.remove(0);
			current.removeAll(list);
		} else if (list.get(0).equals("=")) {
			// replace list
			list.remove(0);
			current = new HashSet<T>(list);
		} else if (list.get(0).equals("+")) {
			// append to list
			list.remove(0);
			current.addAll(list);
		} else {
			// append to list
			current.addAll(list);
		}

		config.set(key, new ArrayList<T>(current));
	}
}
