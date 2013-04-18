package me.desht.scrollingmenusign.views.redout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.material.Lever;
import org.bukkit.material.Redstone;

import me.desht.scrollingmenusign.SMSException;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PersistableLocation;
import me.desht.scrollingmenusign.views.SMSGlobalScrollableView;
import me.desht.scrollingmenusign.views.SMSView;

public class Switch implements Comparable<Switch> {
	private static final Map<PersistableLocation,Switch> allSwitchLocs = new HashMap<PersistableLocation,Switch>();
	private static final Map<String, Switch> allSwitches = new HashMap<String,Switch>();

	private static Map<String, Set<ConfigurationSection>> deferred = new HashMap<String, Set<ConfigurationSection>>();

	private final SMSGlobalScrollableView view;
	private final PersistableLocation location;
	private final String trigger;
	private final String name;

	public Switch(SMSGlobalScrollableView view, String trigger, Location location) {
		this.view = view;
		this.location = new PersistableLocation(location);
		this.trigger = trigger;
		this.name = makeUniqueName(view.getName());

		initCommon();
	}

	public Switch(SMSGlobalScrollableView view, ConfigurationSection conf) throws SMSException {
		String worldName = conf.getString("world");
		World w = Bukkit.getWorld(worldName);
		Validate.notNull(w, "World not available");

		this.view = view;
		Location loc = new Location(w, conf.getInt("x"), conf.getInt("y"), conf.getInt("z"));
		this.location = new PersistableLocation(loc);
		this.trigger = MiscUtil.parseColourSpec(conf.getString("trigger"));
		this.name = makeUniqueName(view.getName());

		initCommon();
	}

	private void initCommon() {
		allSwitches.put(name, this);
		allSwitchLocs.put(location, this);
		view.addSwitch(this);
	}

	/**
	 * Get the view this output switch belongs to.
	 * 
	 * @return	The owning view
	 */
	public SMSGlobalScrollableView getView() {
		return view;
	}

	/**
	 * Get the name of this output switch.
	 * 
	 * @return	The switch's name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get this output switch's location.
	 * 
	 * @return	The switch's location
	 */
	public Location getLocation() {
		return location.getLocation();
	}

	/**
	 * Get the trigger string for this switch.
	 * 
	 * @return	The switch's trigger string
	 */
	public String getTrigger() {
		return trigger;
	}

	/**
	 * Remove this switch from its owning view.
	 */
	public void delete() {
		allSwitches.remove(name);
		allSwitchLocs.remove(location);
		view.removeSwitch(this);
	}

	/**
	 * Get the Material for this switch.  (Right now, only Lever is supported).
	 * 
	 * @return	The switch's material
	 */
	private Material getSwitchType() {
		return getLocation().getBlock().getType();
	}

	/**
	 * Check if this switch is currently powered.
	 * 
	 * @return	true if powered, false otherwise
	 */
	public boolean getPowered() {
		Block b = getLocation().getBlock();
		if (getSwitchType() == Material.LEVER) {
			return ((Redstone)b.getState().getData()).isPowered();
		} else {
			LogUtils.warning("Found " + getSwitchType() + " at " + location + " - expecting LEVER!");
			return false;
		}
	}

	/**
	 * Set the powered status of this switch.
	 * 
	 * @param powered	true to switch on, false to switch off
	 */
	public void setPowered(boolean powered) {
		if (getSwitchType() == Material.LEVER) {
			setLeverPowered(getLocation().getBlock(), powered);
		} else {
			LogUtils.warning("Found " + getSwitchType() + " at " + location + " - expecting LEVER!");
		}
	}

	private void setLeverPowered(Block b, boolean powered) {
		Lever lever = (Lever) b.getState().getData();
		lever.setPowered(powered);
		b.setData(lever.getData(), true);
	}

	/**
	 * Retrieve the switch with the given name
	 * 
	 * @param name	The desired name
	 * @return	The Switch object, or null if no switch by this name exists
	 */
	public static Switch getSwitch(String name) {
		return allSwitches.get(name);
	}

	/**
	 * Retrieve the switch at the given location
	 * 
	 * @param loc	Location to chekc
	 * @return	The Switch object, or null if no switch at this location
	 */
	public static Switch getSwitchAt(Location loc) {
		return allSwitchLocs.get(new PersistableLocation(loc));
	}

	public static List<Switch> getSwitches() {
		return new ArrayList<Switch>(allSwitchLocs.values());
	}

	/**
	 * Check if a switch by the given name exists
	 * 
	 * @param name	The name to check
	 * @return	true if a switch of this name exists, false otherwise
	 */
	public static boolean checkForSwitch(String name) {
		return allSwitches.containsKey(name);
	}

	public Map<String,Object> freeze() {
		Map<String, Object> map = new HashMap<String, Object>();

		map.put("trigger", MiscUtil.unParseColourSpec(trigger));
		map.put("world", location.getWorldName());
		map.put("x", location.getX());
		map.put("y", location.getY());
		map.put("z", location.getZ());

		return map;
	}

	private static String makeUniqueName(String base) {
		int idx = 1;

		String s = String.format("%s-%d", base, idx);
		while (Switch.checkForSwitch(s)) {
			idx++;
			s = String.format("%s-%d", base, idx);
		}
		return s;
	}

	/**
	 * Mark a switch configuration as a deferred load - we do this if the world is not (yet)
	 * available.
	 * 
	 * @param view
	 * @param conf
	 */
	public static void deferLoading(SMSGlobalScrollableView view, ConfigurationSection conf) {
		conf.set("viewName", view.getName());
		String world = conf.getString("world");
		Set<ConfigurationSection> set = deferred.get(world);
		if (set == null) {
			set = new HashSet<ConfigurationSection>();
			deferred.put(conf.getString("world"), set);
		}
		set.add(conf);
	}

	/**
	 * Go ahead and try to load any deferred switches for the given world.   Called from the
	 * WorldLoadEvent handler.
	 * 
	 * @param worldName
	 */
	public static void loadDeferred(World world) {
		Set<ConfigurationSection> set = deferred.get(world.getName());
		if (set == null) {
			return;
		}

		for (ConfigurationSection conf : set) {
			String viewName = conf.getString("viewName");
			try {
				SMSView view = SMSView.getView(viewName);
				new Switch((SMSGlobalScrollableView)view, conf);
			} catch (SMSException e) {
				LogUtils.warning("Unknown view " + viewName + " while loading deferred switch?");
			} catch (IllegalArgumentException e) {
				// really shouldn't happen
				LogUtils.warning("Can't load  deferred switch for view " + viewName + ": " + e.getMessage());
			}
		}

		deferred.remove(world.getName());
	}

	@Override
	public int compareTo(Switch other) {
		return name.compareTo(other.getName());
	}
}
