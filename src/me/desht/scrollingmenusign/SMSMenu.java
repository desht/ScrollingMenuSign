package me.desht.scrollingmenusign;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;

import me.desht.scrollingmenusign.ScrollingMenuSign.MenuRemoveAction;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

/**
 * @author des
 *
 */
public class SMSMenu {
	private ScrollingMenuSign plugin;
	private String name;
	private String title;
	private String owner;
	private ArrayList<SMSMenuItem> items;
	Map<Location, Integer> locations;	// maps sign location to scroll pos
	
	private static Map<Location, String> menuLocations = new HashMap<Location, String>();
	private static Map<String, SMSMenu> menus = new HashMap<String, SMSMenu>();
	
	private static SMSMenuItem blankItem = new SMSMenuItem("", "", "");

	// Construct a new menu
	public SMSMenu(ScrollingMenuSign plugin, String n, String t, String o, Location l) {
		this.plugin = plugin;
		items = new ArrayList<SMSMenuItem>();
		name = n;
		title = t;
		owner = o;
		locations = new HashMap<Location, Integer>();
		if (l != null) locations.put(l, 0);
	}

	/**
	 * Construct a new menu which is a copy of an existing menu
	 *
	 * @param plugin
	 * @param other
	 * @param n
	 * @param o
	 * @param l
	 */
	public SMSMenu(ScrollingMenuSign plugin, SMSMenu other, String n, String o, Location l) {
		this.plugin = plugin;
		items = new ArrayList<SMSMenuItem>();
		name = n;
		title = other.getTitle();
		owner = o;
		locations = new HashMap<Location, Integer>();
		if (l != null) locations.put(l, 0);
		for (SMSMenuItem item: other.getItems()) {
			addItem(item.getLabel(), item.getCommand(), item.getMessage());
		}
	}

	/**
	 * Construct a new menu from data read from the save file
	 * 
	 * @param menuData A map of properties for the menu
	 */
	@SuppressWarnings("unchecked")
	SMSMenu(ScrollingMenuSign plugin, String name, Map<String, Object> menuData) {
		this.plugin = plugin;
		this.name = name;
		items = new ArrayList<SMSMenuItem>();
		setTitle(SMSUtils.parseColourSpec(null, ((String) menuData.get("title"))));
		setOwner((String) menuData.get("owner"));
		locations = new HashMap<Location, Integer>();
		
		if (menuData.get("locations") != null) {
			// v0.3 or newer format - multiple locations per menu
			List<List<Object>> l0 = (List<List<Object>>) menuData.get("locations");			
			for (List<Object> locList: l0) {
				World w = findWorld((String) locList.get(0));
				Location loc = new Location(w, (Integer)locList.get(1), (Integer)locList.get(2), (Integer)locList.get(3));
				addSign(loc);
			}
		} else {
			// v0.2 or older
			String worldName = (String) menuData.get("world");
			World w = findWorld(worldName);
			List<Integer>locList = (List<Integer>) menuData.get("location");
			addSign(new Location(w, locList.get(0), locList.get(1), locList.get(2)));
		}
		List<Map<String,String>>items = (List<Map<String, String>>) menuData.get("items");
		for (Map<String,String> item : items) {
			addItem(SMSUtils.parseColourSpec(null, item.get("label")),
					item.get("command"),
					SMSUtils.parseColourSpec(null, item.get("message"))
			);
		}
	}

	Map<String, Object> freeze() {
		HashMap<String, Object> map = new HashMap<String, Object>();
		
		map.put("title", SMSUtils.unParseColourSpec(getTitle()));
		map.put("owner", getOwner());
		List<List<Object>> locs = new ArrayList<List<Object>>();
		for (Location l: getLocations().keySet()) {
			locs.add(makeBlockList(l));
		}
		map.put("locations", locs);
		map.put("items", makeItemList(getItems()));
		
		return map;
	}
	
	private World findWorld(String worldName) {
        World w = Bukkit.getServer().getWorld(worldName);

        if (w != null) {
        	return w;
        } else {
        	throw new IllegalArgumentException("World " + worldName + " was not found on the server.");
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
			Map<String,String> h = new HashMap<String, String>();
			h.put("label", SMSUtils.unParseColourSpec(item.getLabel()));
			h.put("command", item.getCommand());
			h.put("message", SMSUtils.unParseColourSpec(item.getMessage()));
			l.add(h);
		}
		return l;
	}

	public String getName() {
		return name;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String newTitle) {
		title = newTitle;
	}

