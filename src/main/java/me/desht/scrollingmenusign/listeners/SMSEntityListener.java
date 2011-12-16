package me.desht.scrollingmenusign.listeners;

import java.util.logging.Level;

import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.util.Debugger;
import me.desht.scrollingmenusign.util.MiscUtil;
import me.desht.scrollingmenusign.views.SMSView;

import org.bukkit.Location;
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
		boolean noExplode = plugin.getConfig().getBoolean("sms.no_explosions", false);
		for (Block b : event.blockList()) {
			Location loc = b.getLocation();
			SMSView view = SMSView.getViewForLocation(loc);
			if (view == null)
				continue;
			
			SMSMenu menu = view.getMenu();
			Debugger.getDebugger().debug("entity explode event @ " + MiscUtil.formatLocation(loc) + ", menu=" + menu.getName());
			if (noExplode) {
				MiscUtil.log(Level.INFO, "stopped an explosion to protect view @ " + MiscUtil.formatLocation(loc) + " (menu " + menu.getName() + ")");
				event.setCancelled(true);
				break;
			} else {
				MiscUtil.log(Level.INFO, "view @ " + MiscUtil.formatLocation(loc) + " (menu " + menu.getName() + ") was destroyed by an explosion.");
				view.deletePermanent();
			}
		}
	}
}
