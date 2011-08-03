package me.desht.scrollingmenusign;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.World;
import org.yaml.snakeyaml.Yaml;

public class SMSPersistence {

	private static String menuFile = "scrollingmenus.yml";
	private ScrollingMenuSign plugin;

	public SMSPersistence(ScrollingMenuSign plugin) {
		this.plugin = plugin;
	}
	
	public void save() {
		Yaml yaml = new Yaml();
	
		Map<String,Map<String,Object>> menuMap = new HashMap<String,Map<String,Object>>();
		
		File f = new File(plugin.getDataFolder(), menuFile);

		Map<String, SMSMenu> menus = SMSMenu.getMenus();
		Iterator<String> iter = menus.keySet().iterator();
		
		SMSUtils.log(Level.INFO, "Saving " + menus.size() + " menus to file...");
		while (iter.hasNext()) {
			String k = iter.next();
			SMSMenu menu = menus.get(k);
			HashMap<String, Object> map = new HashMap<String, Object>();
			map.put("title", SMSUtils.unParseColourSpec(menu.getTitle()));
			map.put("owner", menu.getOwner());
			List<List<Object>> locs = new ArrayList<List<Object>>();
			for (Location l: menu.getLocations().keySet()) {
				locs.add(makeBlockList(l));
			}
			map.put("locations", locs);
			map.put("items", makeItemList(menu.getItems()));
			menuMap.put(menu.getName(), map);
		}

		try {
			yaml.dump(menuMap, new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8")));
		} catch (IOException e) {
			SMSUtils.log(Level.SEVERE, e.getMessage());
		}
	}
	
	@SuppressWarnings("unchecked")
	public void load() {
		File f = new File(plugin.getDataFolder(), menuFile);
		if (!f.exists()) { // create empty file if doesn't already exist
            try {
                f.createNewFile();
            } catch (IOException e) {
                SMSUtils.log(Level.SEVERE, e.getMessage());
            }
        }

        Yaml yaml = new Yaml();

        try {        	
        	HashMap<String,Map<String,Object>> menuMap = 
        		(HashMap<String,Map<String,Object>>) yaml.load(new InputStreamReader(new FileInputStream(f), "UTF-8"));
        	if (menuMap != null) {
        		Iterator<String> iter = menuMap.keySet().iterator();
        		while (iter.hasNext()) {
        			String k = iter.next();
        			Map<String,Object> entry = menuMap.get(k);
        			createMenuSign(k, entry);
        		}
        		SMSUtils.log(Level.INFO, "read " + menuMap.size() + " menus from file.");
                // now delete any menus which are in-game but not in the loaded map file
        		List<String> staleMenus = new ArrayList<String>();
        		for (String menuName : SMSMenu.getMenus().keySet()) {
        			if (!menuMap.containsKey(menuName)) {
        				staleMenus.add(menuName);
        			}
        		}
        		for (String stale : staleMenus) {
    				SMSUtils.log(Level.INFO, "deleted stale menu '" + stale + "'.");
        			SMSMenu.removeMenu(stale, ScrollingMenuSign.MenuRemoveAction.BLANK_SIGN);
        		}
        	}        	
        } catch (FileNotFoundException e) {
            SMSUtils.log(Level.SEVERE, "menu file '" + f + "' was not found.");
        } catch (Exception e) {
        	SMSUtils.log(Level.SEVERE, "caught exception loading " + f + ": " + e.getMessage());
        	backupMenuFile(f);
        }
         
	}

	private List<Object> makeBlockList(Location l) {
        List<Object> list = new ArrayList<Object>();
        list.add(l.getWorld().getName());
        list.add(l.getBlockX());
        list.add(l.getBlockY());
        list.add(l.getBlockZ());

        return list;
    }

	
	private List<Map<String, String>> makeItemList(List<SMSMenuItem> items) {
		List<Map<String,String>> l = new ArrayList<Map<String, String>>();
		for (SMSMenuItem item : items) {		
			HashMap<String,String> h = new HashMap<String, String>();
			h.put("label", SMSUtils.unParseColourSpec(item.getLabel()));
			h.put("command", item.getCommand());
			h.put("message", SMSUtils.unParseColourSpec(item.getMessage()));
			l.add(h);
		}
		return l;
	}


	@SuppressWarnings("unchecked")
	private void createMenuSign(String menuName, Map<String, Object> menuData) {
		String title = SMSUtils.parseColourSpec(null, (String) menuData.get("title"));
		String owner = (String) menuData.get("owner");
		SMSMenu menu;
		if (menuData.get("locations") != null) {
			// v0.3 or newer format - multiple locations per menu
			List<List<Object>> l0 = (List<List<Object>>) menuData.get("locations");
			menu = new SMSMenu(plugin, menuName, title, owner, null);
			for (List<Object> l: l0) {
				World w = findWorld((String) l.get(0));
				Location loc = new Location(w, (Integer)l.get(1), (Integer)l.get(2), (Integer)l.get(3));
				menu.addSign(loc);
			}
		} else {
			// v0.2 or older
			String worldName = (String) menuData.get("world");
			World w = findWorld(worldName);
			List<Integer>l = (List<Integer>) menuData.get("location");
			menu = new SMSMenu(plugin, menuName, title, owner,
					new Location(w, l.get(0), l.get(1), l.get(2)));

		}
		SMSMenu.addMenu(menuName, menu, false);
		
		List<Map<String,String>>items = (List<Map<String, String>>) menuData.get("items");
		for (Map<String,String> item : items) {
			menu.addItem(
					SMSUtils.parseColourSpec(null, item.get("label")),
					item.get("command"),
					SMSUtils.parseColourSpec(null, item.get("message"))
			);
		}
		
		menu.updateSigns();
	}

	private World findWorld(String worldName) {
        World w = plugin.getServer().getWorld(worldName);

        if (w != null) {
        	return w;
        } else {
        	throw new IllegalArgumentException("World " + worldName + " was not found on the server.");
        }
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
