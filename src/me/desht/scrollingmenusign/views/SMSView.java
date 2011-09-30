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
import java.util.Map.Entry;
import java.util.logging.Level;

import me.desht.scrollingmenusign.Freezable;
import me.desht.scrollingmenusign.SMSConfig;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSPersistence;
import me.desht.util.MiscUtil;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.config.ConfigurationNode;

/**
 * @author des
 *
 */
public abstract class SMSView implements Observer, Freezable {

	private static final Map<String, SMSView> allViewNames = new HashMap<String, SMSView>();
	private static final Map<Location, SMSView> allViewLocations = new HashMap<Location, SMSView>();

	private static final Map<String,Integer> viewIdx = new HashMap<String, Integer>();

	private SMSMenu menu;
	private final Set<Location> locations = new HashSet<Location>();
	private String name;
	private boolean autosave;
	private String owner;

	private boolean dirty;

	@Override
	public abstract void update(Observable menu, Object arg1);

	protected abstract void thaw(ConfigurationNode node);

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
		this.autosave = SMSConfig.getConfiguration().getBoolean("sms.autosave", true);
		this.owner = "";	// unowned by default

		menu.addObserver(this);

		registerView();
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
		map.put("owner", owner);
		List<List<Object>> locs = new ArrayList<List<Object>>();
		for (Location l: getLocations()) {
			locs.add(freezeLocation(l));
		}
		map.put("locations", locs);		
		return map;
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

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
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

	/**
	 * Register a new location as being part of this view object
	 * 
	 * @param loc	The location to register
	 * @throws SMSException if the location is not suitable for adding to this view
	 */
	public void addLocation(Location loc) throws SMSException {
		locations.add(loc);
		allViewLocations.put(loc, this);

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

	void autosave() {
		if (isAutosave() && SMSView.checkForView(getName()))
			SMSPersistence.save(this);
	}

	void registerView() {
		if (checkForView(getName())) {
			throw new IllegalArgumentException("duplicate name: " + getName());
		}

		allViewNames.put(getName(), this);
		for (Location l : getLocations()) {
			allViewLocations.put(l, this);
		}

		//		System.out.println("registered view " + getName());
	}

	private void deleteCommon() {
		getMenu().deleteObserver(this);
		allViewNames.remove(getName());
		for (Location l : getLocations()) {
			allViewLocations.remove(l);
		}
	}

	/**
	 * Temporarily delete a view.  The view is deactivated, but not removed from disk.
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
		List<SMSView> l = new ArrayList<SMSView>();
		for (Entry<String, SMSView> e : allViewNames.entrySet()) {
			if (e.getValue().getMenu() == menu) {
				l.add(e.getValue());
			}
		}
		return l;
	}

	/**
	 * Check if the given player is allowed to use this view.
	 * 
	 * @param player	The player to check
	 * @return	True if the player may use this view, false if not
	 */
	public boolean allowedToUse(Player player) {
		if (SMSConfig.getConfiguration().getBoolean("sms.use_any_view", true))
			return true;
		if (player.hasPermission("scrollingmenusign.useAnyView"))
			return true;
		if (getOwner().isEmpty() || getOwner().equalsIgnoreCase(player.getName()))
			return true;
		return false;
	}


	/**
	 * Instantiate a new view from a saved config file
	 * 
	 * @param node	The configuration
	 * @return	The view object
	 */
	public static SMSView load(ConfigurationNode node) {
		String className = node.getString("class");
		String viewName = node.getString("name");
		try {
			Class<? extends SMSView> c = Class.forName(className).asSubclass(SMSView.class);
			//			System.out.println("got class " + c.getName());
			Constructor<? extends SMSView> ctor = c.getDeclaredConstructor(String.class, SMSMenu.class);
			SMSView v = ctor.newInstance(viewName, SMSMenu.getMenu(node.getString("menu")));
			v.setAutosave(false);
			List<Object> locs = node.getList("locations");
			for (Object o : locs) {
				@SuppressWarnings("unchecked")
				List<Object> locList = (List<Object>) o;
				World w = MiscUtil.findWorld((String) locList.get(0));
				Location loc = new Location(w, (Integer)locList.get(1), (Integer)locList.get(2), (Integer)locList.get(3));
				v.addLocation(loc);
			}
			v.setOwner(node.getString("owner", ""));
			v.thaw(node);
			v.setAutosave(true);
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
			e.printStackTrace();
			MiscUtil.log(Level.WARNING, "invocation target exception while loading view " + viewName + ": " + e.getMessage());
		}
		return null;
	}
}
