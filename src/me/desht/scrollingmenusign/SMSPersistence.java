package me.desht.scrollingmenusign;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.scrollingmenusign.views.SMSView;
import me.desht.util.MiscUtil;

import org.bukkit.util.config.Configuration;
import org.bukkit.util.config.ConfigurationNode;
import org.yaml.snakeyaml.reader.ReaderException;

public class SMSPersistence {

	private static final FilenameFilter ymlFilter = new FilenameFilter() {

		@Override
		public boolean accept(File dir, String name) {
			return name.endsWith(".yml");
		}
	};

	static void init() {
	}

	public static void unPersist(Freezable object) {
		File saveFile = new File(object.getSaveFolder(), object.getName() + ".yml");
		if (!saveFile.delete()) {
			MiscUtil.log(Level.WARNING, "can't delete " + saveFile);
		}
	}

	static void saveAll() {
		for (SMSMenu menu : SMSMenu.listMenus()) {
			save(menu);
		}
		for (SMSView view : SMSView.listViews()) {
			save(view);
		}
		MiscUtil.log(Level.INFO, "saved " + SMSMenu.listMenus().size() + " menus " +
		             " and " + SMSView.listViews().size() + " views to file.");
	}

	public static void save(Freezable object) {
		File saveFile = new File(object.getSaveFolder(), object.getName() + ".yml");
		Configuration conf = new Configuration(saveFile);
		Map<String, Object> map = object.freeze();
		for (Entry<String, Object> e : map.entrySet()) {
			conf.setProperty(e.getKey(), e.getValue());
		}
		conf.save();
	}

	static void loadAll() {
		loadMenus();
		loadViews();
	}

	private static void loadViews() {
		for (SMSView view : SMSView.listViews()) {
			view.deleteTemporary();
		}

		for (File f : SMSConfig.getViewsFolder().listFiles(ymlFilter)) {
			try {
				Configuration conf = new Configuration(f);
				conf.load();
				SMSView view = SMSView.load(conf);
				if (view != null) {
					view.getMenu().addObserver(view);
					view.update(view.getMenu(), SMSMenuAction.REPAINT);
				}
			} catch (ReaderException e)	{
				MiscUtil.log(Level.WARNING, "caught exception while loading view file " +
				             f + ": " + e.getMessage());
				backupMenuFile(f);
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
			oldStyleLoad(oldMenusFile);
			oldMenusFile.renameTo(new File(oldMenusFile.getParent(), oldMenusFile.getName() + ".OLD"));
			saveAll();
			MiscUtil.log(Level.INFO, "Converted old-style menu data file to new v0.5+ format");
		} else {
			for (File f : SMSConfig.getMenusFolder().listFiles(ymlFilter)) {
				try {
					Configuration conf = new Configuration(f);
					conf.load();
					SMSMenu menu = new SMSMenu(conf);
					SMSMenu.addMenu(menu.getName(), menu, true);

				} catch (ReaderException e)	{
					MiscUtil.log(Level.WARNING, "caught exception while loading menu file " +
					             f + ": " + e.getMessage());
					backupMenuFile(f);
				} catch (SMSException e) {
					MiscUtil.log(Level.WARNING, "caught exception while restoring menu " + f + ": " + e.getMessage());
				}
			}
			MiscUtil.log(Level.INFO, "Loaded " + SMSMenu.listMenus().size() + " menus from file.");
		}
	}

	private static void oldStyleLoad(File menusFile) {	
		try {
			Configuration conf = new Configuration(menusFile);
			conf.load();
			for (String menuName : conf.getKeys()) {
				ConfigurationNode cn = conf.getNode(menuName);
				cn.setProperty("name", menuName);
				SMSMenu menu = new SMSMenu(cn);
				SMSMenu.addMenu(menu.getName(), menu, true);
			}
		} catch (ReaderException e) {
			MiscUtil.log(Level.WARNING, "caught exception while loading menu data: " + e.getMessage());
			backupMenuFile(menusFile);
		} catch (SMSException e) {
			MiscUtil.log(Level.WARNING, "caught exception while restoring menus: " + e.getMessage());
		}
		MiscUtil.log(Level.INFO, "read " + SMSMenu.listMenus().size() + " menus from file.");
	}	

	static void backupMenuFile(File original) {
		try {
			File backup = getBackupFileName(original.getParentFile(), original.getName());

			MiscUtil.log(Level.INFO, "An error occurred while loading the menus file, so a backup copy of "
			             + original + " is being created. The backup can be found at " + backup.getPath());
			copy(original, backup);
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

}	
