package me.desht.scrollingmenusign.listeners;

import java.util.Iterator;

import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.enums.SMSUserAction;
import me.desht.scrollingmenusign.views.SMSMapView;
import me.desht.scrollingmenusign.views.SMSView;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.inventory.ItemStack;

public class SMSEntityListener extends SMSListenerBase {

	public SMSEntityListener(ScrollingMenuSign plugin) {
		super(plugin);
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityExplode(EntityExplodeEvent event) {
		boolean noExplode = plugin.getConfig().getBoolean("sms.no_explosions", false);

		Iterator<Block> iter = event.blockList().iterator();
		while (iter.hasNext()) {
			Location loc = iter.next().getLocation();
			SMSView view = plugin.getViewManager().getViewForLocation(loc);
			if (view == null)
				continue;

			SMSMenu menu = view.getNativeMenu();
			LogUtils.fine("entity explode event @ " + MiscUtil.formatLocation(loc) + ", menu=" + menu.getName());
			if (noExplode) {
				LogUtils.info("view @ " + MiscUtil.formatLocation(loc) + " (menu " + menu.getName() + ") was protected from an explosion.");
				iter.remove();
			} else {
				LogUtils.info("view @ " + MiscUtil.formatLocation(loc) + " (menu " + menu.getName() + ") was destroyed by an explosion.");
				plugin.getViewManager().deleteView(view, true);
			}
		}
	}

	@EventHandler
	public void onItemFrameLeftClicked(HangingBreakByEntityEvent event) {
		// CB 1.6.4 and older - item frame auto-breaks even with an item in it
		Entity entity = event.getEntity();
		if (entity instanceof ItemFrame && event.getRemover() instanceof Player && event.getCause() == HangingBreakEvent.RemoveCause.ENTITY) {
			ItemStack item = ((ItemFrame) entity).getItem();
			if (item != null && item.getType() == Material.MAP && plugin.getViewManager().checkForMapId(item.getDurability())) {
				SMSUserAction action = SMSUserAction.getAction(event);
				SMSMapView mapView = plugin.getViewManager().getMapViewForId(item.getDurability());
				Player player = (Player) event.getRemover();
				try {
					action.execute(player, mapView);
				} catch (SMSException e) {
					MiscUtil.errorMessage(player, e.getMessage());
				}
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onItemFrameDamagedByEntity(EntityDamageByEntityEvent event) {
		// CB 1.7.2+ - item frame with an item in it doesn't auto-break - instead a damage event is fired
		Entity entity = event.getEntity();
		if (entity instanceof ItemFrame) {
			ItemStack item = ((ItemFrame) entity).getItem();
			if (item != null && item.getType() == Material.MAP && plugin.getViewManager().checkForMapId(item.getDurability())) {
				if (event.getDamager() instanceof Player && event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
					SMSUserAction action = SMSUserAction.getAction(event);
					SMSMapView mapView = plugin.getViewManager().getMapViewForId(item.getDurability());
					Player player = (Player) event.getDamager();
					try {
						action.execute(player, mapView);
					} catch (SMSException e) {
						MiscUtil.errorMessage(player, e.getMessage());
					}
					event.setCancelled(true);
				} else {
					if (plugin.getConfig().getBoolean("sms.no_itemframe_damage")) {
						event.setCancelled(true);
					}
				}
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onItemFrameDamagedByBlock(EntityDamageByBlockEvent event) {
		Entity entity = event.getEntity();
		if (entity instanceof ItemFrame) {
			ItemStack item = ((ItemFrame) entity).getItem();
			if (item != null && item.getType() == Material.MAP && plugin.getViewManager().checkForMapId(item.getDurability())) {
				if (plugin.getConfig().getBoolean("sms.no_itemframe_damage")) {
					event.setCancelled(true);
				}
			}
		}
	}
}
