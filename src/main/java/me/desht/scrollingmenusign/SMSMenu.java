package me.desht.scrollingmenusign;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.SortedSet;
import java.util.TreeSet;

import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.scrollingmenusign.views.SMSView;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;

/**
 * @author des
 *
 */
public class SMSMenu extends Observable implements SMSPersistable, SMSUseLimitable {
	private String name;
	private String title;
	private String owner;
	private List<SMSMenuItem> items;
	private Map<String,Integer> itemMap;
	private boolean autosave;
	private boolean autosort;
	private SMSRemainingUses uses;
	private String defaultCommand;

	private static final Map<String, SMSMenu> menus = new HashMap<String, SMSMenu>();

	/**
	 * Construct a new menu
	 *
	 * @param plugin	Reference to the ScrollingMenuSign plugin
	 * @param n			Name of the menu
	 * @param t			Title of the menu
	 * @param o			Owner of the menu
	 * @param l			Location of the menu's first sign (may be null)
	 * @throws SMSException If there is already a menu at this location
	 */
	SMSMenu(String n, String t, String o) throws SMSException {
		initCommon(n, t, o);
		uses = new SMSRemainingUses(this);
	}

	/**
	 * Construct a new menu which is a copy of an existing menu
	 *
	 * @param plugin	Reference to the ScrollingMenuSign plugin
	 * @param other		The existing menu to be copied
	 * @param n			Name of the menu
	 * @param o			Owner of the menu
	 * @param l			Location of the menu's first sign (may be null)
	 * @throws SMSException  If there is already a menu at this location
	 */
	SMSMenu(SMSMenu other, String n, String o) throws SMSException {
		initCommon(n, other.getTitle(), o);
		uses = new SMSRemainingUses(this);

		for (SMSMenuItem item: other.getItems()) {
			addItem(item.getLabel(), item.getCommand(), item.getMessage());
		}
	}

	/**
	 * Construct a new menu from data read from the save file
	 * 
	 * @param node 		A ConfigurationSection containing the menu's properties
	 * @throws SMSException If there is already a menu at this location
	 */
	@SuppressWarnings("unchecked")
	SMSMenu(ConfigurationSection node) throws SMSException {
		SMSPersistence.mustHaveField(node, "name");
		SMSPersistence.mustHaveField(node, "title");
		SMSPersistence.mustHaveField(node, "owner");
		
		initCommon(node.getString("name"),
		           MiscUtil.parseColourSpec(node.getString("title")),
		           node.getString("owner"));

		autosort = node.getBoolean("autosort", false);
		uses = new SMSRemainingUses(this, node.getConfigurationSection("usesRemaining"));
		defaultCommand = node.getString("defaultCommand", "");

		List<Map<String,Object>> items = (List<Map<String,Object>>) node.getList("items");
		for (Map<String,Object> item : items) {
			MemoryConfiguration itemNode = new MemoryConfiguration();
			// need to expand here because the item may contain a usesRemaining object - item could contain a nested map
			SMSPersistence.expandMapIntoConfig(itemNode, item);
			SMSMenuItem menuItem = new SMSMenuItem(this, itemNode);
			SMSMenuItem actual = menuItem.uniqueItem();
			if (!actual.getLabel().equals(menuItem.getLabel()))
				LogUtils.warning("Menu '" + getName() + "': duplicate item '" + menuItem.getLabelStripped() + "' renamed to '" + actual.getLabelStripped() + "'");
			addItem(actual);
		}
	}

	private void initCommon(String n, String t, String o) {
		items = new ArrayList<SMSMenuItem>();
		itemMap = new HashMap<String, Integer>();
		name = n;
		title = t;
		owner = o;
		autosave = ScrollingMenuSign.getInstance().getConfig().getBoolean("sms.autosave", true);
		autosort = false;
		uses = new SMSRemainingUses(this);
		defaultCommand = "";
	}

	public Map<String, Object> freeze() {
		HashMap<String, Object> map = new HashMap<String, Object>();

		List<Map<String,Object>> l = new ArrayList<Map<String, Object>>();
		for (SMSMenuItem item : items) {
			l.add(item.freeze());
		}

		map.put("name", getName());
		map.put("title", MiscUtil.unParseColourSpec(getTitle()));
		map.put("owner", getOwner());
		map.put("items", l);
		map.put("autosort", autosort);
		map.put("usesRemaining", uses.freeze());
		map.put("defaultCommand", defaultCommand);

		return map;
	}

	/**
	 * Get the menu's unique name
	 * 
	 * @return	Name of this menu
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get the menu's title string
	 * 
	 * @return	The title string
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Set the menu's title string
	 * 
	 * @param newTitle	The new title string
	 */
	public void setTitle(String newTitle) {
		title = newTitle;
		setChanged();

		autosave();
	}

