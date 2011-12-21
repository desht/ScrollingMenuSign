package me.desht.scrollingmenusign.views;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;

import me.desht.scrollingmenusign.Freezable;
import me.desht.scrollingmenusign.SMSConfig;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSPersistence;
import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.scrollingmenusign.util.MiscUtil;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

/**
 * @author des
 *
 */
public abstract class SMSView implements Observer, Freezable {

	// attributes
	protected static final String OWNER = "owner";
	// map view name to view object for registered views
	private static final Map<String, SMSView> allViewNames = new HashMap<String, SMSView>();
	// map location to view object for registered views
	private static final Map<Location, SMSView> allViewLocations = new HashMap<Location, SMSView>();

	private static final Map<String,Integer> viewIdx = new HashMap<String, Integer>();

	private SMSMenu menu;
	private final Set<Location> locations = new HashSet<Location>();
	private String name;
	private boolean autosave;
	private boolean dirty;
	private Configuration attributes;	// view attributes to be displayed and/or edited by players
	private int maxLocations;

	@Override
	public abstract void update(Observable menu, Object arg1);

	/**
	 * Get a user-friendly string representing the type of this view.
	 * 
	 * @return	The type of view this is.
	 */
	public abstract String getType();

	public SMSView(SMSMenu menu) {
		this(null, menu);
	}

	public SMSView(String name, SMSMenu menu) {
		if (name == null) {
			name = makeUniqueName(menu.getName());
		}
		this.name = name;
		this.menu = menu;
		this.dirty = true;
		this.autosave = SMSConfig.getConfig().getBoolean("sms.autosave", true);
		this.attributes = new YamlConfiguration();
		this.maxLocations = 1;

		menu.addObserver(this);

		registerAttribute(OWNER, "");
	}

	private String makeUniqueName(String base) {
		Integer idx;
		if (viewIdx.containsKey(base)) {
			idx = viewIdx.get(base);
		} else {
			idx = 1;
			viewIdx.put(base, 1);
		}

		String s = String.format("%s-%d", base, idx);
		while (SMSView.checkForView(s)) {
			idx++;
			s = String.format("%s-%d", base, idx);
		}
		viewIdx.put(base, idx + 1);
		return s;
	}

	/**
	 * Get the view's autosave status - will the view be automatically saved to disk when modified?
	 * 
	 * @return	true or false
	 */
	public boolean isAutosave() {
		return autosave;
	}

	/**
	 * Set the view's autosave status - will the view be automatically saved to disk when modified?
	 * 
	 * @param autosave	true or false
	 */
	public void setAutosave(boolean autosave) {
		this.autosave = autosave;
	}

	/* (non-Javadoc)
	 * @see me.desht.scrollingmenusign.Freezable#getName()
	 */
	public String getName() {
		return name;	
	}

	/**
	 * Get the menu associated with this view.
	 * 
	 * @return	The SMSMenu object that this view is a view for.
	 */
	public SMSMenu getMenu() {
		return menu;
	}

	public Map<String, Object> freeze() {
		Map<String, Object> map = new HashMap<String, Object>();

		map.put("name", name);
		map.put("menu", menu.getName());
		map.put("class", getClass().getName());
		for (String key : listAttributeKeys(false)) {
			map.put(key, attributes.get(key).toString());
		}
		List<List<Object>> locs = new ArrayList<List<Object>>();
		for (Location l: getLocations()) {
			locs.add(freezeLocation(l));
		}
		map.put("locations", locs);
		return map;
	}

	@SuppressWarnings("unchecked")
	protected void thaw(ConfigurationSection node) throws SMSException {
		List<Object> locs = node.getList("locations");
		for (Object o : locs) {
			List<Object> locList = (List<Object>) o;
			World w = MiscUtil.findWorld((String) locList.get(0));
			Location loc = new Location(w, (Integer)locList.get(1), (Integer)locList.get(2), (Integer)locList.get(3));
			addLocation(loc);
		}
		for (String k : node.getKeys(false)) {
			if (hasAttribute(k)) {
				setAttribute(k, node.getString(k));
			}
		}
	}

