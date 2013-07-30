package me.desht.scrollingmenusign;

import java.util.HashMap;
import java.util.Map;

import me.desht.dhutils.PersistableLocation;

import org.bukkit.Location;

/**
 * Track the location of every block which is managed by SMS.
 */
public class LocationManager {
	private final Map<PersistableLocation, SMSInteractableBlock> locationMap;

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
		if (o != null && c.isAssignableFrom(o.getClass())) {
			return c.cast(o);
		} else {
			return null;
		}
	}
}
