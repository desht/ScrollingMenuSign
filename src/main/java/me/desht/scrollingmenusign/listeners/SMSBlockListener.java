package me.desht.scrollingmenusign.listeners;

import me.desht.dhutils.BlockFaceUtil;
import me.desht.dhutils.DHUtilsException;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PermissionUtils;
import me.desht.scrollingmenusign.RedstoneControlSign;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSHandler;
import me.desht.scrollingmenusign.SMSInteractableBlock;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSValidate;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.scrollingmenusign.expector.ExpectSwitchAddition;
import me.desht.scrollingmenusign.views.SMSGlobalScrollableView;
import me.desht.scrollingmenusign.views.SMSRedstoneView;
import me.desht.scrollingmenusign.views.SMSView;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.material.Sign;

public class SMSBlockListener extends SMSListenerBase {

	public SMSBlockListener(ScrollingMenuSign plugin) {
		super(plugin);
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockDamage(BlockDamageEvent event) {
		Block b = event.getBlock();
		Location loc = b.getLocation();
		SMSView view = plugin.getViewManager().getViewForLocation(loc);
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

		SMSView view = plugin.getViewManager().getViewForLocation(loc);
		SMSInteractableBlock iBlock = plugin.getLocationManager().getInteractableAt(loc);

		if (plugin.getViewManager().getHeldMapView(p) != null) {
			// avoid breaking blocks while holding active map view (mainly for benefit of creative mode)
			event.setCancelled(true);
			if (view != null) {
				view.update(view.getActiveMenu(p.getName()), SMSMenuAction.REPAINT);
			}
		} else if (iBlock != null) {
			iBlock.processEvent(plugin, event);
		} else if (view != null) {
			LogUtils.fine("block break event @ " + b.getLocation() + ", view = " + view.getName() + ", menu=" + view.getNativeMenu().getName());
			if (plugin.getConfig().getBoolean("sms.no_destroy_signs")) {
				event.setCancelled(true);
				view.update(view.getActiveMenu(p.getName()), SMSMenuAction.REPAINT);
			} else {
				view.removeLocation(loc);
				if (view.getLocations().size() == 0) {
					plugin.getViewManager().deleteView(view, true);
					//					view.deletePermanent();
				}
				MiscUtil.statusMessage(p, String.format("%s block @ &f%s&- was removed from view &e%s&- (menu &e%s&-).", 
				                                        b.getType().toString(), MiscUtil.formatLocation(loc),
				                                        view.getName(), view.getNativeMenu().getName()));
			}
//		} else if (RedstoneControlSign.checkForSign(loc)) {
//			RedstoneControlSign rcSign = RedstoneControlSign.getControlSign(loc);
//			rcSign.delete();
//			MiscUtil.statusMessage(p, String.format("Redstone control sign @ &f%s&- was removed from view &e%s&-.",
//			                                        MiscUtil.formatLocation(loc), rcSign.getView().getName()));
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

		SMSView view = plugin.getViewManager().getViewForLocation(loc);
		SMSInteractableBlock iBlock = plugin.getLocationManager().getInteractableAt(loc);

		if (iBlock != null) {
			iBlock.processEvent(plugin, event);
		} else if (view != null) {
			LogUtils.fine("block physics event @ " + loc + ", view = " + view.getName() + ", menu=" + view.getNativeMenu().getName());
			if (plugin.getConfig().getBoolean("sms.no_physics", false)) {
				event.setCancelled(true);
			} else if (plugin.isAttachableDetached(b)) {
				// attached to air? looks like the sign (or other attachable) has become detached
				LogUtils.info("Attachable view block " + view.getName() + " @ " + loc + " has become detached: deleting");
				plugin.getViewManager().deleteView(view, true);
				//				view.deletePermanent();
			}
//		} else if (RedstoneControlSign.checkForSign(loc)) {
//			RedstoneControlSign rcSign = RedstoneControlSign.getControlSign(loc);
//			if (!rcSign.isAttached()) {
//				rcSign.delete();
//				LogUtils.info("Redstone control sign for " + rcSign.getView().getName() + " @ " + loc + " has become detached: deleting");
//			} else {
//				LogUtils.fine("block physics event @ " + b + " power=" + b.getBlockPower() + " prev-power=" + rcSign.getLastPowerLevel());
//				if (b.getBlockPower() > 0 && b.getBlockPower() > rcSign.getLastPowerLevel()) {
//					rcSign.processActions();
//				}
//				rcSign.setLastPowerLevel(b.getBlockPower());
//			}
		} else if (SMSGlobalScrollableView.getViewForTooltipLocation(loc) != null) {
			if (plugin.isAttachableDetached(b)) {
				SMSGlobalScrollableView gsv = SMSGlobalScrollableView.getViewForTooltipLocation(loc);
				LogUtils.info("Tooltip sign for " + gsv.getName() + " @ " + loc + " has become detached: deleting");
				gsv.removeTooltipSign();
			}
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
//		Block b = event.getBlock();
//		Location loc = b.getLocation();
		SMSInteractableBlock iBlock = plugin.getLocationManager().getInteractableAt(event.getBlock().getLocation());

		// redstone views
		SMSRedstoneView.processRedstoneEvent(event);

		if (iBlock != null) {
			iBlock.processEvent(plugin, event);
		} 
//		else if (RedstoneControlSign.checkForSign(b.getLocation())) {
//			try {
//				LogUtils.fine("redstone control: " + b + " current=" + event.getNewCurrent() + " power=" + b.getBlockPower());
//				RedstoneControlSign rcSign = RedstoneControlSign.getControlSign(loc);
//				rcSign.setLastPowerLevel(b.getBlockPower());
//			} catch (SMSException e) {
//				LogUtils.warning(e.getMessage());
//				RedstoneControlSign.getSignAt(loc).delete();
//			}
//		}
	}

	private void placeRedstoneControlSign(SignChangeEvent event) {
		final Player player = event.getPlayer();

		PermissionUtils.requirePerms(player, "scrollingmenusign.create.redstonecontrol");
		SMSView view = plugin.getViewManager().getView(event.getLine(1));
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
		Sign sign = new Sign(b.getType(), b.getData());
		BlockFace left = BlockFaceUtil.getLeft(sign.getAttachedFace());
		BlockFace toCheck = null;
		if (!event.getLine(1).isEmpty()) {
			switch (Character.toLowerCase(event.getLine(1).charAt(0))) {
			case 'u': toCheck = BlockFace.UP; break;
			case 'd': toCheck = BlockFace.DOWN; break;
			case 'l': toCheck = left; break;
			case 'r': toCheck = left.getOppositeFace(); break;
			default: throw new SMSException("Invalid direction [" + event.getLine(1) + "] (want one of U,D,L,R)");
			}
		}
		SMSGlobalScrollableView gsv;
		if (toCheck != null) {
			gsv = checkForGSView(b, toCheck);
		} else {
			gsv = checkForGSView(b, BlockFace.UP, left, BlockFace.DOWN, left.getOppositeFace());
		}
		SMSValidate.notNull(gsv, "Sorry, couldn't find a suitable view for this tooltip sign.");
		gsv.addTooltipSign(b.getLocation());
		String[] text = gsv.getTooltipText();
		for (int i = 0; i < 4; i++) {
			event.setLine(i, text[i]);
		}
		MiscUtil.statusMessage(event.getPlayer(), String.format("Tooltip sign @ &f%s&- has been added to view &e%s&-.",
		                                                        MiscUtil.formatLocation(b.getLocation()), gsv.getName()));
	}

	private SMSGlobalScrollableView checkForGSView(Block b, BlockFace... faces) {
		for (BlockFace face: faces) {
			Location viewLoc = b.getRelative(face).getLocation();
			SMSView view = plugin.getViewManager().getViewForLocation(viewLoc);
			if (view == null || !(view instanceof SMSGlobalScrollableView)) {
				continue;
			}
			SMSGlobalScrollableView gsv = (SMSGlobalScrollableView) view;
			if (gsv.getTooltipSign() != null) {
				continue;
			}
			return gsv;
		}
		return null;
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
				SMSView view = plugin.getViewManager().addSignToMenu(menu2, b.getLocation(), player);
				MiscUtil.statusMessage(player, String.format("Added new sign view &e%s&- @ &f%s&- to menu &e%s&-.",
				                                             view.getName(), MiscUtil.formatLocation(b.getLocation()), menu2.getName()));
			}
		});
	}
}
