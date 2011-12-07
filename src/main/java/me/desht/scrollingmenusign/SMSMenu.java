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
import java.util.logging.Level;

import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.scrollingmenusign.util.MiscUtil;
import me.desht.scrollingmenusign.views.SMSSignView;
import me.desht.scrollingmenusign.views.SMSView;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;

/**
 * @author des
 *
 */
public class SMSMenu extends Observable implements Freezable {
	private String name;
	private String title;
	private String owner;
	private List<SMSMenuItem> items;
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
	 * @param node 		A Bukkit ConfigurationNode containg the menu's properties
	 * @throws SMSException If there is already a menu at this location
	 */
	@SuppressWarnings("unchecked")
	SMSMenu(ConfigurationSection node) throws SMSException {
		SMSPersistence.mustHaveField(node, "name");
		SMSPersistence.mustHaveField(node, "title");
		SMSPersistence.mustHaveField(node, "owner");
		
		initCommon(node.getString("name"),
		           MiscUtil.parseColourSpec(null, node.getString("title")),
		           node.getString("owner"));

		autosort = node.getBoolean("autosort", false);
		uses = new SMSRemainingUses(this, node.getConfigurationSection("usesRemaining"));
		defaultCommand = node.getString("defaultCommand", "");

		loadLegacyLocations(node);	

		List<Map<String,Object>> items = node.getList("items");
		for (Map<String,Object> item : items) {
			MemoryConfiguration itemNode = new MemoryConfiguration();
			// need to expand here because the item may contain a usesRemaining object - item could contain a nested map
			SMSPersistence.expandMapIntoConfig(itemNode, item);
			SMSMenuItem menuItem = new SMSMenuItem(this, itemNode);
			addItem(menuItem);
		}
	}


	/**
	 * In v0.5 and older, locations were in the menu object.  Now they are in the view
	 * object.
	 * 
	 * @param node
	 * @throws SMSException
	 */
	@SuppressWarnings("unchecked")
	private void loadLegacyLocations(ConfigurationSection node) throws SMSException {
		List<Object> locs = node.getList("locations");
		if (locs != null) {
			// v0.3 or newer format - multiple locations per menu
			for (Object o : locs) {
				List<Object> locList = (List<Object>) o;
				World w = MiscUtil.findWorld((String) locList.get(0));
				Location loc = new Location(w, (Integer)locList.get(1), (Integer)locList.get(2), (Integer)locList.get(3));
				try {
					SMSView v = SMSSignView.addSignToMenu(this, loc);
					System.out.println("add view " + v.getName() + " to menu " + getName());
					SMSPersistence.save(v);
				} catch (SMSException e) {
					MiscUtil.log(Level.WARNING, "Could not add sign to menu " + name + ": " + e.getMessage());
				}
				SMSPersistence.save(this);
			}

		} else {
			// v0.2 or older
			String worldName = node.getString("world");
			if (worldName != null) {
				World w = MiscUtil.findWorld(worldName);
				List<Integer>locList = (List<Integer>) node.getList("location", null);
				SMSView v = SMSSignView.addSignToMenu(this, new Location(w, locList.get(0), locList.get(1), locList.get(2)));
				SMSPersistence.save(v);
				SMSPersistence.save(this);
			}
		}
	}

