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
import org.bukkit.util.config.ConfigurationNode;

public abstract class SMSView implements Observer, Freezable {

	private static final Map<String, SMSView> allViewNames = new HashMap<String, SMSView>();
	private static final Map<Location, SMSView> allViewLocations = new HashMap<Location, SMSView>();

	private static final Map<String,Integer> viewIdx = new HashMap<String, Integer>();
	
	private SMSMenu menu;
	private final Set<Location> locations = new HashSet<Location>();
	private String name;
	private boolean autosave;

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

	public boolean isAutosave() {
		return autosave;
	}

	public void setAutosave(boolean autosave) {
		this.autosave = autosave;
	}

	public String getName() {
		return name;	
	}

	public SMSMenu getMenu() {
		return menu;
	}

	public Map<String, Object> freeze() {
		Map<String, Object> map = new HashMap<String, Object>();

		map.put("name", name);
		map.put("menu", menu.getName());
		map.put("class", getClass().getName());
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

	public boolean isDirty() {
		return dirty;
	}

	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	public Set<Location> getLocations() {	
		return locations;
	}

	public Location[] getLocationsArray() {
		Set<Location> locs = getLocations();
		return locs.toArray(new Location[locs.size()]);
	}

	public void addLocation(Location loc) throws SMSException {
		locations.add(loc);
		allViewLocations.put(loc, this);
		
		autosave();
	}

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
		
		System.out.println("registered view " + getName());
		
	}

	private void deleteCommon() {
		getMenu().deleteObserver(this);
		allViewNames.remove(getName());
		for (Location l : getLocations()) {
			allViewLocations.remove(l);
		}
	}

	public void deleteTemporary() {
		deleteCommon();
	}

	public void deletePermanent() {
		deleteCommon();
		SMSPersistence.unPersist(this);
	}

	public File getSaveFolder() {
		return SMSConfig.getViewsFolder();
	}

	public static boolean checkForView(String name) {
		return allViewNames.containsKey(name);
	}

	public static List<SMSView> listViews() {
		return new ArrayList<SMSView>(allViewNames.values());
	}

	public static SMSView[] getViewsAsArray() {
		return allViewNames.values().toArray(new SMSView[allViewNames.size()]);
	}

	public static SMSView getView(String name) throws SMSException {
		if (!checkForView(name))
			throw new SMSException("No such view " + name);
		
		return allViewNames.get(name);
	}

	public static SMSView getViewForLocation(Location loc) {
		return allViewLocations.get(loc);
	}

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
			System.out.println("got class " + c.getName());
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
			v.thaw(node);
			v.setAutosave(true);
			return v;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			MiscUtil.log(Level.WARNING, "can't load view " + viewName + ": " + e.getMessage());
		} catch (SMSException e) {
			e.printStackTrace();
			MiscUtil.log(Level.WARNING, "can't load view " + viewName + ": " + e.getMessage());
		} catch (InstantiationException e) {
			e.printStackTrace();
			MiscUtil.log(Level.WARNING, "can't load view " + viewName + ": " + e.getMessage());
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			MiscUtil.log(Level.WARNING, "can't load view " + viewName + ": " + e.getMessage());
		} catch (SecurityException e) {
			e.printStackTrace();
			MiscUtil.log(Level.WARNING, "can't load view " + viewName + ": " + e.getMessage());
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
			MiscUtil.log(Level.WARNING, "can't load view " + viewName + ": " + e.getMessage());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			MiscUtil.log(Level.WARNING, "can't load view " + viewName + ": " + e.getMessage());
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			MiscUtil.log(Level.WARNING, "can't load view " + viewName + ": " + e.getMessage());
		}
		return null;
	}
}