	private List<Object> freezeLocation(Location l) {
		List<Object> list = new ArrayList<Object>();
		list.add(l.getWorld().getName());
		list.add(l.getBlockX());
		list.add(l.getBlockY());
		list.add(l.getBlockZ());

		return list;
	}

	/**
	 * Get the "dirty" status for this view - whether or not a repaint is needed.
	 * 
	 * @return true if a repaint is needed, false otherwise
	 */
	public boolean isDirty() {
		return dirty;
	}

	/**
	 * Set the "dirty" status for this view - whether or not a repaint is needed.
	 * 
	 * @param dirty	true if a repaint is needed, false otherwise
	 */
	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	@Deprecated
	public String getOwner() {
		return getAttributeAsString(OWNER);
	}

	@Deprecated
	public void setOwner(String owner) {
		try {
			setAttribute(OWNER, owner);
		} catch (SMSException e) {
			// shouldn't get here... ignore it
		}
	}

	/**
	 * Get a set of all locations for this view.  Views may have zero or more locations (e.g. a sign
	 * view has one location, a map view has zero locations, a hypothetical multi-sign view might have
	 * several locations...)
	 * 
	 * @return	A Set of all locations for this view object
	 */
	public Set<Location> getLocations() {	
		return locations;
	}

	/**
	 * Get a list of all locations for this view as a Java array.
	 * 
	 * @return	An array of all locations for this view object
	 */
	public Location[] getLocationsArray() {
		Set<Location> locs = getLocations();
		return locs.toArray(new Location[locs.size()]);
	}

	protected void setMaxLocations(int maxLocations) {
		this.maxLocations = maxLocations;
	}

	/**
	 * Get the maximum number of locations that this view may occupy.
	 * 
	 * @return	The maximum number of locations
	 */
	public int getMaxLocations() {
		return maxLocations;
	}

	/**
	 * Register a new location as being part of this view object
	 * 
	 * @param loc	The location to register
	 * @throws SMSException if the location is not suitable for adding to this view
	 */
	public void addLocation(Location loc) throws SMSException {
		if (getLocations().size() >= getMaxLocations())
			throw new SMSException("View " + getName() + " already occupies the maximum number of locations (" + getMaxLocations() + ")");

		SMSView v = SMSView.getViewForLocation(loc);
		if (v != null) {
			throw new SMSException("Location " + MiscUtil.formatLocation(loc) + " already contains view on menu: " + v.getMenu().getName());
		}

		locations.add(loc);
		if (checkForView(getName())) {
			allViewLocations.put(loc, this);
		}
		autosave();
	}

	/**
	 * Unregister a location from the given view.
	 * 
	 * @param loc	The location to unregister.
	 */
	public void removeLocation(Location loc) {
		locations.remove(loc);
		allViewLocations.remove(loc);

		autosave();
	}

	/**
	 * Save this view's contents to disk (if autosaving is enabled, and the view
	 * is registered).
	 */
	public void autosave() {
		if (isAutosave() && SMSView.checkForView(getName()))
			SMSPersistence.save(this);
	}

	public void screenClosed() {
		// TODO Auto-generated method stub

	}

	/**
	 * Register this view in the global view list.
	 */
	public void register() {
		if (checkForView(getName())) {
			throw new IllegalArgumentException("duplicate name: " + getName());
		}

		allViewNames.put(getName(), this);
		for (Location l : getLocations()) {
			allViewLocations.put(l, this);
		}
	}

	private void deleteCommon() {
		getMenu().deleteObserver(this);
		allViewNames.remove(getName());
		for (Location l : getLocations()) {
			allViewLocations.remove(l);
		}
	}

	/**
	 * Temporarily delete a view.  The view is deactivated and in-memory objects are dereference,
	 * but the saved view data is not removed from disk.
	 */
	public void deleteTemporary() {
		deleteCommon();
	}

