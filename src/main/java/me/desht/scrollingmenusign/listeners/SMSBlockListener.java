package me.desht.scrollingmenusign.listeners;

import me.desht.dhutils.BlockFaceUtil;
import me.desht.dhutils.DHUtilsException;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PermissionUtils;
import me.desht.scrollingmenusign.RedstoneControlSign;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSHandler;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSValidate;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.scrollingmenusign.expector.ExpectSwitchAddition;
import me.desht.scrollingmenusign.views.SMSGlobalScrollableView;
import me.desht.scrollingmenusign.views.SMSMapView;
import me.desht.scrollingmenusign.views.SMSRedstoneView;
import me.desht.scrollingmenusign.views.SMSSignView;
import me.desht.scrollingmenusign.views.SMSView;
import me.desht.scrollingmenusign.views.redout.Switch;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.material.Attachable;
import org.bukkit.material.Sign;

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
		Player player = event.getPlayer();
		if (plugin.getConfig().getBoolean("sms.no_destroy_signs") ||
			!menu.isOwnedBy(player) && !PermissionUtils.isAllowedTo(player, "scrollingmenusign.edit.any")) {
			event.setCancelled(true);
		}
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
			if (view != null) {
				view.update(view.getActiveMenu(p.getName()), SMSMenuAction.REPAINT);
			}
		} else if (view != null) {
			LogUtils.fine("block break event @ " + b.getLocation() + ", view = " + view.getName() + ", menu=" + view.getNativeMenu().getName());
			if (plugin.getConfig().getBoolean("sms.no_destroy_signs")) {
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
		} else if (SMSGlobalScrollableView.getViewForTooltipLocation(loc) != null) {
			SMSGlobalScrollableView gsv = SMSGlobalScrollableView.getViewForTooltipLocation(loc);
			gsv.removeTooltipSign();
			MiscUtil.statusMessage(p, String.format("Tooltip sign @ &f%s&- was removed from view &e%s&-.",
			                                        MiscUtil.formatLocation(loc), gsv.getName()));
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
			} else if (isAttachableDetached(b)) {
				// attached to air? looks like the sign (or other attachable) has become detached
				LogUtils.info("Attachable view block " + view.getName() + " @ " + loc + " has become detached: deleting");
				view.deletePermanent();
			}
		} else if (RedstoneControlSign.checkForSign(loc)) {
			RedstoneControlSign rcSign = RedstoneControlSign.getControlSign(loc);
			if (!rcSign.isAttached()) {
				rcSign.delete();
				LogUtils.info("Redstone control sign for " + rcSign.getView().getName() + " @ " + loc + " has become detached: deleting");
			} else {
				LogUtils.fine("block physics event @ " + b + " power=" + b.getBlockPower() + " prev-power=" + rcSign.getLastPowerLevel());
				if (b.getBlockPower() > 0 && b.getBlockPower() > rcSign.getLastPowerLevel()) {
					rcSign.processActions();
				}
				rcSign.setLastPowerLevel(b.getBlockPower());
			}
		} else if (SMSGlobalScrollableView.getViewForTooltipLocation(loc) != null) {
			if (isAttachableDetached(b)) {
				SMSGlobalScrollableView gsv = SMSGlobalScrollableView.getViewForTooltipLocation(loc);
				LogUtils.info("Tooltip sign for " + gsv.getName() + " @ " + loc + " has become detached: deleting");
				gsv.removeTooltipSign();
			}
		}
	}

	private boolean isAttachableDetached(Block b) {
		BlockState bs = b.getState();
		if (bs instanceof Attachable) {
			Attachable a = (Attachable)	b.getState().getData();
			Block attachedBlock = b.getRelative(a.getAttachedFace());
			return !attachedBlock.getType().isSolid();
		} else {
			return false;
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
		try {
			if (event.getLine(0).equals("[sms]")) {
				tryToActivateSign(event);
			} else if (event.getLine(0).equals("[smsred]")) {
				placeRedstoneControlSign(event);
			} else if (event.getLine(0).equals("[smstooltip]")) {
				placeTooltipSign(event);
			}
		} catch (SMSException e) {
			MiscUtil.errorMessage(event.getPlayer(), e.getMessage());
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

	private void placeRedstoneControlSign(SignChangeEvent event) {
		final Player player = event.getPlayer();

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
	}

	private void placeTooltipSign(SignChangeEvent event) {
		Player player = event.getPlayer();

		PermissionUtils.requirePerms(player, "scrollingmenusign.create.tooltipsign");

		// check up, down, left, right for a global scrollable view
		Block b = event.getBlock();
		BlockState bs = b.getState();
		Sign sign = (Sign) bs.getData();
		BlockFace face = sign.getAttachedFace();
		BlockFace left = BlockFaceUtil.getLeft(face);
		if (!checkForGSView(b, BlockFace.UP) &&	!checkForGSView(b, left) && 
				!checkForGSView(b, BlockFace.DOWN) && !checkForGSView(b, left.getOppositeFace())) {
			throw new SMSException("Tooltip signs must be placed next to a ScrollingMenuSign view");
		}
		SMSGlobalScrollableView gsv = SMSGlobalScrollableView.getViewForTooltipLocation(b.getLocation());
		String[] text = gsv.getTooltipText();
		for (int i = 0; i < 4; i++) {
			event.setLine(i, text[i]);
		}
		MiscUtil.statusMessage(event.getPlayer(), String.format("Tooltip sign @ &f%s&- has been added to view &e%s&-.",
		                                                        MiscUtil.formatLocation(b.getLocation()), gsv.getName()));
	}

	private boolean checkForGSView(Block b, BlockFace face) {
		Location viewLoc = b.getRelative(face).getLocation();
		SMSView view = SMSView.getViewForLocation(viewLoc);
		if (view == null || !(view instanceof SMSGlobalScrollableView)) {
			return false;
		}
		SMSGlobalScrollableView gsv = (SMSGlobalScrollableView) view;
		if (gsv.getTooltipSign() != null) {
			return false;
		}
		gsv.addTooltipSign(b.getLocation());
		return true;
	}

	private void tryToActivateSign(SignChangeEvent event) {
		final Block b = event.getBlock();
		final Player player = event.getPlayer();
		String menuName = event.getLine(1);
		SMSValidate.isFalse(menuName.isEmpty(), "Missing menu name on line 2.");
		String title = MiscUtil.parseColourSpec(player, event.getLine(2));

		SMSHandler handler = plugin.getHandler();
		SMSMenu menu = null;
		if (handler.checkMenu(menuName)) {
			PermissionUtils.requirePerms(player, "scrollingmenusign.commands.sync");
			menu = handler.getMenu(menuName);
		} else if (title.length() > 0) {
			PermissionUtils.requirePerms(player, "scrollingmenusign.commands.create");
			menu = handler.createMenu(menuName, title, player.getName());
		} else {
			throw new SMSException("No such menu '" + menuName + "'.");
		}

		// using the scheduler here because updating the sign from its SignChangeEvent handler doesn't work 
		final SMSMenu menu2 = menu;
		Bukkit.getScheduler().runTask(plugin, new Runnable() {
			@Override
			public void run() {
				SMSView view = SMSSignView.addSignToMenu(menu2, b.getLocation(), player);
				MiscUtil.statusMessage(player, String.format("Added new sign view &e%s&- @ &f%s&- to menu &e%s&-.",
				                                             view.getName(), MiscUtil.formatLocation(b.getLocation()), menu2.getName()));
			}
		});
	}
}
