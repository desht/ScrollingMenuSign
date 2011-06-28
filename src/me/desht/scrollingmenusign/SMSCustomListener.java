package me.desht.scrollingmenusign;

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
		for (Location l : plugin.getLocations().keySet()) {
			if (c.isCraftBlock(l.getBlockX(), l.getBlockY(), l.getBlockZ())) {
				String menuName = plugin.getMenuNameAt(l);
				if (menuName != null) {
					plugin.debug("movecraft craft " + c.name + ", moved " + v + ",sign " + l + ", menu " + menuName);			
				}
			}
		}
	}

	public void onMoveCraftTurnEvent(MoveCraftTurnEvent event) {
		Craft c = event.getCraft();
		int d = event.getDegrees();
		plugin.debug("movecraft craft " + c.name + " turned " + d);
	}
}
