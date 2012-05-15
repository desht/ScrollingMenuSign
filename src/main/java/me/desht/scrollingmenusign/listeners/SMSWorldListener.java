package me.desht.scrollingmenusign.listeners;

import java.util.List;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.util.SMSLogger;
import me.desht.scrollingmenusign.views.SMSView;
import me.desht.scrollingmenusign.views.redout.Switch;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.util.Vector;

public class SMSWorldListener implements Listener {
	@EventHandler
	public void onWorldLoad(WorldLoadEvent event) {
		// check if any view locations for this world need to be loaded
		for (SMSView view : SMSView.listViews()) {
			List<Vector> l = view.getDeferredLocations(event.getWorld().getName());	
			if (l == null) {
				continue;
			}
			
			for (Vector vec : l) {
				try {
					view.addLocation(new Location(event.getWorld(), vec.getBlockX(), vec.getBlockY(), vec.getBlockZ()));
					System.out.println("added loc " + event.getWorld().getName() + ", " + vec + " to view " + view.getName());
				} catch (SMSException e) {
					SMSLogger.warning("Can't add location " + event.getWorld().getName() + ", " + vec + " to view " + view.getName());
					SMSLogger.warning("  Exception message: " + e.getMessage());
				}
			}
			l.clear();
		}
		
		// also load any switches for the world
		Switch.loadDeferred(event.getWorld().getName());
	}
}
