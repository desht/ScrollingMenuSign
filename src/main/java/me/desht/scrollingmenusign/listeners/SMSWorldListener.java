package me.desht.scrollingmenusign.listeners;

import me.desht.scrollingmenusign.RedstoneControlSign;
import me.desht.scrollingmenusign.views.SMSView;
import me.desht.scrollingmenusign.views.redout.Switch;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

public class SMSWorldListener implements Listener {
	@EventHandler
	public void onWorldLoad(WorldLoadEvent event) {
		// load any view locations for this world
		SMSView.loadDeferred(event.getWorld());
		// load any switches for the world
		Switch.loadDeferred(event.getWorld());
		// load any control signs for the world
		RedstoneControlSign.loadDeferred(event.getWorld());
	}
}