	/**
	 * Get the menu's owner string
	 * 
	 * @return	Name of the menu's owner
	 */
	public String getOwner() {
		return owner;
	}

	/**
	 * Set the menu's owner string.
	 * 
	 * @param owner	Name of the menu's owner
	 */
	public void setOwner(String owner) {
		this.owner = owner;

		autosave();
	}

	/**
	 * Get the menu's autosave status - will menus be automatically saved to disk when modified?
	 *
	 * @return	true or false
	 */
	public boolean isAutosave() {
		return autosave;
	}

	/**
	 * Set the menu's autosave status - will menus be automatically saved to disk when modified?
	 * 
	 * @param autosave	true or false
	 * @return			the previous autosave status - true or false
	 */
	public boolean setAutosave(boolean autosave) {
		boolean prev = this.autosave;
		this.autosave = autosave;
		if (autosave)
			autosave();
		return prev;
	}

	/**
	 * Get the menu's autosort status - will menu items be automatically sorted when added?
	 * 
	 * @return	true or false
	 */
	public boolean isAutosort() {
		return autosort;
	}

	/**
	 * Set the menu's autosort status - will menu items be automatically sorted when added?
	 * 
	 * @param autosort	true or false
	 */
	public void setAutosort(boolean autosort) {
		this.autosort = autosort;

		autosave();
	}

	/**
	 * Get the menu's default command.  This command will be used if the menu item
	 * being executed has a missing command.
	 * 
	 * @return	The default command string
	 */
	public String getDefaultCommand() {
		return defaultCommand;
	}

