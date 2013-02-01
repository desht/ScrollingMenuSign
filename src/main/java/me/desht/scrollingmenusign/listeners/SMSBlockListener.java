package me.desht.scrollingmenusign.listeners;

import me.desht.dhutils.DHUtilsException;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PermissionUtils;
import me.desht.scrollingmenusign.RedstoneControlSign;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.scrollingmenusign.expector.ExpectSwitchAddition;
import me.desht.scrollingmenusign.views.SMSGlobalScrollableView;
import me.desht.scrollingmenusign.views.SMSMapView;
import me.desht.scrollingmenusign.views.SMSRedstoneView;
import me.desht.scrollingmenusign.views.SMSView;
import me.desht.scrollingmenusign.views.redout.Switch;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.material.Attachable;

public class SMSBlockListener extends SMSListenerBase {

	public SMSBlockListener(ScrollingMenuSign plugin) {
		super(plugin);
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockDamage(BlockDamageEvent event) {
		Block b = event.getBlock();
		Location loc = b.getLocation();
		SMSView view = SMSView.getViewForLocation(loc);
		if (view == null)
			return;

		SMSMenu menu = view.getNativeMenu();
		LogUtils.fine("block damage event @ " + MiscUtil.formatLocation(loc) + ", view = " + view.getName() + ", menu=" + menu.getName());
		Player p = event.getPlayer();
		if (p.getName().equalsIgnoreCase(menu.getOwner()) || PermissionUtils.isAllowedTo(p, "scrollingmenusign.destroy")) 
			return;

		// don't allow destruction
		event.setCancelled(true);
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		Block b = event.getBlock();
		Player p = event.getPlayer();
		Location loc = b.getLocation();

		SMSView view = SMSView.getViewForLocation(loc);
		
		if (SMSMapView.getHeldMapView(p) != null) {
			// avoid breaking blocks while holding active map view (mainly for benefit of creative mode)
			event.setCancelled(true);
			if (view != null) view.update(view.getActiveMenu(p.getName()), SMSMenuAction.REPAINT);
			return;
		}

		if (view != null) {
			LogUtils.fine("block break event @ " + b.getLocation() + ", view = " + view.getName() + ", menu=" + view.getNativeMenu().getName());
			if (plugin.getConfig().getBoolean("sms.no_destroy_signs", false)) {
				event.setCancelled(true);
				view.update(view.getActiveMenu(p.getName()), SMSMenuAction.REPAINT);
			} else {
				view.removeLocation(loc);
				if (view.getLocations().size() == 0) {
					view.deletePermanent();
				}
				MiscUtil.statusMessage(p, String.format("%s block @ &f%s&- was removed from view &e%s&- (menu &e%s&-).", 
				                                        b.getType().toString(), MiscUtil.formatLocation(loc),
				                                        view.getName(), view.getNativeMenu().getName()));
			}
		} else if (Switch.getSwitchAt(loc) != null) {
			Switch sw = Switch.getSwitchAt(loc);
			sw.delete();
			MiscUtil.statusMessage(p, String.format("Output switch @ &f%s&- was removed from view &e%s / %s.",
			                                        MiscUtil.formatLocation(loc),
			                                        sw.getView().getName(), sw.getTrigger()));
		} else if (RedstoneControlSign.checkForSign(loc)) {
			RedstoneControlSign rcSign = RedstoneControlSign.getControlSign(loc);
			rcSign.delete();
			MiscUtil.statusMessage(p, String.format("Redstone control sign @ &f%s&- was removed from view &e%s&-.",
			                                        MiscUtil.formatLocation(loc), rcSign.getView().getName()));
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockPhysics(BlockPhysicsEvent event) {
		Block b = event.getBlock();
		Location loc = b.getLocation();

		SMSView view = SMSView.getViewForLocation(loc);
		if (view != null) {
			LogUtils.fine("block physics event @ " + loc + ", view = " + view.getName() + ", menu=" + view.getNativeMenu().getName());
			if (plugin.getConfig().getBoolean("sms.no_physics", false)) {
				event.setCancelled(true);
			} else if (b.getState().getData() instanceof Attachable) {
				Attachable a = (Attachable)	b.getState().getData();
				Block attachedBlock = b.getRelative(a.getAttachedFace());
				if (attachedBlock.getTypeId() == 0) {
					// attached to air? looks like the sign (or other attachable) has become detached
					LogUtils.info("Attachable view block " + view.getName() + " @ " + loc + " has become detached: deleting");
					view.deletePermanent();
				}
			}
		} else if (RedstoneControlSign.checkForSign(loc)) {
			RedstoneControlSign rcSign = RedstoneControlSign.getControlSign(loc);
			if (!rcSign.isAttached()) {
				rcSign.delete();
				LogUtils.info("Redstone control sign for " + rcSign.getView().getName() + " @ " + loc + " has become detached: deleting");
				return;
			}
			LogUtils.fine("block physics event @ " + b + " power=" + b.getBlockPower() + " prev-power=" + rcSign.getLastPowerLevel());
			if (b.getBlockPower() > 0 && b.getBlockPower() > rcSign.getLastPowerLevel()) {
				rcSign.processActions();
			}
			rcSign.setLastPowerLevel(b.getBlockPower());
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {
		Player p = event.getPlayer();
		if (plugin.responseHandler.isExpecting(p.getName(), ExpectSwitchAddition.class)) {
			ExpectSwitchAddition swa = plugin.responseHandler.getAction(p.getName(), ExpectSwitchAddition.class);
			swa.setLocation(event.getBlock().getLocation());
			try {
				swa.handleAction();
			} catch (DHUtilsException e) {
				MiscUtil.errorMessage(p, e.getMessage());
			}
		}
	}

	@EventHandler
	public void onSignChanged(SignChangeEvent event) {
		if (event.getLine(0).equals("[smsred]")) {
			final Player player = event.getPlayer();
			try {
				PermissionUtils.requirePerms(player, "scrollingmenusign.create.redstonecontrol");
				SMSView view = SMSView.getView(event.getLine(1));
				if (!(view instanceof SMSGlobalScrollableView)) {
					throw new SMSException(view.getName() + " must be a globally scrollable view");
				}
				event.setLine(0, ChatColor.RED + "[smsred]");
				final Block block = event.getBlock();
				
				Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
					// get the new control sign cached
					@Override
					public void run() {
						try {
							RedstoneControlSign.getControlSign(block.getLocation());
						} catch (SMSException e) {
							MiscUtil.errorMessage(player, e.getMessage());
						}
					}
				});
			} catch (SMSException e) {
				MiscUtil.errorMessage(player, e.getMessage());
			}
		}
	}

	@EventHandler
	public void onBlockRedstoneChange(BlockRedstoneEvent event) {
		Block b = event.getBlock();
		Location loc = b.getLocation();

		// redstone views
		SMSRedstoneView.processRedstoneEvent(event);

		// redstone control for sign views etc.
		if (RedstoneControlSign.checkForSign(b.getLocation())) {
			try {
				LogUtils.fine("redstone control: " + b + " current=" + event.getNewCurrent() + " power=" + b.getBlockPower());
				RedstoneControlSign rcSign = RedstoneControlSign.getControlSign(loc);
				rcSign.setLastPowerLevel(b.getBlockPower());
			} catch (SMSException e) {
				LogUtils.warning(e.getMessage());
				RedstoneControlSign.getSignAt(loc).delete();
			}
		}

	}
}
