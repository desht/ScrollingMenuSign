package me.desht.scrollingmenusign;

import java.util.logging.Level;

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
		for (Block b : event.blockList()) {
			if (b.getType() != Material.WALL_SIGN && b.getType() != Material.SIGN_POST) continue;
			String menuName = SMSMenu.getMenuNameAt(b.getLocation());
			if (menuName == null) continue;
			plugin.debug("entity explode event @ " + b.getLocation() + ", menu=" + menuName);
			if (noExplode) {
				Location l = b.getLocation();
				SMSUtils.log(Level.INFO, "stopped an explosion to protect sign @ " +
					l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ() + "," +
					l.getWorld().getName() + " (menu " + menuName + ")");
				event.setCancelled(true);
				break;
			} else {
				try {
					SMSMenu.removeSignFromMenu(b.getLocation(), ScrollingMenuSign.MenuRemoveAction.DO_NOTHING);
					plugin.maybeSaveMenus();
				} catch (SMSNoSuchMenuException e) {
					SMSUtils.log(Level.WARNING, e.getMessage());
				}
			}
		}
	}
}
