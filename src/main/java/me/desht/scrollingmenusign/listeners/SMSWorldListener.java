package me.desht.scrollingmenusign.listeners;

import java.util.List;
import java.util.logging.Level;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.util.MiscUtil;
import me.desht.scrollingmenusign.views.SMSView;

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
					MiscUtil.log(Level.WARNING, "Can't add location " + event.getWorld().getName() + ", " + vec + " to view " + view.getName());
					MiscUtil.log(Level.WARNING, "  Exception message: " + e.getMessage());
				}
			}
			l.clear();
		}
	}
}
