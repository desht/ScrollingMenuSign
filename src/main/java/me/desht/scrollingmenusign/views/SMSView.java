package me.desht.scrollingmenusign.views;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import me.desht.scrollingmenusign.DirectoryStructure;
import me.desht.scrollingmenusign.SMSPersistable;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSPersistence;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.scrollingmenusign.enums.SMSUserAction;
import me.desht.scrollingmenusign.enums.ViewJustification;
import me.desht.dhutils.AttributeCollection;
import me.desht.dhutils.ConfigurationListener;
import me.desht.dhutils.ConfigurationManager;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.PermissionUtils;
import me.desht.dhutils.PersistableLocation;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * @author des
 *
 */
public abstract class SMSView implements Observer, SMSPersistable, ConfigurationListener {
	// view attribute names
	public static final String OWNER = "owner";
	public static final String ITEM_JUSTIFY = "item_justify";
	public static final String TITLE_JUSTIFY = "title_justify";

	// map view name to view object for registered views
	private static final Map<String, SMSView> allViewNames = new HashMap<String, SMSView>();
	// map (persistable - no World reference) location to view object for registered views
	private static final Map<PersistableLocation, SMSView> allViewLocations = new HashMap<PersistableLocation, SMSView>();
	// track the number we append to each new view name
	private static final Map<String,Integer> viewIdx = new HashMap<String, Integer>();

	private final SMSMenu menu;
	private final Set<PersistableLocation> locations = new HashSet<PersistableLocation>();
	private final String name;
	private final AttributeCollection attributes;	// view attributes to be displayed and/or edited by players

	private boolean autosave;
	private boolean dirty;
	private int maxLocations;
	// we can't use a Set here, since there are three possible values: 1) dirty, 2) clean, 3) unknown
	private final Map<String,Boolean> dirtyPlayers = new HashMap<String,Boolean>();
	// map a world name (which hasn't been loaded yet) to a list of x,y,z positions
	private final Map<String,List<Vector>> deferredLocations = new HashMap<String, List<Vector>>();

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
		this.autosave = ScrollingMenuSign.getInstance().getConfig().getBoolean("sms.autosave", true);
		this.attributes = new AttributeCollection(this);
		this.maxLocations = 1;

		menu.addObserver(this);

		registerAttribute(OWNER, "");
		registerAttribute(TITLE_JUSTIFY, ViewJustification.DEFAULT);
		registerAttribute(ITEM_JUSTIFY, ViewJustification.DEFAULT);
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

	/**
	 * Return the justification for menu items in this view
	 * 
	 * @return
	 */
	public ViewJustification getItemJustification() {
		return getJustification("sms.item_justify", ITEM_JUSTIFY, ViewJustification.LEFT);
	}

	/**
	 * Return the justification for the menu title in this view
	 * 
	 * @return
	 */
	public ViewJustification getTitleJustification() {
		return getJustification("sms.title_justify", TITLE_JUSTIFY, ViewJustification.CENTER);
	}

	private ViewJustification getJustification(String configItem, String attrName, ViewJustification fallback) {
		ViewJustification viewJust = (ViewJustification) getAttribute(attrName);
		if (viewJust != ViewJustification.DEFAULT) {
			return viewJust;
		}

		String j = ScrollingMenuSign.getInstance().getConfig().getString(configItem, fallback.toString());
		try {
			return ViewJustification.valueOf(j.toUpperCase());
		} catch (IllegalArgumentException e) {
			return fallback;
		}
	}

	public Map<String, Object> freeze() {
		Map<String, Object> map = new HashMap<String, Object>();

		map.put("name", name);
		map.put("menu", menu.getName());
		map.put("class", getClass().getName());
		for (String key : listAttributeKeys(false)) {
			map.put(key, attributes.get(key).toString());
		}
		List<PersistableLocation> locs = new ArrayList<PersistableLocation>();
		for (Location l : getLocations()) {
			PersistableLocation pl = new PersistableLocation(l);
			pl.setSavePitchAndYaw(false);
			locs.add(pl);
		}
		map.put("locations", locs);
		return map;
	}

	@SuppressWarnings("unchecked")
	protected void thaw(ConfigurationSection node) throws SMSException {
		List<Object> locs = (List<Object>) node.getList("locations");
		for (Object o : locs) {
			if (o instanceof Collection<?>) {
				addOldStyleLocation((List<Object>) o);
			} else if (o instanceof PersistableLocation) {
				PersistableLocation pl = (PersistableLocation) o;
				try {
					addLocation(pl.getLocation());
				} catch (IllegalStateException e) {
					// world not loaded? we'll defer adding this location to the view for now
					// perhaps the world will get loaded later
					addDeferredLocation(pl.getWorldName(), new Vector(pl.getX(), pl.getY(), pl.getZ()));
				}
			} else {
				throw new SMSException("invalid location in view " + getName() + " (corrupted file?)");
			}
		}
		for (String k : node.getKeys(false)) {
			if (attributes.hasAttribute(k)) {
				setAttribute(k, node.getString(k));
			}
		}
	}

