package me.desht.scrollingmenusign;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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

		HashMap<String, SMSMenu> menus = plugin.getMenus();
		Iterator<String> iter = menus.keySet().iterator();
		
		plugin.log(Level.INFO, "Saving " + menus.size() + " menus to file...");
		while (iter.hasNext()) {
			String k = iter.next();
			SMSMenu menu = menus.get(k);
			HashMap<String, Object> map = new HashMap<String, Object>();
			map.put("title", menu.getTitle());
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
			plugin.log(Level.SEVERE, e.getMessage());
		}
	}
	
	@SuppressWarnings("unchecked")
	public void load() {
		File f = new File(plugin.getDataFolder(), menuFile);
		if (!f.exists()) { // create empty file if doesn't already exist
            try {
                f.createNewFile();
            } catch (IOException e) {
                plugin.log(Level.SEVERE, e.getMessage());
            }
        }

        Yaml yaml = new Yaml();

        try {        	
        	HashMap<String,Map<String,Object>> menuMap = 
        		(HashMap<String,Map<String,Object>>) yaml.load(new FileInputStream(f));
        	if (menuMap != null) {
        		Iterator<String> iter = menuMap.keySet().iterator();
        		while (iter.hasNext()) {
        			String k = iter.next();
        			Map<String,Object> entry = menuMap.get(k);
        			createMenuSign(k, entry);
        		}
        		plugin.log(Level.INFO, "read " + menuMap.size() + " menus from file.");
        	}        	
        } catch (FileNotFoundException e) {
            plugin.log(Level.SEVERE, "menu file '" + f + "' was not found.");
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
			h.put("label", item.getLabel());
			h.put("command", item.getCommand());
			h.put("message", item.getMessage());
			l.add(h);
		}
		return l;
	}


	@SuppressWarnings("unchecked")
	private void createMenuSign(String menuName, Map<String, Object> menuData) {
		String title = plugin.parseColourSpec(null, (String) menuData.get("title"));
		String owner = (String) menuData.get("owner");
		SMSMenu menu;
		if (menuData.get("locations") != null) {
			// v0.3 or newer format - multiple locations per menu
			List<List<Object>> l0 = (List<List<Object>>) menuData.get("locations");
			List<Location> locs = new ArrayList<Location>();
			for (List<Object> l: l0) {
				World w = plugin.getServer().getWorld((String) l.get(0));
				locs.add(new Location(w, (Integer)l.get(1), (Integer)l.get(2), (Integer)l.get(3)));
			}
			menu = new SMSMenu(menuName, title, owner, null);
			if (locs.size() > 0) {
				for (int i = 0; i < locs.size(); i++) {
					menu.addSign(locs.get(i));
				}
			}
		} else {
			// v0.2 or older
			String worldName = (String) menuData.get("world");
			World w = findWorld(worldName);
			List<Integer>l = (List<Integer>) menuData.get("location");
			menu = new SMSMenu(menuName, title, owner,
					new Location(w, l.get(0), l.get(1), l.get(2)));

		}
		plugin.addMenu(menuName, menu, false);
		
		List<Map<String,String>>items = (List<Map<String, String>>) menuData.get("items");
		for (Map<String,String> item : items) {
			menu.add(item.get("label"), item.get("command"), item.get("message"));
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
}	
