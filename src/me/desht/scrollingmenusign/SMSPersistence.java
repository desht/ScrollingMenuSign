package me.desht.scrollingmenusign;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.util.config.Configuration;
import org.bukkit.util.config.ConfigurationNode;
import org.yaml.snakeyaml.reader.ReaderException;

public class SMSPersistence {

	private static String menuFile = "scrollingmenus.yml";
	private ScrollingMenuSign plugin;

	public SMSPersistence(ScrollingMenuSign plugin) {
		this.plugin = plugin;
	}
	
	public void save() {
		File menusFile = new File(plugin.getDataFolder(), menuFile);
		Configuration conf = new Configuration(menusFile);
		
		for (SMSMenu menu : SMSMenu.listMenus()) {
			Map<String, Object> map = menu.freeze();
			conf.setProperty(menu.getName(), map);
		}
		conf.save();
		SMSUtils.log(Level.INFO, "saved " + SMSMenu.listMenus().size() + " menus to file.");
	}
	
	public void load() {
		File menusFile = new File(plugin.getDataFolder(), menuFile);
		
		for (SMSMenu menu : SMSMenu.listMenus()) {
			menu.deleteTemporary();
		}
		
		try {
			Configuration conf = new Configuration(menusFile);
			conf.load();
			for (String menuName : conf.getKeys()) {
				ConfigurationNode cn = conf.getNode(menuName);
				System.out.println("load " + menuName);
				SMSMenu menu = new SMSMenu(plugin, menuName, cn.getAll());
				SMSMenu.addMenu(menu.getName(), menu, true);
			}
		} catch (ReaderException e) {
			SMSUtils.log(Level.WARNING, "caught exception while loading menu data: " + e.getMessage());
			backupMenuFile(menusFile);
		}
		SMSUtils.log(Level.INFO, "read " + SMSMenu.listMenus().size() + " menus from file.");
	}	

	void backupMenuFile(File original) {
        try {
        	File backup = getBackupFileName(original.getParentFile(), menuFile);

            SMSUtils.log(Level.INFO, "An error occurred while loading the menus file, so a backup copy of "
                + original + " is being created. The backup can be found at " + backup.getPath());
            copy(original, backup);
        } catch (IOException e) {
            SMSUtils.log(Level.SEVERE, "Error while trying to write backup file: " + e);
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
