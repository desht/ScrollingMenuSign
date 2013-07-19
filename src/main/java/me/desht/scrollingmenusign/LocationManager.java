package me.desht.scrollingmenusign;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;

import me.desht.dhutils.PersistableLocation;

public class LocationManager {
	private Map<PersistableLocation, SMSInteractableBlock> locationMap;
	
	public LocationManager() {
		locationMap = new HashMap<PersistableLocation, SMSInteractableBlock>();
	}

	public void registerLocation(Location loc, SMSInteractableBlock interactable) {
		locationMap.put(new PersistableLocation(loc), interactable);
	}

	public void unregisterLocation(Location loc) {
		locationMap.remove(new PersistableLocation(loc));
	}

	public SMSInteractableBlock getInteractableAt(Location loc) {
		return locationMap.get(new PersistableLocation(loc));
	}

	public <T> T getInteractableAt(Location loc, Class<T> c) {
		Object o = locationMap.get(new PersistableLocation(loc));
		if (c.isAssignableFrom(o.getClass())) {
			return c.cast(o);
		} else {
			return null;
		}
	}
}
