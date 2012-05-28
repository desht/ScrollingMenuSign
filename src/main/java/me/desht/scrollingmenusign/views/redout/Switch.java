package me.desht.scrollingmenusign.views.redout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import me.desht.scrollingmenusign.views.SMSGlobalScrollableView;
import me.desht.scrollingmenusign.views.SMSView;

public class Switch {
	private static final Map<Location,Switch> allSwitchLocs = new HashMap<Location,Switch>();
	private static final Map<String, Switch> allSwitches = new HashMap<String,Switch>();

	private static Map<String, Set<ConfigurationSection>> deferred = new HashMap<String, Set<ConfigurationSection>>();

	private final SMSGlobalScrollableView view;
	private final Location location;
	private final String trigger;
	private final String name;

	public Switch(SMSGlobalScrollableView view, String trigger, Location location) {
		this.view = view;
		this.location = location;
		this.trigger = trigger;
		this.name = makeUniqueName(view.getName());

		initCommon();
	}

	public Switch(SMSGlobalScrollableView view, ConfigurationSection conf) throws SMSException {
		String worldName = conf.getString("world");
		World w = Bukkit.getWorld(worldName);
		if (w == null) {
			// throw an exception - the view thawing code can defer the loading
			throw new IllegalArgumentException("World not available");
		} 
		this.view = view;
		this.location = new Location(w, conf.getInt("x"), conf.getInt("y"), conf.getInt("z"));
		this.trigger = conf.getString("trigger");
		this.name = makeUniqueName(view.getName());

		initCommon();
	}

	private void initCommon() {
		allSwitches.put(name, this);
		allSwitchLocs.put(location, this);
		view.addSwitch(this);
	}

	public SMSGlobalScrollableView getView() {
		return view;
	}

	public String getName() {
		return name;
	}

	public Location getLocation() {
		return location;
	}

	public String getTrigger() {
		return trigger;
	}

	public void delete() {
		allSwitches.remove(name);
		allSwitchLocs.remove(location);
		view.removeSwitch(this);
	}

	private Material getSwitchType() {
		return location.getBlock().getType();
	}

	public boolean getPowered() {
		Block b = location.getBlock();
		if (getSwitchType() == Material.LEVER) {
			return ((Redstone)b.getState().getData()).isPowered();
		} else {
			LogUtils.warning("Found " + getSwitchType() + " at " + location + " - expecting LEVER!");
			return false;
		}
	}

	public void setPowered(boolean powered) {
		if (getSwitchType() == Material.LEVER) {
			setLeverPowered(location.getBlock(), powered);
		} else {
			LogUtils.warning("Found " + getSwitchType() + " at " + location + " - expecting LEVER!");
		}
	}

	private void setLeverPowered(Block b, boolean powered) {
		Lever lever = (Lever) b.getState().getData();
		lever.setPowered(powered);
		b.setData(lever.getData(), true);
	}

	public static Switch getSwitch(String name) {
		return allSwitches.get(name);
	}

	public static Switch getSwitchAt(Location loc) {
		return allSwitchLocs.get(loc);
	}

	public static List<Switch> getSwitches() {
		return new ArrayList<Switch>(allSwitchLocs.values());
	}

	public static boolean checkForSwitch(String s) {
		return allSwitches.containsKey(s);
	}

	public Map<String,Object> freeze() {
		Map<String, Object> map = new HashMap<String, Object>();

		map.put("trigger", trigger);
		map.put("world", location.getWorld().getName());
		map.put("x", location.getBlockX());
		map.put("y", location.getBlockY());
		map.put("z", location.getBlockZ());

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
}
