package me.desht.scrollingmenusign.views.redout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.material.Button;
import org.bukkit.material.Lever;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.util.MiscUtil;
import me.desht.scrollingmenusign.views.SMSGlobalScrollableView;

public class Switch {
	private static final Map<Location,Switch> allSwitches = new HashMap<Location,Switch>();
	
	private final SMSGlobalScrollableView view;
	private final Location location;
	private final String trigger;
	
	public Switch(SMSGlobalScrollableView view, String trigger, Location location) {
		this.view = view;
		this.location = location;
		this.trigger = trigger;
		
		allSwitches.put(location, this);
		view.addSwitch(this);
	}
	
	public Switch(SMSGlobalScrollableView view, ConfigurationSection conf) throws SMSException {
		String worldName = conf.getString("world");
		World w = Bukkit.getWorld(worldName);
		if (w == null) {
			// TODO: need to defer loading in case world is not yet loaded
			throw new IllegalArgumentException("no such world: " + worldName);
		}
		this.view = view;
		this.location = new Location(w, conf.getInt("x"), conf.getInt("y"), conf.getInt("z"));
		this.trigger = conf.getString("trigger");
		
		allSwitches.put(location, this);
		view.addSwitch(this);
	}
	
	public SMSGlobalScrollableView getView() {
		return view;
	}

	public Location getLocation() {
		return location;
	}

	public String getTrigger() {
		return trigger;
	}

	public void delete() {
		allSwitches.remove(location);
		view.removeSwitch(this);
	}
	
	public Material getSwitchType() {
		return location.getBlock().getType();
	}
	
	public void setPowered(boolean powered) {
		switch (getSwitchType()) {
		case LEVER:
			setLeverPowered(location.getBlock(), powered);
			break;
		case STONE_BUTTON:
			setButtonPowered(location.getBlock(), powered);
			break;
		default:
			MiscUtil.log(Level.WARNING, "Found " + getSwitchType() + " at " + location + " - expecting LEVER or STONE_BUTTON!");
			break;
		}
	}
	
	private void setButtonPowered(Block b, boolean powered) {
		Button button = (Button) b.getState().getData();
		button.setPowered(powered);
		b.setData(button.getData(), true);
	}

	private void setLeverPowered(Block b, boolean powered) {
		Lever lever = (Lever) b.getState().getData();
		lever.setPowered(powered);
		b.setData(lever.getData(), true);
	}

	public static Switch getSwitchAt(Location loc) {
		return allSwitches.get(loc);
	}
	
	public static List<Switch> getSwitches() {
		return new ArrayList<Switch>(allSwitches.values());
	}
	
	public Map<String,Object> freeze() {
		Map<String, Object> map = new HashMap<String, Object>();
			
		map.put("trigger", trigger);
		map.put("world", location.getWorld());
		map.put("x", location.getBlockX());
		map.put("y", location.getBlockY());
		map.put("z", location.getBlockZ());
		
		return map;
	}
}