	private void initCommon(String n, String t, String o) {
		items = new ArrayList<SMSMenuItem>();
		name = n;
		title = t;
		owner = o;
		autosave = SMSConfig.getConfig().getBoolean("sms.autosave", true);
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
	 * @param index	1-based numeric index
	 * @return		The menu item at that index
	 */
	public SMSMenuItem getItem(int index) {
		return items.get(index - 1);
	}

	/**
	 * Get the menu item matching the given label
	 * 
	 * @param wanted	The label to match (case-insensitive)
	 * @return			The menu item matching that index
	 */
	public SMSMenuItem getItem(String wanted) {
		if (wanted == null || wanted.isEmpty()) {
			return null;
		}

		try {
			return items.get(indexOfItem(wanted) - 1);
		} catch (ArrayIndexOutOfBoundsException e) {
			return null;
		}
	}

	/**
	 * Get the index of the item matching the given label
	 * 
	 * @param wanted	Label to match
	 * @return			1-based item index
	 */
	public int indexOfItem(String wanted) {
		int index = -1;
		try {
			index = Integer.parseInt(wanted);
		} catch (NumberFormatException e) {
			// not an integer - try to remove by label
			String wantedClean = MiscUtil.deColourise(wanted);
			for (int i = 0; i < items.size(); i++) {
				String label = MiscUtil.deColourise(items.get(i).getLabel());
				if (wantedClean.equalsIgnoreCase(label)) {
					index = i + 1;
					break;
				}
			};
		}
		return index;
	}



	/**
	 * Add a new item to the menu
	 * 
	 * @param label	Label of the item to add
	 * @param command Command to be run when the item is selected
	 * @param message Feedback text to be shown when the item is selected
	 */
	public void addItem(String label, String command, String message) {
		addItem(new SMSMenuItem(this, label, command, message));
	}

	/**
	 * Add a new item to the menu
	 * 
	 * @param item	The item to be added
	 */
	void addItem(SMSMenuItem item) {
		if (item == null)
			throw new NullPointerException();

		items.add(item);
		if (autosort)
			Collections.sort(items);

		setChanged();

		autosave();
	}

	/**
	 * Sort the menu's items by label text - see {@link SMSMenuItem#compareTo(SMSMenuItem)}
	 */
	public void sortItems() {
		Collections.sort(items);
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
		int index = -1;
		try {
			index = Integer.parseInt(indexStr);
		} catch (NumberFormatException e) {
			// not an integer - try to remove by label
			for (int i = 0; i < items.size(); i++) {
				String label = MiscUtil.deColourise(items.get(i).getLabel());
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
		setChanged();

		autosave();
	}

	/**
	 * Remove all items from a menu 
	 */
	public void removeAllItems() {
		items.clear();
		setChanged();

		autosave();
	}

	/**
	 * Permanently delete a menu, blanking all its signs
	 */
	void delete() {
		deletePermanent();
	}

	/**
	 * Permanently delete a menu
	 * @param action	Action to take on the menu's signs
	 */
	void delete(SMSMenuAction action) {
		deletePermanent(action);
	}

	private void deleteCommon(SMSMenuAction action) throws SMSException {
		SMSMenu.removeMenu(getName(), action);	
	}

	private void deleteAllViews(boolean permanent) {
		List<SMSView> toDelete = new ArrayList<SMSView>();
		for (SMSView view : SMSView.listViews()) {
			if (view.getMenu() == this)	{
				toDelete.add(view);
			}
		}
		for (SMSView view : toDelete) {
			if (permanent) {
				view.deletePermanent();	
			} else {
				view.deleteTemporary();
			}
		}
	}

	/**
	 * Permanently delete a menu, dereferencing the object and removing saved data from disk.
	 */
	void deletePermanent() {
		deletePermanent(SMSMenuAction.BLANK_SIGN);
	}

	/**
	 * Permanently delete a menu, dereferencing the object and removing saved data from disk.
	 * 
	 * @param action	The action to carry out on the menu's views
	 */
	void deletePermanent(SMSMenuAction action) {
		try {
			deleteCommon(action);
			deleteAllViews(true);
			SMSPersistence.unPersist(this);
		} catch (SMSException e) {
			// Should not get here
			MiscUtil.log(Level.WARNING, "Impossible: deletePermanent got SMSException?");
		}
	}

	/**
	 * Temporarily delete a menu.  The menu object is dereferenced but saved menu data is not 
	 * deleted from disk.
	 */
	void deleteTemporary() {
		try {
			deleteCommon(SMSMenuAction.DO_NOTHING);
			deleteAllViews(false);
		} catch (SMSException e) {
			// Should not get here
			MiscUtil.log(Level.WARNING, "Impossible: deleteTemporary got SMSException?");
		}
	}

	void autosave() {
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
	static void addMenu(String menuName, SMSMenu menu, boolean updateSign) {
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
	static void removeMenu(String menuName, SMSMenuAction action) throws SMSException {
		SMSMenu menu = getMenu(menuName);
		menu.notifyObservers(action);
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
		return v == null ? null : v.getMenu().getName();
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
	 * Return the name of the menu sign that the player is looking at, if any
	 * 
	 * @param player	The Bukkit player object
	 * @param complain	Whether or not to throw an exception if there is no menu
	 * @return	The menu name, or null if there is no menu and <b>complain</b> is false
	 * @throws SMSException	if there is not menu and <b>complain</b> is true
	 */
	public static String getTargetedMenuSign(Player player, boolean complain) throws SMSException {
		Block b = player.getTargetBlock(null, 3);
		String name = SMSMenu.getMenuNameAt(b.getLocation());
		if (name == null && complain)
			throw new SMSException("You are not looking at a menu.");
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
	public String formatUses(Player player) {
		if (player == null) {
			return formatUses();
		} else {
			return uses.toString(player.getName());
		}
	}

	/* (non-Javadoc)
	 * @see me.desht.scrollingmenusign.Freezable#getSaveFolder()
	 */
	@Override
	public File getSaveFolder() {
		return SMSConfig.getMenusFolder();
	}
}
