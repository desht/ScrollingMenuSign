package me.desht.scrollingmenusign.listeners;

import java.util.logging.Level;

import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.util.Debugger;
import me.desht.scrollingmenusign.util.MiscUtil;
import me.desht.scrollingmenusign.util.PermissionsUtils;
import me.desht.scrollingmenusign.views.SMSMapView;
import me.desht.scrollingmenusign.views.SMSRedstoneView;
import me.desht.scrollingmenusign.views.SMSView;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.material.Attachable;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockRedstoneEvent;

public class SMSBlockListener extends BlockListener {

	private ScrollingMenuSign plugin;

	public SMSBlockListener(ScrollingMenuSign plugin) {
		this.plugin = plugin;
	}

	@Override
	public void onBlockDamage(BlockDamageEvent event) {
		if (event.isCancelled())
			return;

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

	@Override
	public void onBlockBreak(BlockBreakEvent event) {
		if (event.isCancelled())
			return;

		Block b = event.getBlock();
		Player p = event.getPlayer();
		Location loc = b.getLocation();

		SMSView view = SMSView.getViewForLocation(loc);
		if (view != null) {
			Debugger.getDebugger().debug("block break event @ " + b.getLocation() + ", view = " + view.getName() + ", menu=" + view.getMenu().getName());
			if (plugin.getConfig().getBoolean("sms.no_destroy_signs", false)) {
				event.setCancelled(true);
			} else if (p.getItemInHand().getType() == Material.MAP) {
				if (SMSMapView.getViewForId(p.getItemInHand().getDurability()) != null) {
					// avoid breaking blocks while holding active map view (mainly for benefit of creative mode)
					event.setCancelled(true);
				}
			} else {
				view.removeLocation(loc);
				if (view.getLocations().size() == 0) {
					view.deletePermanent();
				}
				MiscUtil.statusMessage(p, String.format("%s block @ &f%s&- was removed from view &e%s&- (menu &e%s&-).", 
				                                        b.getType().toString(), MiscUtil.formatLocation(loc),
				                                        view.getName(), view.getMenu().getName()));
			}
		}
	}

	@Override
	public void onBlockPhysics(BlockPhysicsEvent event) {
		if (event.isCancelled())
			return;

		Block b = event.getBlock();
		Location loc = b.getLocation();

		SMSView view = SMSView.getViewForLocation(loc);
		if (view != null) {
			Debugger.getDebugger().debug("block physics event @ " + loc + ", view = " + view.getName() + ", menu=" + view.getMenu().getName());
			if (plugin.getConfig().getBoolean("sms.no_physics", false)) {
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

	@Override
	public void onBlockRedstoneChange(BlockRedstoneEvent event) {
		SMSRedstoneView.processRedstoneEvent(event);
	}
}