	private void addOldStyleLocation(List<Object> locList) {
		String worldName = (String)locList.get(0);
		int x = (Integer) locList.get(1);
		int y = (Integer) locList.get(2);
		int z = (Integer) locList.get(3);
		try {
			World w = MiscUtil.findWorld(worldName);
			addLocation(new Location(w, x, y, z));
		} catch (IllegalArgumentException e) {
			// world not loaded? we'll defer adding this location to the view for now
			// perhaps the world will get loaded later
			addDeferredLocation(worldName, new Vector(x, y, z));
		}
	}

	private void addDeferredLocation(String worldName, Vector v) {
		List<Vector> l = deferredLocations.get(worldName);
		if (l == null) {
			l = new ArrayList<Vector>();
			deferredLocations.put(worldName, l);
		}
		l.add(v);
	}

	public List<Vector> getDeferredLocations(String worldName) {
		return deferredLocations.get(worldName);
	}

	protected List<Object> freezeLocation(Location l) {
		List<Object> list = new ArrayList<Object>();
		list.add(l.getWorld().getName());
		list.add(l.getBlockX());
		list.add(l.getBlockY());
		list.add(l.getBlockZ());

		return list;
	}

	/**
	 * Get the "dirty" status for this view - whether or not a repaint is needed for all players.
	 * 
	 * @return true if a repaint is needed, false otherwise
	 */
	public boolean isDirty() {
		return dirty;
	}

	/**
	 * Get the "dirty" status for this view - whether or not a repaint is needed for the given player.
	 * 
	 * @param playerName	The player to check for
	 * @return true if a repaint is needed, false otherwise
	 */
	public boolean isDirty(String playerName) {
		return dirtyPlayers.containsKey(playerName) ? dirtyPlayers.get(playerName) : dirty;
	}

	/**
	 * Set the "dirty" status for this view - whether or not a repaint is needed for all players.
	 * 
	 * @param dirty	true if a repaint is needed, false otherwise
	 */
	public void setDirty(boolean dirty) {
		this.dirty = dirty;
		if (dirty) {
			dirtyPlayers.clear();
		}
	}

	/**
	 * Set the "dirty" status for this view - whether or not a repaint is needed for the given player.
	 * 
	 * @param playerName	The player to check for
	 * @param dirty		Whether or not a repaint is needed
	 */
	public void setDirty(String playerName, boolean dirty) {
		dirtyPlayers.put(playerName, dirty);
	}

	/**
	 * Get a set of all locations for this view.  Views may have zero or more locations (e.g. a sign
	 * view has one location, a map view has zero locations, a hypothetical multi-sign view might have
	 * several locations...)
	 * 
	 * @return	A Set of all locations for this view object
	 * @throws IllegalStateException if the world for this view has become unloaded
	 */
	public Set<Location> getLocations() {
		Set<Location> res = new HashSet<Location>();
		for (PersistableLocation l : locations) {
			res.add(l.getLocation());
		}
		return res;
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

		locations.add(new PersistableLocation(loc));
		if (checkForView(getName())) {
			allViewLocations.put(new PersistableLocation(loc), this);
		}
		autosave();
	}

	/**
	 * Unregister a location from the given view.
	 * 
	 * @param loc	The location to unregister.
	 */
	public void removeLocation(Location loc) {
		locations.remove(new PersistableLocation(loc));
		allViewLocations.remove(new PersistableLocation(loc));

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
			allViewLocations.put(new PersistableLocation(l), this);
		}

