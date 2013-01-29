package me.desht.scrollingmenusign.listeners;

import java.util.Iterator;

import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.views.SMSView;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityExplodeEvent;

public class SMSEntityListener extends SMSListenerBase {
	
	public SMSEntityListener(ScrollingMenuSign plugin) {
		super(plugin);
	}

	@EventHandler
	public void onEntityExplode(EntityExplodeEvent event) {
		if (event.isCancelled()) return;
		boolean noExplode = plugin.getConfig().getBoolean("sms.no_explosions", false);
		Iterator<Block>	iter = event.blockList().iterator();
		while (iter.hasNext()) {
			Location loc = iter.next().getLocation();
			SMSView view = SMSView.getViewForLocation(loc);
			if (view == null)
				continue;
			
			SMSMenu menu = view.getNativeMenu();
			LogUtils.fine("entity explode event @ " + MiscUtil.formatLocation(loc) + ", menu=" + menu.getName());
			if (noExplode) {
				LogUtils.info("view @ " + MiscUtil.formatLocation(loc) + " (menu " + menu.getName() + ") was protected from an explosion.");
				iter.remove();
			} else {
				LogUtils.info("view @ " + MiscUtil.formatLocation(loc) + " (menu " + menu.getName() + ") was destroyed by an explosion.");
				view.deletePermanent();
			}
		}
	}
}