	/**
	 * Permanently delete a view.  The view is deactivated and purged from persisted storage on disk.
	 */
	public void deletePermanent() {
		deleteCommon();
		SMSPersistence.unPersist(this);
	}

	/* (non-Javadoc)
	 * @see me.desht.scrollingmenusign.Freezable#getSaveFolder()
	 */
	public File getSaveFolder() {
		return SMSConfig.getViewsFolder();
	}

	/**
	 * Check to see if the name view exists
	 * 
	 * @param name	The view name
	 * @return		true if the named view exists, false otherwise
	 */
	public static boolean checkForView(String name) {
		return allViewNames.containsKey(name);
	}

	/**
	 * Get all known view objects as a List
	 * 
	 * @return	A list of all known views
	 */
	public static List<SMSView> listViews() {
		return new ArrayList<SMSView>(allViewNames.values());
	}

	/**
	 * Get all known view objects as a Java array
	 * 
	 * @return	An array of all known views
	 */
	public static SMSView[] getViewsAsArray() {
		return allViewNames.values().toArray(new SMSView[allViewNames.size()]);
	}

	/**
	 * Get the named SMSView object
	 * 
	 * @param name	The view name
	 * @return		The SMSView object of that name
	 * @throws SMSException	if there is no such view with the given name
	 */
	public static SMSView getView(String name) throws SMSException {
		if (!checkForView(name))
			throw new SMSException("No such view " + name);

		return allViewNames.get(name);
	}

	/**
	 * Get the view object at the given location, if any.
	 * 
	 * @param loc	The location to check
	 * @return		The SMSView object at that location, or null if there is none
	 */
	public static SMSView getViewForLocation(Location loc) {
		return allViewLocations.get(loc);
	}

	/**
	 * Find all the views for the given menu.
	 * 
	 * @param menu	The menu object to check
	 * @return	A list of SMSView objects which are views for that menu
	 */
	public static List<SMSView> getViewsForMenu(SMSMenu menu) {
		return getViewsForMenu(menu, false);
	}
	
	/**
	 *  Find all the views for the given menu, optionally sorting the resulting list.
	 *  
	 * @param menu	The menu object to check
	 * @param isSorted	If true, sort the returned view list by view name
	 * @return	A list of SMSView objects which are views for that menu
	 */
	public static List<SMSView> getViewsForMenu(SMSMenu menu, boolean isSorted) {
		if (isSorted) {
			SortedSet<String> sorted = new TreeSet<String>(allViewNames.keySet());
			List<SMSView> res = new ArrayList<SMSView>();
			for (String name : sorted) {
				SMSView v = allViewNames.get(name);
				if (v.getMenu() == menu) {
					res.add(v);
				}
			}
			return res;
		} else {
			return new ArrayList<SMSView>(allViewNames.values());
		}
	}

	/**
	 * Check if the given player is allowed to use this view.
	 * 
	 * @param player	The player to check
	 * @return	True if the player may use this view, false if not
	 */
	public boolean allowedToUse(Player player) {
		if (SMSConfig.getConfig().getBoolean("sms.ignore_view_ownership"))
			return true;
		if (player.hasPermission("scrollingmenusign.ignoreViewOwnership"))
			return true;

		String owner = getAttributeAsString(OWNER);
		if (owner == null || owner.isEmpty() || owner.equalsIgnoreCase(player.getName()))
			return true;
		return false;
	}