		autosave();
	}

	private void deleteCommon() {
		getMenu().deleteObserver(this);
		allViewNames.remove(getName());
		for (Location l : getLocations()) {
			allViewLocations.remove(new PersistableLocation(l));
		}
	}

	/**
	 * Temporarily delete a view.  The view is deactivated and in-memory objects are dereferenced,
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
		return DirectoryStructure.getViewsFolder();
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
			throw new SMSException("No such view: " + name);

		return allViewNames.get(name);
	}

	/**
	 * Get the view object at the given location, if any.
	 * 
	 * @param loc	The location to check
	 * @return		The SMSView object at that location, or null if there is none
	 */
	public static SMSView getViewForLocation(Location loc) {
		return allViewLocations.get(new PersistableLocation(loc));
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
	 * Get a count of views used, keyed by view type.  Used for metrics gathering.
	 * 
	 * @return	a map of type -> count of views of that type 
	 */
	public static Map<String,Integer> getViewCounts() {
		Map<String,Integer> map = new HashMap<String, Integer>();
		for (Entry<String,SMSView> e : allViewNames.entrySet()) {
			String type = e.getValue().getType();
			if (!map.containsKey(type)) {
				map.put(type, 1);
			} else {
				map.put(type, map.get(type) + 1);
			}
		}
		return map;
	}

	/**
	 * Check if the given player is allowed to use this view.
	 * 
	 * @param player	The player to check
	 * @return	True if the player may use this view, false if not
	 */
	public boolean hasOwnerPermission(Player player) {
		boolean ignore = ScrollingMenuSign.getInstance().getConfig().getBoolean("sms.ignore_view_ownership");
		if (!ignore && !player.hasPermission("scrollingmenusign.ignoreViewOwnership")) {
			String owner = getAttributeAsString(OWNER);
			if (owner != null && !owner.isEmpty() && !owner.equalsIgnoreCase(player.getName())) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Check if the given player has owner permission to use this view.
	 * @param player	The player to check
	 * @return True if the player may use this view, false if not
	 * @deprecated Use hasOwnerPermission(player)
	 */
	@Deprecated
	public boolean allowedToUse(Player player) {
		return hasOwnerPermission(player);
	}

	/**
	 * Require that the given player is allowed to use this view, and throw a SMSException if not.
	 * 
	 * @param player	The player to check
	 */
	public void ensureAllowedToUse(Player player) {
		if (!hasOwnerPermission(player)) {
			throw new SMSException("You do not own that view");
		}

		if (!PermissionUtils.isAllowedTo(player, "scrollingmenusign.use." + getType())) {
			throw new SMSException("You don't have permission to use this type of view");
		}
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
			loadError(viewName, e);
		} catch (SMSException e) {
			loadError(viewName, e);
		} catch (InstantiationException e) {
			loadError(viewName, e);
		} catch (IllegalAccessException e) {
			loadError(viewName, e);
		} catch (SecurityException e) {
			loadError(viewName, e);
		} catch (NoSuchMethodException e) {
			loadError(viewName, e);
		} catch (IllegalArgumentException e) {
			loadError(viewName, e);
		} catch (InvocationTargetException e) {
			loadError(viewName, e);
		}
		return null;
	}

	private static void loadError(String viewName, Exception e) {
		LogUtils.warning("Caught " + e.getClass().getName() + " while loading view " + viewName);
		LogUtils.warning("  Exception message: " + e.getMessage());
	}

	protected void registerAttribute(String attr, Object def) {
		attributes.registerAttribute(attr, def);
	}

	public AttributeCollection getAttributes() {
		return attributes;
	}

	public Object getAttribute(String k)  {
		return attributes.get(k);
	}

	public String getAttributeAsString(String k, String def) {
		Object o = getAttribute(k);
		return o == null || o.toString().isEmpty() ? def : o.toString();
	}

	public String getAttributeAsString(String k)  {
		return getAttributeAsString(k, "");
	}

	public void setAttribute(String k, String val) throws SMSException {
		if (!attributes.contains(k)) {
			throw new SMSException("No such view attribute: " + k);
		}
		attributes.set(k, val);
	}

	public Set<String> listAttributeKeys(boolean isSorted) {
		return attributes.listAttributeKeys(isSorted);
	}

	/* (non-Javadoc)
	 * @see me.desht.dhutils.ConfigurationListener#onConfigurationChanged(me.desht.dhutils.ConfigurationManager, java.lang.String, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void onConfigurationChanged(ConfigurationManager configurationManager, String key, Object oldVal, Object newVal) {
		update(getMenu(), SMSMenuAction.REPAINT);
	}

	/* (non-Javadoc)
	 * @see me.desht.dhutils.ConfigurationListener#onConfigurationValidate(me.desht.dhutils.ConfigurationManager, java.lang.String, java.lang.String)
	 */
	@Override
	public void onConfigurationValidate(ConfigurationManager configurationManager, String key, String val) {
		// no validation here, override in subclasses
	}

	/* (non-Javadoc)
	 * @see me.desht.dhutils.ConfigurationListener#onConfigurationValidate(me.desht.dhutils.ConfigurationManager, java.lang.String, java.util.List)
	 */
	@Override
	public void onConfigurationValidate(ConfigurationManager configurationManager, String key, List<?> val) {
		// no validation here, override in subclasses
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
	 * Called automatically when the view is scrolled.  Override and extend this
	 * in subclasses.
	 * 
	 * @param player	The player who did the scrolling
	 * @param action	The scroll direction: SCROLLDOWN or SCROLLUP
	 */
	public void onScrolled(Player player, SMSUserAction action) {
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

	/**
	 * Load any deferred locations for the given world.  This is called by the WorldLoadEvent handler.
	 * 
	 * @param world	The world that's just been loaded.
	 */
	public static void loadDeferred(World world) {
		for (SMSView view : listViews()) {
			List<Vector> l = view.getDeferredLocations(world.getName());	
			if (l == null) {
				continue;
			}

			for (Vector vec : l) {
				try {
					view.addLocation(new Location(world, vec.getBlockX(), vec.getBlockY(), vec.getBlockZ()));
					LogUtils.fine("added loc " + world.getName() + ", " + vec + " to view " + view.getName());
				} catch (SMSException e) {
					LogUtils.warning("Can't add location " + world.getName() + ", " + vec + " to view " + view.getName());
					LogUtils.warning("  Exception message: " + e.getMessage());
				}
			}
			l.clear();
		}
	}
}
