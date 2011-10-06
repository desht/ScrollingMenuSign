package me.desht.scrollingmenusign.listeners;

import java.util.logging.Level;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSHandler;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.enums.SMSUserAction;
import me.desht.scrollingmenusign.views.SMSMapView;
import me.desht.scrollingmenusign.views.SMSSignView;
import me.desht.scrollingmenusign.views.SMSView;
import me.desht.util.Debugger;
import me.desht.util.MiscUtil;
import me.desht.util.PermissionsUtils;

import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerListener;

public class SMSPlayerListener extends PlayerListener {
	private ScrollingMenuSign plugin;

	public SMSPlayerListener(ScrollingMenuSign plugin) {
		this.plugin = plugin;
	}

	@Override
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.isCancelled())
			return;
		Block block = event.getClickedBlock();
		if (block == null)
			return;

		Player player = event.getPlayer();

		SMSView locView = SMSView.getViewForLocation(block.getLocation());
		SMSMapView mapView = null;
		if (player.getItemInHand().getTypeId() == 358) {
			mapView = SMSMapView.getViewForId(player.getItemInHand().getDurability());
		}

		try {
			if (locView == null && block.getState() instanceof Sign && player.getItemInHand().getTypeId() == 0) {
				// No view present at this location, but a left-click could create a new sign view if the sign's
				// text is in the right format...
				tryToActivateSign(block, player); 
			} else if (locView != null && player.getItemInHand().getTypeId() == 358) {
				// Hit an existing view with a map - the map now becomes a view on the same menu
				tryToActivateMap(block, player);
			} else if (player.getItemInHand().getTypeId() == 358 && block.getTypeId() == 20) {
				// Hit glass with map - deactivate the map if it has a sign view on it
				tryToDeactivateMap(block, player);
			} else if (mapView != null && locView == null && block.getState() instanceof Sign) {
				// Hit a non-active sign with an active map - try to make the sign into a view
				tryToActivateSign(block, player, mapView);
			} else if (mapView != null) {
				// Holding an active map, use that as the view
				Debugger.getDebugger().debug("player interact event @ map_" + mapView.getMapView().getId() + ", " + player.getName() + " did " + event.getAction() + ", menu=" + mapView.getMenu().getName());
				SMSUserAction action = SMSUserAction.getAction(event);
				action.execute(player, mapView);
			} else if (locView != null) {
				// There's a view at the targeted block, use that as the view
				Debugger.getDebugger().debug("player interact event @ " + block.getLocation() + ", " + player.getName() + " did " + event.getAction() + ", menu=" + locView.getMenu().getName());
				SMSUserAction action = SMSUserAction.getAction(event);
				if (action != null) {
					action.execute(player, locView);
				}
			}
		} catch (SMSException e) {
			MiscUtil.errorMessage(player, e.getMessage());
		}
	}

	@Override
	public void onItemHeldChange(PlayerItemHeldEvent event) {
		try {
			Player player = event.getPlayer();
			Block block = player.getTargetBlock(null, 3);
			SMSView view = SMSView.getViewForLocation(block.getLocation());
			if (view == null)
				return;
			Debugger.getDebugger().debug("player item held change event @ " + block.getLocation() + ", " + player.getName() + " did " + event.getPreviousSlot() + "->" + event.getNewSlot() + ", menu =" + view.getMenu().getName());
			SMSUserAction action = SMSUserAction.getAction(event);
			if (action != null) {
				action.execute(player, view);
			}
		} catch (SMSException e) {
			MiscUtil.log(Level.WARNING, e.getMessage());
		}
	}

	//	@Override
	//	public void onPlayerDropItem(PlayerDropItemEvent event) {
	//		if (event.isCancelled())
	//			return;
	//
	//		Item item = event.getItemDrop();
	//		ItemStack is = item.getItemStack();
	//
	//		if (is.getTypeId() == 358 && SMSMapView.checkForMapId(is.getDurability()) && !SMSConfig.getConfiguration().getBoolean("sms.maps.tradable", true)) {
	//			short d = 0;
	//			SMSMenu menu = SMSMapView.getViewForId(is.getDurability()).getMenu();
	//			while (Bukkit.getServer().getMap(d) != null) {
	//				if (SMSMapView.checkForMapId(d)) {
	//					d++;
	//				} else {
	//					item.getItemStack().setDurability(d);
	//					MiscUtil.statusMessage(event.getPlayer(), "Dropped map detached from menu &e" + menu.getName() + "&-.");
	//					break;
	//				}
	//			}
	//			if (Bukkit.getServer().getMap(d) == null) {
	//				MapView mv = Bukkit.getServer().createMap(event.getPlayer().getWorld());
	//				item.getItemStack().setDurability(mv.getId());
	//				MiscUtil.statusMessage(event.getPlayer(), "Dropped map detached from menu &e" + menu.getName() + "&-.");
	//			}
	//		}
	//	}

	//	@Override
	//	public void onPlayerAnimation(PlayerAnimationEvent event) {
	//		if (event.isCancelled())
	//			return;
	//		
	//		Player player = event.getPlayer();
	//		
	//		SMSMapView mapView = null;
	//		if (player.getItemInHand().getTypeId() == 358) {
	//			mapView = SMSMapView.getViewForId(player.getItemInHand().getDurability());
	//		}
	//		
	//		try {
	//			switch (event.getAnimationType()) {
	//			case ARM_SWING:
	//				if (mapView != null) {
	//					Block b = player.getTargetBlock(null, 2);
	//					if (b.getTypeId() == 0) {
	//						// we'll only do this if the player is targeting air - if a block is targeted, the onPlayerInteract handler deals with it
	//						Debugger.getDebugger().debug("player animation event @ map_" + mapView.getMapView().getId() + ", " + player.getName() + ", menu=" + mapView.getMenu().getName());
	//						SMSUserAction action = SMSUserAction.getAction(event);
	//						processAction(action, player, mapView);
	//					}
	//				}	
	//			}
	//		} catch (SMSException e) {
	//			MiscUtil.log(Level.WARNING, e.getMessage());
	//		}
	//	}

	/**
	 * Try to activate a sign by punching it.  The sign needs to contain "[sms]"
	 * on the first line, the menu name on the second line, and (only if a new menu
	 * is to be created) the menu title on the third line. 
	 * 
	 * @param b
	 * @param player
	 * @throws SMSException
	 */
	private void tryToActivateSign(Block b, Player player) throws SMSException {
		Sign sign = (Sign) b.getState();
		if (!sign.getLine(0).equals("[sms]"))
			return;

		String name = sign.getLine(1);
		String title = MiscUtil.parseColourSpec(player, sign.getLine(2));
		if (name.isEmpty())
			return;

		SMSHandler handler = plugin.getHandler();
		if (handler.checkMenu(name)) {
			if (title.isEmpty()) {
				PermissionsUtils.requirePerms(player, "scrollingmenusign.commands.sync");
				SMSMenu menu = handler.getMenu(name);
				SMSView view = SMSSignView.addSignToMenu(menu, b.getLocation());
				MiscUtil.statusMessage(player, "Added new sign view " + view.getName() + " @ &f" + MiscUtil.formatLocation(b.getLocation()) +
				                       "&- was added to menu &e" + name + "&-");
			} else {
				MiscUtil.errorMessage(player, "A menu called '" + name + "' already exists.");
			}
		} else if (title.length() > 0) {
			PermissionsUtils.requirePerms(player, "scrollingmenusign.commands.create");
			SMSMenu menu = plugin.getHandler().createMenu(name, title, player.getName());
			SMSView view = SMSSignView.addSignToMenu(menu, b.getLocation());
			MiscUtil.statusMessage(player, "Sign view " + view.getName() + " @ &f" + MiscUtil.formatLocation(b.getLocation()) +
			                       "&- was added to new menu &e" + name + "&-");
		}

	}

	/**
	 * Try to activate a sign by waving an active map at it.  The map's menu will be "transferred"
	 * to the sign.
	 * 
	 * @param block
	 * @param player
	 * @param mapView
	 * @throws SMSException
	 */
	private void tryToActivateSign(Block block, Player player, SMSMapView mapView) throws SMSException {
		PermissionsUtils.requirePerms(player, "scrollingmenusign.commands.sync");
		PermissionsUtils.requirePerms(player, "scrollingmenusign.maps");
		PermissionsUtils.requirePerms(player, "scrollingmenusign.maps.toSign");
		SMSMenu menu = mapView.getMenu();
		SMSView view = SMSSignView.addSignToMenu(menu, block.getLocation());
		MiscUtil.statusMessage(player, "Added new sign view " + view.getName() + " @ &f" + MiscUtil.formatLocation(block.getLocation()) +
		                       "&- to menu &e" + menu.getName() + "&-");
	}

	/**
	 * Try to deactivate an active map view by hitting glass with it.
	 * 
	 * @param block
	 * @param player
	 * @throws SMSException
	 */
	private void tryToDeactivateMap(Block block, Player player) throws SMSException {
		PermissionsUtils.requirePerms(player, "scrollingmenusign.commands.break");
		PermissionsUtils.requirePerms(player, "scrollingmenusign.maps");
		short mapId = player.getItemInHand().getDurability();
		SMSMapView mapView = SMSMapView.getViewForId(mapId);
		if (mapView != null) {
			mapView.deletePermanent();
			MiscUtil.statusMessage(player, "Removed map view &emap_" + mapView.getName() + "&- from menu &e" + mapView.getMenu().getName() + "&-.");
		}
	}

	/**
	 * Try to activate a map by hitting an active sign view with it.
	 * 
	 * @param block
	 * @param player
	 * @throws SMSException
	 */
	private void tryToActivateMap(Block block, Player player) throws SMSException {
		PermissionsUtils.requirePerms(player, "scrollingmenusign.commands.sync");
		PermissionsUtils.requirePerms(player, "scrollingmenusign.maps");
		PermissionsUtils.requirePerms(player, "scrollingmenusign.maps.fromSign");

		SMSView currentView = SMSSignView.getViewForLocation(block.getLocation());
		if (currentView == null)
			return;

		short mapId = player.getItemInHand().getDurability();
		SMSMapView mapView = SMSMapView.addMapToMenu(mapId, currentView.getMenu());

		MiscUtil.statusMessage(player, "Added new map view &e" + mapView.getName() + "&- to menu &e" + mapView.getMenu().getName() + "&-.");
	}
}
