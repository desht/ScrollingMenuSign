package me.desht.scrollingmenusign.listeners;

import java.util.logging.Level;

import me.desht.scrollingmenusign.SMSConfig;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.expector.ExpectSwitchAddition;
import me.desht.scrollingmenusign.util.Debugger;
import me.desht.scrollingmenusign.util.MiscUtil;
import me.desht.scrollingmenusign.util.PermissionsUtils;
import me.desht.scrollingmenusign.views.SMSMapView;
import me.desht.scrollingmenusign.views.SMSRedstoneView;
import me.desht.scrollingmenusign.views.SMSView;
import me.desht.scrollingmenusign.views.redout.Switch;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.material.Attachable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;

public class SMSBlockListener implements Listener {

	@EventHandler(ignoreCancelled = true)
	public void onBlockDamage(BlockDamageEvent event) {
		Block b = event.getBlock();
		Location loc = b.getLocation();
		SMSView view = SMSView.getViewForLocation(loc);
		if (view == null)
			return;

		SMSMenu menu = view.getMenu();
		Debugger.getDebugger().debug("block damage event @ " + MiscUtil.formatLocation(loc) + ", view = " + view.getName() + ", menu=" + menu.getName());
		Player p = event.getPlayer();
		if (p.getName().equalsIgnoreCase(menu.getOwner()) || PermissionsUtils.isAllowedTo(p, "scrollingmenusign.destroy")) 
			return;

		// don't allow destruction
		event.setCancelled(true);
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		Block b = event.getBlock();
		Player p = event.getPlayer();
		Location loc = b.getLocation();

		if (SMSMapView.getHeldMapView(p) != null) {
			// avoid breaking blocks while holding active map view (mainly for benefit of creative mode)
			event.setCancelled(true);
			return;
		}

		SMSView view = SMSView.getViewForLocation(loc);
		if (view != null) {
			Debugger.getDebugger().debug("block break event @ " + b.getLocation() + ", view = " + view.getName() + ", menu=" + view.getMenu().getName());
			if (SMSConfig.getConfig().getBoolean("sms.no_destroy_signs", false)) {
				event.setCancelled(true);
			} else {
				view.removeLocation(loc);
				if (view.getLocations().size() == 0) {
					view.deletePermanent();
				}
				MiscUtil.statusMessage(p, String.format("%s block @ &f%s&- was removed from view &e%s&- (menu &e%s&-).", 
				                                        b.getType().toString(), MiscUtil.formatLocation(loc),
				                                        view.getName(), view.getMenu().getName()));
			}
		} else if (Switch.getSwitchAt(loc) != null) {
			Switch sw = Switch.getSwitchAt(loc);
			sw.delete();
			MiscUtil.statusMessage(p, String.format("Output switch @ &f%s&- was removed from view &e%s / %s.",
			                                        MiscUtil.formatLocation(loc),
			                                        sw.getView().getName(), sw.getTrigger()));
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockPhysics(BlockPhysicsEvent event) {
		Block b = event.getBlock();
		Location loc = b.getLocation();

		SMSView view = SMSView.getViewForLocation(loc);
		if (view != null) {
			Debugger.getDebugger().debug("block physics event @ " + loc + ", view = " + view.getName() + ", menu=" + view.getMenu().getName());
			if (SMSConfig.getConfig().getBoolean("sms.no_physics", false)) {
				event.setCancelled(true);
			} else if (b.getState().getData() instanceof Attachable) {
				Attachable a = (Attachable)	b.getState().getData();
				Block attachedBlock = b.getRelative(a.getAttachedFace());
				if (attachedBlock.getTypeId() == 0) {
					// attached to air? looks like the sign (or other attachable) has become detached
					MiscUtil.log(Level.INFO, "Attachable view block " + view.getName() + " @ " + loc + " has become detached: deleting");
					view.deletePermanent();
				}
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {
		Player p = event.getPlayer();
		ScrollingMenuSign plugin = ScrollingMenuSign.getInstance();
		if (plugin.responseHandler.isExpecting(p, ExpectSwitchAddition.class)) {
			ExpectSwitchAddition swa = (ExpectSwitchAddition) plugin.responseHandler.getAction(p, ExpectSwitchAddition.class);
			swa.setLocation(event.getBlock().getLocation());
			try {
				plugin.responseHandler.handleAction(p, ExpectSwitchAddition.class);
			} catch (SMSException e) {
				MiscUtil.errorMessage(p, e.getMessage());
			}
		}
	}
	
	@EventHandler
	public void onBlockRedstoneChange(BlockRedstoneEvent event) {
		SMSRedstoneView.processRedstoneEvent(event);
	}
}