	/**
	 * Instantiate a new view from a saved config file
	 * 
	 * @param node	The configuration
	 * @return	The view object
	 */
	public static SMSView load(ConfigurationSection node) {
		String viewName = null;
		try {
			SMSPersistence.mustHaveField(node, "class");
			SMSPersistence.mustHaveField(node, "name");
			SMSPersistence.mustHaveField(node, "menu");
			
			String className = node.getString("class");
			viewName = node.getString("name");
			
			Class<? extends SMSView> c = Class.forName(className).asSubclass(SMSView.class);
			//			System.out.println("got class " + c.getName());
			Constructor<? extends SMSView> ctor = c.getDeclaredConstructor(String.class, SMSMenu.class);
			SMSView v = ctor.newInstance(viewName, SMSMenu.getMenu(node.getString("menu")));
			v.thaw(node);
			v.register();
			return v;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			MiscUtil.log(Level.WARNING, "can't find class for view " + viewName + ": " + e.getMessage());
		} catch (SMSException e) {
			e.printStackTrace();
			MiscUtil.log(Level.WARNING, "can't load view " + viewName + ": " + e.getMessage());
		} catch (InstantiationException e) {
			e.printStackTrace();
			MiscUtil.log(Level.WARNING, "can't instantiate view " + viewName + ": " + e.getMessage());
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			MiscUtil.log(Level.WARNING, "illegal access while loading view " + viewName + ": " + e.getMessage());
		} catch (SecurityException e) {
			e.printStackTrace();
			MiscUtil.log(Level.WARNING, "security exception while loading view " + viewName + ": " + e.getMessage());
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
			MiscUtil.log(Level.WARNING, "no such method while loading view " + viewName + ": " + e.getMessage());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			MiscUtil.log(Level.WARNING, "illegal argument while loading view " + viewName + ": " + e.getMessage());
		} catch (InvocationTargetException e) {
			MiscUtil.log(Level.WARNING, "invocation target exception while loading view " + viewName + ": " + e.getCause().getMessage());
		}
		return null;
	}

	protected void registerAttribute(String attr, Object def) {
		attributes.addDefault(attr, def);
	}

	public Configuration getAttributes() {
		return attributes;
	}

	public Object getAttribute(String k)  {
		return attributes.get(k);
	}

	public String getAttributeAsString(String k)  {
		Object o = getAttribute(k);
		return o == null ? "(none)" : o.toString();
	}

	public void setAttribute(String k, String val) throws SMSException {
		if (!attributes.contains(k)) {
			throw new SMSException("No such view attribute: " + k);
		}
		String oldVal = getAttributeAsString(k);
		onAttributeValidate(k, oldVal, val);
		SMSConfig.setConfigItem(attributes, k, val);
		String newVal = attributes.get(k).toString();
		onAttributeChanged(k, oldVal, newVal);
	}

	/**
	 * Called automatically when an attribute is about to be changed.  Override and extend this
	 * in subclasses.
	 * @param attribute		The attribute name	
	 * @param curVal		The current value
	 * @param newVal		The proposed new value
	 * @throws SMSException to prevent the attribute being changed (the exception's message will be
	 * passed to the player)
	 */
	protected void onAttributeValidate(String attribute, String curVal, String newVal) throws SMSException {
	}

	public boolean hasAttribute(String k) {
		return attributes.getDefaults().contains(k);
	}

	public Set<String> listAttributeKeys(boolean isSorted) {
		if (isSorted) {
			SortedSet<String> sorted = new TreeSet<String>(attributes.getDefaults().getKeys(false));
			return sorted;
		} else {
			return attributes.getDefaults().getKeys(false);
		}
	}

	/**
	 * Called automatically when an attribute is changed.  Override and extend this
	 * in subclasses.
	 */
	protected void onAttributeChanged(String attribute, String oldVal, String newVal) {
		update(getMenu(), SMSMenuAction.REPAINT);
	}

	/**
	 * Called automatically when the view is used to execute a menu item.  Override and extend this
	 * in subclasses.
	 * 
	 * @param player	The player who did the execution
	 */
	public void onExecuted(Player player) {
		// does nothing
	}
	
	
	/**
	 * Called automatically when a player logs out.  Call the clearPlayerForView() method on all
	 * known views.
	 * 
	 * @param player
	 */
	public static void clearPlayer(Player player) {
		for (SMSView v : listViews()) {
			v.clearPlayerForView(player);
		}
	}
	
	/**
	 * Called automatically when a player logs out.  Perform any cleardown work to remove player
	 * records from the view.  Override and extend this in subclasses.
	 * 
	 * @param player	The player who logged out
	 */
	public void clearPlayerForView(Player player) {
	}
}