	public Map<Location,Integer> getLocations() {
		return locations;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	/**
	 * Get a list of all the items in the menu
	 * 
	 * @return A list of the items
	 */
	public List<SMSMenuItem> getItems() {
		return items;
	}

	/**
	 * Get the number of items in the menu
	 * 
	 * @return	The number of items
	 */
	public int getNumItems() {
		return items.size();
	}

	/**
	 * Get the currently-selected menu item for the given sign location
	 * 
	 * @param l	Location of the item to check
	 * @return	The menu item that is currently selected
	 */
	public SMSMenuItem getCurrentItem(Location l) {
		if (items.size() == 0) {
			return null;
		}
		return items.get(locations.get(l));
	}
	
	/**
	 * Add a new sign to the menu.  Equivalent to <b>addSign(l, false)</b>
	 * 
	 * @param l	Location of the sign to add
	 */
	public void addSign(Location l) {
		addSign(l, false);
	}
	
	/**
	 * Add a new sign to the menu, possibly updating its text.
	 * 
	 * @param l Location of the sign to add
	 * @param updateSignText true to immediately repaint the sign, false to leave it as is
	 */
	public void addSign(Location l, boolean updateSignText) {
		Block b = l.getBlock();
		if (b.getType() == Material.SIGN_POST || b.getType() == Material.WALL_SIGN) {
			locations.put(l, 0);
		}
		menuLocations.put(l, getName());
		if (updateSignText) {
			updateSign(l);
		}
	}
	
	/**
	 * Remove a sign from the menu.  Don't do anything with the sign's text.
	 * 
	 * @param l Location of the sign to remove
	 */
	public void removeSign(Location l) {
		removeSign(l, MenuRemoveAction.DO_NOTHING);
	}
	
	/**
	 * Remove a sign from the menu.
	 * 
	 * @param l	Location of the sign to remove
	 * @param action	Action to take on the sign.
	 */
	public void removeSign(Location l, MenuRemoveAction action) {
		switch(action) {
		case BLANK_SIGN:
			blankSign(l);
			break;
		case DESTROY_SIGN:
			destroySign(l);
			break;
		}
		locations.remove(l);
		menuLocations.remove(l);
	}

	/**
	 * Add a new item to the menu
	 * 
	 * @param label	Label of the item to add
	 * @param command Command to be run when the item is selected
	 * @param message Feedback text to be shown when the item is selected
	 */
	public void addItem(String label, String command, String message) {
		SMSMenuItem item = new SMSMenuItem(label, command, message);
		items.add(item);
	}
	
	/**
	 * Remove an item from the menu by matching label.  If the label string is
	 * just an integer value, remove the item at that 1-based numeric index.
	 * 
	 * @param indexStr	The label to search for and remove
	 * @throws IllegalArgumentException if the label does not exist in the menu
	 */
	public void removeItem(String indexStr) {
		int index = -1;
		try {
			index = Integer.parseInt(indexStr);
		} catch (NumberFormatException e) {
			// not an integer - try to remove by label
			for (int i = 0; i < items.size(); i++) {
				String label = SMSUtils.deColourise(items.get(i).getLabel());
				if (indexStr.equalsIgnoreCase(label)) {
					index = i + 1;
					break;
				}
			}
			if (index == -1)
				throw new IllegalArgumentException("No such label '" + indexStr + "'.");
		}
		removeItem(index);
	}
	
	/**
	 * Remove an item from the menu by numeric index
	 * 
	 * @param index 1-based index of the item to remove
	 */
	public void removeItem(int index) {
		// Java lists are 0-indexed, our signs are 1-indexed
		items.remove(index - 1);
		for (Location l : locations.keySet()) {
			if (locations.get(l) >= items.size()) {
				locations.put(l, items.size() == 0 ? 0 : items.size() - 1);
			}
		}
	}

	/**
	 * Force a repaint of all of the menu's signs
	 */
	public void updateSigns() {
		for (Location l : locations.keySet()) {
			String[] lines = buildSignText(l);
			updateSign(l, lines);
		}
	}		
	
	/**
	 * Remove all items from a menu 
	 */
	public void removeAllItems() {
		items.clear();
		for (Location l : locations.keySet()) {
			locations.put(l, 0);
		}
	}
	
	/**
	 * Force a repaint of the given sign according to the current menu state
	 * 
	 * @param l	Location of the sign to repaint
	 */
	public void updateSign(Location l) {
		updateSign(l, null);
	}
	
	private void updateSign(Location l, String[] lines) {
		Sign sign = getSign(l);
		if (sign == null) return;
		if (lines == null) lines = buildSignText(l);
		for (int i = 0; i < 4; i++) {
			sign.setLine(i, lines[i]);
		}
		sign.update();
	}
	
	/**
	 * Set the currently selected item for this sign to the next item.
	 * @param l	Location of the sign
	 */
	public void nextItem(Location l) {
		if (!locations.containsKey(l))
			return;
		int pos = locations.get(l) + 1;
		if (pos >= items.size())
			pos = 0;
		locations.put(l, pos);
	}

	/**
	 * Set the currently selected item for this sign to the previous item.
	 * @param l	Location of the sign
	 */
	public void prevItem(Location l) {
		if (items.size() == 0 || !locations.containsKey(l))
			return;
		int pos = locations.get(l) - 1;
		if (pos < 0) pos = items.size() - 1;
		locations.put(l, pos);
	}

	/**
	 * Get the Bukkit Sign object at the given location.  The sign must belong to this menu.
	 * 
	 * @param l A location
	 * @return	The sign
	 */
	public Sign getSign(Location l) {
		if (locations.get(l) == null) {
			return null;
		}
		Block b = l.getBlock();
		if (b.getType() != Material.SIGN_POST && b.getType() != Material.WALL_SIGN) {
			return null;
		}
		return (Sign) b.getState();
	}

	/**
	 * Permanently delete a menu, blanking all its signs
	 */
	public void delete() {
		deletePermanent();
	}

	/**
	 * Permanently delete a menu
	 * @param action	Action to take on the menu's signs
	 */
	public void delete(MenuRemoveAction action) {
		deletePermanent(action);
	}

	// blank the signs
	private void blankSigns() {
		for (Location l : locations.keySet()) {
			blankSign(l);
		}
	}
	// blank the sign at location l
	private void blankSign(Location l) {
		Sign sign = getSign(l);
		if (sign == null) return;
		for (int i = 0; i < 4; i++) {
			sign.setLine(i, "");
		}
		sign.update();
	}
	
	// build the text for the sign based on current menu contents
	private String[] buildSignText(Location l) {
		String[] res = new String[4];
		
		// first line of the sign in the menu title
		res[0] = title;
		
		// line 2-4 are the menu items around the current menu position
		// line 3 is the current position
		String prefix1 = SMSConfig.getConfiguration().getString("sms.item_prefix.not_selected", "  ");
		String prefix2 = SMSConfig.getConfiguration().getString("sms.item_prefix.selected", "> ");
		
		res[1] = String.format(makePrefix(prefix1), getLine2Item(l).getLabel());
		res[2] = String.format(makePrefix(prefix2), getLine3Item(l).getLabel());
		res[3] = String.format(makePrefix(prefix1), getLine4Item(l).getLabel());
		
		return res;
	}

	private String makePrefix(String prefix) {
		String just = SMSConfig.getConfiguration().getString("sms.item_justify", "left");
		int l = 15 - prefix.length();
		String s = "";
		if (just.equals("left"))
			s =  prefix + "%1$-" + l + "s";
		else if (just.equals("right"))
			s = prefix + "%1$" + l + "s";
		else
			s = prefix + "%1$s";
		return SMSUtils.parseColourSpec(null, s);
	}
	
	// Get line 2 of the sign (item before the current item, or blank
	// if the menu has less than 3 items)
	private SMSMenuItem getLine2Item(Location l) {
		if (items.size() < 3) {
			return blankItem;	
		}
		int prev_pos = locations.get(l) - 1;
		if (prev_pos < 0) {
			prev_pos = items.size() - 1;
		}
		return items.get(prev_pos);
	}

	// Get line 3 of the sign (this is the currently selected item)
	private SMSMenuItem getLine3Item(Location l) {
		if (items.size() < 1) {
			return blankItem;
		}
		return items.get(locations.get(l));
	}
	
	// Get line 4 of the sign (item after the current item, or blank
	// if the menu has less than 2 items)
	private SMSMenuItem getLine4Item(Location l) {
		if (items.size() < 2) {
			return blankItem;
		}
		int next_pos = locations.get(l) + 1;
		if (next_pos >= items.size()) {
			next_pos = 0;
		}
		return items.get(next_pos);
	}
	
	private void destroySigns() {
		for (Location l: getLocations().keySet()) {
			destroySign(l);
		}
	}
	
	private void destroySign(Location l) {
		l.getBlock().setTypeId(0);
	}
	
	void deletePermanent() {
		deletePermanent(MenuRemoveAction.BLANK_SIGN);
	}
	
	void deletePermanent(MenuRemoveAction action) {
		try {
			SMSMenu.removeMenu(getName(), MenuRemoveAction.BLANK_SIGN);
		} catch (SMSNoSuchMenuException e) {
			// Should not get here
			SMSUtils.log(Level.WARNING, "Impossible: deletePermanent got SMSNoSuchMenuException?");
		}
	}

	void deleteTemporary() {
		try {
			SMSMenu.removeMenu(getName(), MenuRemoveAction.DO_NOTHING);
		} catch (SMSNoSuchMenuException e) {
			// Should not get here
			SMSUtils.log(Level.WARNING, "Impossible: deleteTemporary got SMSNoSuchMenuException?");
		}
	}

	void autosave() {
		if (SMSConfig.getConfiguration().getBoolean("sms.autosave", true))
			plugin.getPersistence().save();
	}

	/**************************************************************************/
	
	/**
	 * Add a menu to the menu list, preserving a reference to it.
	 * 
	 * @param menuName	The menu's name
	 * @param menu		The menu object
	 * @param updateSign	Whether or not to update the menu's signs now
	 */
	static void addMenu(String menuName, SMSMenu menu, Boolean updateSign) {
		menus.put(menuName, menu);
		for (Location l: menu.getLocations().keySet()) {
			menuLocations.put(l, menuName);
		}
		if (updateSign) {
			menu.updateSigns();
		}
		System.out.println("added menu " + menuName);
	}
	
	/**
	 * Remove a menu from the list, destroying the reference to it.
	 * 
	 * @param menuName	The menu's name
	 * @param action	Action to take on removal
	 * @throws SMSNoSuchMenuException
	 */
	static void removeMenu(String menuName, MenuRemoveAction action) throws SMSNoSuchMenuException {
		SMSMenu menu = getMenu(menuName);
		switch(action) {
		case DESTROY_SIGN:
			menu.destroySigns();
			break;
		case BLANK_SIGN:
			menu.blankSigns();
			break;
		}
		for (Location loc: menu.getLocations().keySet()) {
			menuLocations.remove(loc);
		}
		menus.remove(menuName);
	}
	
	/**
	 * Retrieve the menu with the given name
	 * @param menuName	The name of the menu to retrieve
	 * @return	The menu object
	 * @throws SMSNoSuchMenuException if the menu name is not found
	 */
	static SMSMenu getMenu(String menuName) throws SMSNoSuchMenuException {
		if (!menus.containsKey(menuName))
			throw new SMSNoSuchMenuException("No such menu '" + menuName + "'.");
		return menus.get(menuName);
	}
	
	/**
	 * Cause the signs on all menus to be redrawn
	 */
	static void updateAllMenus(){
		for (SMSMenu menu : listMenus()) {
			menu.updateSigns();
		}
	}
	
	/**
	 * Get the name of the menu at the given location.
	 * 
	 * @param loc	The location
	 * @return	The menu name, or null if there is no menu sign at the location
	 */
	static String getMenuNameAt(Location loc) {
		return menuLocations.get(loc);
	}
	
	/**
	 * Get the menu at the given location
	 * 
	 * @param loc	The location
	 * @return	The menu object
	 * @throws SMSNoSuchMenuException if there is no menu sign at the location
	 */
	static SMSMenu getMenuAt(Location loc) throws SMSNoSuchMenuException {
		return getMenu(getMenuNameAt(loc));
	}

	/**
	 * Check to see if a menu with the given name exists
	 * 
	 * @param menuName The menu name
	 * @return true if the menu exists, false if it does not
	 */
	static Boolean checkForMenu(String menuName) {
		return menus.containsKey(menuName);
	}
	
	/**
	 * Return the name of the menu sign that the player is looking at, if any
	 * 
	 * @param player	The Bukkit player object
	 * @param complain	Whether or not to throw an exception if there is no menu
	 * @return	The menu name, or null if there is no menu and <b>complain</b> is false
	 * @throws SMSException	if there is not menu and <b>complain</b> is true
	 */
	static String getTargetedMenuSign(Player player, Boolean complain) throws SMSException {
		Block b = player.getTargetBlock(null, 3);
		if (b.getType() != Material.SIGN_POST && b.getType() != Material.WALL_SIGN) {
			if (complain) SMSUtils.errorMessage(player, "You are not looking at a sign.");
			return null;
		}
		String name = SMSMenu.getMenuNameAt(b.getLocation());
		if (name == null && complain)
			throw new SMSException("There is no menu associated with that sign.");
		return name;
	}

	/**
	 * Return an unsorted list of all the known menus
	 * Equivalent to calling <b>listMenus(false)</b>
	 * @return A list of SMSMenu objects
	 */
	static List<SMSMenu> listMenus() {
		return listMenus(false);
	}
	
	/**
	 * Return a list of all the known menus
	 * 
	 * @param isSorted Whether or not to sort the menus by name
	 * @return	A list of SMSMenu objects
	 */
	static List<SMSMenu> listMenus(boolean isSorted) {
		if (isSorted) {
			SortedSet<String> sorted = new TreeSet<String>(menus.keySet());
			List<SMSMenu> res = new ArrayList<SMSMenu>();
			for (String name : sorted) {
				res.add(menus.get(name));
			}
			return res;
		} else {
			return new ArrayList<SMSMenu>(menus.values());
		}
	}

}
