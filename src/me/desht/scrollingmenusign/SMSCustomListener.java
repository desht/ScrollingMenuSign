package me.desht.scrollingmenusign;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.event.CustomEventListener;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;

import com.sycoprime.movecraft.Craft;
import com.sycoprime.movecraft.events.MoveCraftMoveEvent;
import com.sycoprime.movecraft.events.MoveCraftTurnEvent;

public class SMSCustomListener extends CustomEventListener implements Listener {
	private ScrollingMenuSign plugin;
	
	public SMSCustomListener(ScrollingMenuSign plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public void onCustomEvent(Event event) {
		if (event instanceof MoveCraftMoveEvent)
			onMoveCraftMoveEvent((MoveCraftMoveEvent)event);
		else if (event instanceof MoveCraftTurnEvent)
			onMoveCraftTurnEvent((MoveCraftTurnEvent) event);
	}
	
	public void onMoveCraftMoveEvent(MoveCraftMoveEvent event) {
		Craft c = event.getCraft();
		Vector v = event.getMovement();
		List<Location> locs = new ArrayList<Location>();
		for (Location l : plugin.getLocations().keySet()) {
			locs.add(l);
		}
		for (Location l : locs) {
			if (c.isIn(l.getBlockX(), l.getBlockY(), l.getBlockZ())) {
				plugin.debug("movecraft move event " + l);
				String menuName = plugin.getMenuNameAt(l);
				if (menuName != null) {
					plugin.debug("movecraft craft " + c.name + ", moved " + v + ",sign " + l + ", menu " + menuName);
					plugin.moveSign(menuName, l, v);
				}
			}
		}
	}

	public void onMoveCraftTurnEvent(MoveCraftTurnEvent event) {
		Craft c = event.getCraft();
		int d = event.getDegrees();
		for (Location l : plugin.getLocations().keySet()) {
			if (c.isCraftBlock(l.getBlockX(), l.getBlockY(), l.getBlockZ())) {
				String menuName = plugin.getMenuNameAt(l);
				if (menuName != null) {
					plugin.debug("movecraft craft " + c.name + " turned " + d + ", sign " + l + ", menu " + menuName);
				}
			}
		}
	}
}