	/**
	 * Set the menu's default command.  This command will be used if the menu item
	 * being executed has a missing command.
	 * 
	 * @param defaultCommand
	 */
	public void setDefaultCommand(String defaultCommand) {
		this.defaultCommand = defaultCommand;

		autosave();
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
	public int getItemCount() {
		return items.size();
	}
	
	/**
	 * Get the item at the given numeric index
	 * 
	 * @param 	index	1-based numeric index
	 * @return		The menu item at that index or null if out of range and mustExist is false
	 * @throws SMSException if the index is out of range and mustExist is true
	 */
	public SMSMenuItem getItemAt(int index, boolean mustExist) {
		if (index < 1 || index > items.size()) {
			if (mustExist) {
				throw new SMSException("Index " + index + " out of range.");
			} else {
				return null;
			}
		} else {
			return items.get(index - 1);
		}
	}

	public SMSMenuItem getItemAt(int index) {
		return getItemAt(index, false);
	}
	
	/**
	 * Get the menu item matching the given label
	 * 
	 * @param wanted	The label to match (case-insensitive)
	 * @return			The menu item with that label, or null if no matching item
	 */
	public SMSMenuItem getItem(String wanted) {
		return getItem(wanted, false);
	}

	/**
	 * Get the menu item matching the given label
	 * 
	 * @param wanted	The label to match (case-insensitive)
	 * @param mustExist	If true and the label is not in the menu, throw an exception
	 * @return			The menu item with that label, or null if no matching item and mustExist is false
	 * @throws SMSException if no matching item and mustExist is true
	 */
	public SMSMenuItem getItem(String wanted, boolean mustExist) {
		if (items.size() != itemMap.size()) rebuildItemMap();	// workaround for Heroes 1.4.8 which calls menu.getItems().clear
		
		Integer idx = itemMap.get(ChatColor.stripColor(wanted));
		if (idx == null) {
			if (mustExist) {
				throw new SMSException("No such item '" + wanted + "' in menu " + getName());
			} else {
				return null;
			}
		}
		return getItemAt(idx);
	}
	
	/**
	 * Get the index of the item matching the given label
	 * 
	 * @param wanted	The label to match (case-insensitive)
	 * @return			1-based item index, or -1 if no matching item
	 */
	public int indexOfItem(String wanted) {
		if (items.size() != itemMap.size()) rebuildItemMap();	// workaround for Heroes 1.4.8 which calls menu.getItems().clear
		
		int index = -1;
		try {
			index = Integer.parseInt(wanted);
		} catch (NumberFormatException e) {
			String l = ChatColor.stripColor(wanted);
			if (itemMap.containsKey(l))
				index = itemMap.get(l);
		}
		return index;
	}

	/**
	 * Append a new item to the menu
	 * 
	 * @param label	Label of the item to add
	 * @param command Command to be run when the item is selected
	 * @param message Feedback text to be shown when the item is selected
	 */
	public void addItem(String label, String command, String message) {
		addItem(new SMSMenuItem(this, label, command, message));
	}

	/**
	 * Append a new item to the menu
	 * 
	 * @param item	The item to be added
	 */
	public void addItem(SMSMenuItem item) {
		insertItem(items.size() + 1, item);
	}
	
	/**
	 * Insert new item in the menu, at the given position.
	 * 
	 * @param pos
	 * @param label
	 * @param command
	 * @param message
	 */
	public void insertItem(int pos, String label, String command, String message) {
		insertItem(pos, new SMSMenuItem(this, label, command, message));
	}

	/**
	 * Insert a new item in the menu, at the given position.
	 * 
	 * @param item	The item to insert
	 * @param pos	The position to insert (1-based index)
	 */
	public void insertItem(int pos, SMSMenuItem item) {
		if (items.size() != itemMap.size()) rebuildItemMap();	// workaround for Heroes 1.4.8 which calls menu.getItems().clear
	
		if (item == null)
			throw new NullPointerException();
		String l = item.getLabelStripped();
		if (itemMap.containsKey(l)) {
			throw new SMSException("Duplicate label '" + l + "' not allowed in menu '" + getName() + "'.");
		}

		if (pos > items.size()) {
			items.add(item);
			itemMap.put(l, items.size());
		} else {
			items.add(pos - 1, item);
			rebuildItemMap();
		}
		
		if (autosort) {
			Collections.sort(items);
			if (pos <= items.size()) rebuildItemMap();
		}
		
		setChanged();
		autosave();
	}
	
	/**
	 * Replace an existing menu item.  The label must already be present in the menu, 
	 * or an exception will be thrown.
	 * 
	 * @param label	Label of the menu item
	 * @param command	The command to be run
	 * @param message	The feedback message
	 * @throws SMSException if the label isn't present in the menu
	 */
	public void replaceItem(String label, String command, String message) {
		replaceItem(new SMSMenuItem(this, label, command, message));
	}
	
	/**
	 * Replace an existing menu item.  The label must already be present in the menu, 
	 * or an exception will be thrown.
	 * 
	 * @param item	The menu item to replace
	 * @throws SMSException if the label isn't present in the menu
	 */
	public void replaceItem(SMSMenuItem item) {
		if (items.size() != itemMap.size()) rebuildItemMap();	// workaround for Heroes 1.4.8 which calls menu.getItems().clear
		
		String l = item.getLabelStripped();
		if (!itemMap.containsKey(l)) {
			throw new SMSException("Label '" + l + "' is not in the menu.");
		}
		int idx = itemMap.get(l);
		items.set(idx - 1, item);
		itemMap.put(l, idx);
		
		setChanged();
		autosave();
	}
	
	/**
	 * Replace the menu item at the given 1-based index.  The new label must not already be
	 * present in the menu or an exception will be thrown - duplicates are not allowed.
	 * 
	 * @param idx
	 * @param label
	 * @param command
	 * @param message
	 */
	public void replaceItem(int idx, String label, String command, String message) {
		replaceItem(idx, new SMSMenuItem(this, label, command, message));
	}
	
	/**
	 * Replace the menu item at the given 1-based index.  The new label must not already be
	 * present in the menu or an exception will be thrown - duplicates are not allowed.
	 * 
	 * @param idx
	 * @param item
	 */
	public void replaceItem(int idx, SMSMenuItem item) {
		if (items.size() != itemMap.size()) rebuildItemMap();	// workaround for Heroes 1.4.8 which calls menu.getItems().clear
		
		String l = item.getLabelStripped();
		if (idx < 1 || idx > items.size()) {
			throw new SMSException("Index " + idx + " out of range.");
		}
		if (itemMap.containsKey(l) && idx != itemMap.get(l)) {
			throw new SMSException("Duplicate label '" + l + "' not allowed in menu '" + getName() + "'.");
		}
		itemMap.remove(items.get(idx - 1).getLabelStripped());
		items.set(idx - 1, item);
		itemMap.put(l, idx);
		
		setChanged();
		autosave();
	}

	/**
	 * Rebuild the label->index mapping for the menu.  Needed if the menu order changes
	 * (insertion, removal, sorting...)
	 */
	private void rebuildItemMap() {
		itemMap.clear();
		for (int i = 0; i < items.size(); i++) {
			itemMap.put(items.get(i).getLabelStripped(), i+1);
		}
	}

	/**
	 * Sort the menu's items by label text - see {@link SMSMenuItem#compareTo(SMSMenuItem)}
	 */
	public void sortItems() {
		Collections.sort(items);
		rebuildItemMap();
		setChanged();
		autosave();
	}

	/**
	 * Remove an item from the menu by matching label.  If the label string is
	 * just an integer value, remove the item at that 1-based numeric index.
	 * 
	 * @param indexStr	The label to search for and remove
	 * @throws IllegalArgumentException if the label does not exist in the menu
	 */
	public void removeItem(String indexStr) {
		Integer index = null;
		try {
			index = Integer.parseInt(indexStr);
		} catch (NumberFormatException e) {
			index = itemMap.get(ChatColor.stripColor(indexStr));
			if (index == null)
				throw new SMSException("No such label '" + indexStr + "' in menu '" + getName() + "'.");
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
		rebuildItemMap();
		setChanged();
		autosave();
	}

	/**
	 * Remove all items from a menu 
	 */
	public void removeAllItems() {
		items.clear();
		itemMap.clear();
		setChanged();
		autosave();
	}

	/**
	 * Permanently delete a menu, dereferencing the object and removing saved data from disk.
	 */
	void deletePermanent() {
		try {
			setChanged();
			notifyObservers(SMSMenuAction.DELETE_PERM);
			SMSMenu.unregisterMenu(getName());
			SMSPersistence.unPersist(this);
		} catch (SMSException e) {
			// Should not get here
			LogUtils.warning("Impossible: deletePermanent got SMSException?");
		}
	}

	/**
	 * Temporarily delete a menu.  The menu object is dereferenced but saved menu data is not 
	 * deleted from disk.
	 */
	void deleteTemporary() {
		try {
			SMSMenu.unregisterMenu(getName());
			notifyObservers(SMSMenuAction.DELETE_TEMP);
		} catch (SMSException e) {
			// Should not get here
			LogUtils.warning("Impossible: deleteTemporary got SMSException?");
		}
	}

	public void autosave() {
		// we only save menus which have been registered via SMSMenu.addMenu()
		if (autosave && SMSMenu.checkForMenu(getName()))
			SMSPersistence.save(this);
	}

	/**************************************************************************/

	/**
	 * Add a menu to the menu list, preserving a reference to it.
	 * 
	 * @param menuName	The menu's name
	 * @param menu		The menu object
	 * @param updateSign	Whether or not to update the menu's signs now
	 */
	static void registerMenu(String menuName, SMSMenu menu, boolean updateSign) {
		menus.put(menuName, menu);

		if (updateSign) {
			menu.notifyObservers(SMSMenuAction.REPAINT);
		}

		menu.autosave();
	}

	/**
	 * Remove a menu from the list, destroying the reference to it.
	 * 
	 * @param menuName	The menu's name
	 * @param action	Action to take on removal
	 * @throws SMSException
	 */
	static void unregisterMenu(String menuName) throws SMSException {
		menus.remove(menuName);
	}

	/**
	 * Retrieve the menu with the given name
	 * @param menuName	The name of the menu to retrieve
	 * @return	The menu object
	 * @throws SMSException if the menu name is not found
	 */
	public static SMSMenu getMenu(String menuName) throws SMSException {
		if (!menus.containsKey(menuName))
			throw new SMSException("No such menu '" + menuName + "'.");
		return menus.get(menuName);
	}

	/**
	 * Cause the views on all menus to be redrawn
	 */
	public static void updateAllMenus(){
		for (SMSMenu menu : listMenus()) {
			menu.notifyObservers(SMSMenuAction.REPAINT);
		}
	}

	/**
	 * Get the name of the menu at the given location.
	 * 
	 * @param loc	The location
	 * @return	The menu name, or null if there is no menu sign at the location
	 */
	static String getMenuNameAt(Location loc) {
		SMSView v = SMSView.getViewForLocation(loc);
		return v == null ? null : v.getNativeMenu().getName();
	}

	/**
	 * Get the menu at the given location
	 * 
	 * @param loc	The location
	 * @return	The menu object
	 * @throws SMSException if there is no menu sign at the location
	 */
	static SMSMenu getMenuAt(Location loc) throws SMSException {
		return getMenu(getMenuNameAt(loc));
	}

	/**
	 * Check to see if a menu with the given name exists
	 * 
	 * @param menuName The menu name
	 * @return true if the menu exists, false if it does not
	 */
	static boolean checkForMenu(String menuName) {
		return menus.containsKey(menuName);
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

	/**
	 * Get the usage limit details for this menu.
	 * 
	 * @return	The usage limit details
	 */
	public SMSRemainingUses getUseLimits() {
		return uses;
	}

	/**
	 * Returns a printable representation of the number of uses remaining for this item.
	 * 
	 * @return	Formatted usage information
	 */
	String formatUses() {
		return uses.toString();
	}

	/**
	 * Returns a printable representation of the number of uses remaining for this item, for the given player.
	 * 
	 * @param player	Player to retrieve the usage information for
	 * @return			Formatted usage information
	 */
	@Override
	public String formatUses(CommandSender sender) {
		if (sender instanceof Player) {
			return uses.toString(sender.getName());
		} else {
			return formatUses();
		}
	}

	/* (non-Javadoc)
	 * @see me.desht.scrollingmenusign.Freezable#getSaveFolder()
	 */
	@Override
	public File getSaveFolder() {
		return DirectoryStructure.getMenusFolder();
	}

	@Override
	public String getDescription() {
		return "menu";
	}

}
