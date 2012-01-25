package me.desht.scrollingmenusign;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.scrollingmenusign.util.MiscUtil;
import me.desht.scrollingmenusign.views.SMSView;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class SMSPersistence {

	private static final FilenameFilter ymlFilter = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
			return name.endsWith(".yml");
		}
	};

	public static void unPersist(Freezable object) {
		File saveFile = new File(object.getSaveFolder(), object.getName() + ".yml");
		if (!saveFile.delete()) {
			MiscUtil.log(Level.WARNING, "can't delete " + saveFile);
		}
	}

	public static void save(Freezable object) {
		File saveFile = new File(object.getSaveFolder(), object.getName() + ".yml");
		YamlConfiguration conf = new YamlConfiguration();
		Map<String, Object> map = object.freeze();
		expandMapIntoConfig(conf, map);
		try {
			conf.save(saveFile);
		} catch (IOException e) {
			MiscUtil.log(Level.SEVERE, "Can't save " + saveFile + ": " + e.getMessage());
		}
	}

	/**
	 * Given a (possibly) nested map object, expand it into a ConfigurationSection,
	 * using recursion if necessary.
	 * 
	 * @param conf	The ConfigurationSection to put the map into
	 * @param map	The map object
	 */
	@SuppressWarnings("unchecked")
	public static void expandMapIntoConfig(ConfigurationSection conf, Map<String, Object> map) {
		for (Entry<String, Object> e : map.entrySet()) {
			if (e.getValue() instanceof Map<?,?>) {
				ConfigurationSection section = conf.createSection(e.getKey());
				Map<String,Object> subMap = (Map<String, Object>) e.getValue();
				expandMapIntoConfig(section, subMap);
			} else {
				conf.set(e.getKey(), e.getValue());
			}
		}
	}

	public static void saveMenusAndViews() {
		for (SMSMenu menu : SMSMenu.listMenus()) {
			save(menu);
		}
		for (SMSView view : SMSView.listViews()) {
			save(view);
		}
		MiscUtil.log(Level.INFO, "saved " + SMSMenu.listMenus().size() + " menus and " +
		             SMSView.listViews().size() + " views to file.");
	}

	public static void loadMenusAndViews() {
		loadMenus();
		loadViews();
	}

	public static void loadMacros() {
		final File oldMacrosFile = new File(SMSConfig.getPluginFolder(), "commands.yml");

		for (SMSMacro macro : SMSMacro.listMacros()) {
			macro.deleteTemporary();
		}

		if (oldMacrosFile.exists()) {
			oldStyleMacroLoad(oldMacrosFile);
			oldMacrosFile.renameTo(new File(oldMacrosFile.getParent(), oldMacrosFile.getName() + ".OLD"));
			MiscUtil.log(Level.INFO, "Converted old-style macro data file to new v0.8+ format");
		} else {
			for (File f : SMSConfig.getMacrosFolder().listFiles(ymlFilter)) {
				YamlConfiguration conf = YamlConfiguration.loadConfiguration(f);
				SMSMacro m = new SMSMacro(conf);
				SMSMacro.addMacro(m);
			}
			MiscUtil.log(Level.INFO, "Loaded " + SMSMacro.listMacros().size() + " macros from file.");
		}
	}

	public static void saveMacros() {
		for (SMSMacro macro : SMSMacro.listMacros()) {
			save(macro);
		}
		MiscUtil.log(Level.INFO, "saved " + SMSMacro.listMacros().size() + " macros to file.");
	}

	private static void loadViews() {
		for (SMSView view : SMSView.listViews()) {
			view.deleteTemporary();
		}

		for (File f : SMSConfig.getViewsFolder().listFiles(ymlFilter)) {
			YamlConfiguration conf = YamlConfiguration.loadConfiguration(f);
			SMSView view = SMSView.load(conf);
			if (view != null) {
				view.getMenu().addObserver(view);
				view.update(view.getMenu(), SMSMenuAction.REPAINT);
			}
		}

		MiscUtil.log(Level.INFO, "Loaded " + SMSView.listViews().size() + " views from file.");
	}

	private static void loadMenus() {
		final File oldMenusFile = new File(SMSConfig.getPluginFolder(), "scrollingmenus.yml");

		for (SMSMenu menu : SMSMenu.listMenus()) {
			menu.deleteTemporary();
		}

		if (oldMenusFile.exists()) {
			// old-style data file, all menus in one file
			oldStyleMenuLoad(oldMenusFile);
			oldMenusFile.renameTo(new File(oldMenusFile.getParent(), oldMenusFile.getName() + ".OLD"));
			saveMenusAndViews();
			MiscUtil.log(Level.INFO, "Converted old-style menu data file to new v0.5+ format");
		} else {
			for (File f : SMSConfig.getMenusFolder().listFiles(ymlFilter)) {
				try {
					YamlConfiguration conf = YamlConfiguration.loadConfiguration(f);
					SMSMenu menu = new SMSMenu(conf);
					SMSMenu.addMenu(menu.getName(), menu, true);
				} catch (SMSException e) {
					MiscUtil.log(Level.SEVERE, "Can't load menu data from " + f + ": " + e.getMessage());
				}
			}
			MiscUtil.log(Level.INFO, "Loaded " + SMSMenu.listMenus().size() + " menus from file.");
		}
	}

	private static void oldStyleMenuLoad(File menusFile) {
		try {
			YamlConfiguration conf = YamlConfiguration.loadConfiguration(menusFile);
			for (String menuName : conf.getKeys(false)) {
				ConfigurationSection cn = conf.getConfigurationSection(menuName);
				cn.set("name", menuName);
				SMSMenu menu = new SMSMenu(cn);
				SMSMenu.addMenu(menu.getName(), menu, true);
			}
		} catch (SMSException e) {
			MiscUtil.log(Level.SEVERE, "Can't restore menus: " + e.getMessage());
		}
		MiscUtil.log(Level.INFO, "read " + SMSMenu.listMenus().size() + " menus from file.");
	}	

	private static void oldStyleMacroLoad(File macrosFile) {
		YamlConfiguration conf = YamlConfiguration.loadConfiguration(macrosFile);
		for (String key : conf.getKeys(false)) {
			SMSMacro m = new SMSMacro(key);
			List<String> cmds = conf.getStringList(key);
			for (String cmd : cmds) {
				m.addLine(cmd);
			}
			SMSMacro.addMacro(m);
		}	
	}

	static void backupFile(File original) {
		try {
			File backup = getBackupFileName(original.getParentFile(), original.getName());
			copy(original, backup);
			MiscUtil.log(Level.INFO, "An error occurred while loading " + original +
			             ", so a backup has been created at " + backup.getPath());
		} catch (IOException e) {
			MiscUtil.log(Level.SEVERE, "Error while trying to write backup file: " + e);
		}
	}

	static void copy(File src, File dst) throws IOException {
		InputStream in = new FileInputStream(src);
		OutputStream out = new FileOutputStream(dst);

		// Transfer bytes from in to out
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		in.close();
		out.close();
	}

	static File getBackupFileName(File parentFile, String template) {
		String ext = ".BACKUP.";
		File backup;
		int idx = 0;

		do {
			backup = new File(parentFile, template + ext + idx);
			idx++;
		} while (backup.exists());
		return backup;
	}


	/**
	 * Require the presence of the given field in the given configuration.
	 * 
	 * @param node		The node to check 
	 * @param field	The field to check for
	 * @throws SMSException	if the field is not present in the configuration
	 */
	public static void mustHaveField(ConfigurationSection node, String field) throws SMSException {
		if (!node.contains(field))
			throw new SMSException("Field '" + field + "' missing - corrupted save file?");
	}
}	
