package me.desht.scrollingmenusign;

import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityListener;

public class SMSEntityListener extends EntityListener {
	private ScrollingMenuSign plugin;
	
	SMSEntityListener(ScrollingMenuSign plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public void onEntityExplode(EntityExplodeEvent event) {
		if (event.isCancelled()) return;
		Boolean noExplode = plugin.getConfiguration().getBoolean("sms.no_explosions", false);
		for (Block b : event.blockList()) {
			if (b.getType() != Material.WALL_SIGN && b.getType() != Material.SIGN_POST)
				continue;
			String menuName = SMSMenu.getMenuNameAt(b.getLocation());
			if (menuName == null)
				continue;
			
			plugin.debug("entity explode event @ " + b.getLocation() + ", menu=" + menuName);
			Location loc = b.getLocation();
			if (noExplode) {
				SMSUtils.log(Level.INFO, "stopped an explosion to protect sign @ " + SMSUtils.formatLocation(loc) + " (menu " + menuName + ")");
				event.setCancelled(true);
				break;
			} else {
				try {
					SMSMenu menu = SMSMenu.getMenuAt(loc);
					menu.removeSign(loc);
				} catch (SMSException e) {
					SMSUtils.log(Level.WARNING, e.getMessage());
				}
			}
		}
	}
}
