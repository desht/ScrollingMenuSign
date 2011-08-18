package me.desht.scrollingmenusign.listeners;

import java.util.logging.Level;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSHandler;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.util.MiscUtil;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityListener;

public class SMSEntityListener extends EntityListener {
	private ScrollingMenuSign plugin;
	
	public SMSEntityListener(ScrollingMenuSign plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public void onEntityExplode(EntityExplodeEvent event) {
		if (event.isCancelled()) return;
		Boolean noExplode = plugin.getConfiguration().getBoolean("sms.no_explosions", false);
		SMSHandler handler = plugin.getHandler();
		for (Block b : event.blockList()) {
			if (b.getType() != Material.WALL_SIGN && b.getType() != Material.SIGN_POST)
				continue;
			String menuName = handler.getMenuNameAt(b.getLocation());
			if (menuName == null)
				continue;
			
			plugin.debug("entity explode event @ " + b.getLocation() + ", menu=" + menuName);
			Location loc = b.getLocation();
			if (noExplode) {
				MiscUtil.log(Level.INFO, "stopped an explosion to protect sign @ " + MiscUtil.formatLocation(loc) + " (menu " + menuName + ")");
				event.setCancelled(true);
				break;
			} else {
				try {
					SMSMenu menu = handler.getMenuAt(loc);
					menu.removeSign(loc);
				} catch (SMSException e) {
					MiscUtil.log(Level.WARNING, e.getMessage());
				}
			}
		}
	}
}
