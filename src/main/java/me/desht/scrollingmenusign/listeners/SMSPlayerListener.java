package me.desht.scrollingmenusign.listeners;

import me.desht.dhutils.DHUtilsException;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MessagePager;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PermissionUtils;
import me.desht.scrollingmenusign.PopupBook;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSHandler;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSMenuItem;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.scrollingmenusign.enums.SMSUserAction;
import me.desht.scrollingmenusign.expector.ExpectCommandSubstitution;
import me.desht.scrollingmenusign.expector.ExpectSwitchAddition;
import me.desht.scrollingmenusign.expector.ExpectViewCreation;
import me.desht.scrollingmenusign.views.PoppableView;
import me.desht.scrollingmenusign.views.SMSGlobalScrollableView;
import me.desht.scrollingmenusign.views.SMSInventoryView;
import me.desht.scrollingmenusign.views.SMSMapView;
import me.desht.scrollingmenusign.views.SMSScrollableView;
import me.desht.scrollingmenusign.views.SMSSignView;
import me.desht.scrollingmenusign.views.SMSView;
import me.desht.scrollingmenusign.views.redout.Switch;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class SMSPlayerListener extends SMSListenerBase {
	
	public SMSPlayerListener(ScrollingMenuSign plugin) {
		super(plugin);
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getAction() == Action.PHYSICAL) {
			// We're not interested in physical actions (pressure plate) here
			return;
		}
		if (event.isCancelled() && SMSMapView.getHeldMapView(event.getPlayer()) == null && !PopupBook.holding(event.getPlayer())) {
			// Work around weird Bukkit behaviour where all air-click events
			// arrive cancelled by default.
			return;
		}
		
		try {
			boolean cancelEvent = handleInteraction(event);
			event.setCancelled(cancelEvent);
		} catch (SMSException e) {
			MiscUtil.errorMessage(event.getPlayer(), e.getMessage());
		} catch (DHUtilsException e) {
			MiscUtil.errorMessage(event.getPlayer(), e.getMessage());
		}
	}

	@EventHandler
	public void onItemHeldChange(PlayerItemHeldEvent event) {
		Player player = event.getPlayer();
		
		if (plugin.responseHandler.isExpecting(player.getName(), ExpectSwitchAddition.class)) {
			plugin.responseHandler.cancelAction(player.getName(), ExpectSwitchAddition.class);
			MiscUtil.statusMessage(player, "&6Switch placement cancelled.");
			return;
		}
		
		try {
			SMSView view = SMSView.getTargetedView(player);
			if (view == null)
				return;
			LogUtils.fine(String.format("PlayerItemHeldChangeEvent @ %s, %s did %d->%d, menu = %s",
			                            view.getName(), player.getName(),
			                            event.getPreviousSlot(), event.getNewSlot(), view.getNativeMenu().getName()));
			SMSUserAction action = SMSUserAction.getAction(event);
			if (action != null) {
				action.execute(player, view);
			}
		} catch (SMSException e) {
			LogUtils.warning(e.getMessage());
		} catch (IllegalStateException e) {
			// ignore
		}
	}

	@EventHandler(priority=EventPriority.HIGH)
	public void onPlayerChat(AsyncPlayerChatEvent event) {
		Player player = event.getPlayer();
		if (plugin.responseHandler.isExpecting(player.getName(), ExpectCommandSubstitution.class)) {
			try {
				ExpectCommandSubstitution cs = plugin.responseHandler.getAction(player.getName(), ExpectCommandSubstitution.class);
				cs.setSub(event.getMessage());
				cs.handleAction();
				event.setCancelled(true);
			} catch (DHUtilsException e) {
				MiscUtil.errorMessage(player, e.getMessage());
			}
		}
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();

		// clear out user variables if not persistent
		Configuration cfg = plugin.getConfig();
		if (!cfg.getBoolean("sms.persistent_user_vars") && cfg.contains("uservar." + player.getName())) {
			cfg.set("uservar." + player.getName(), null);
			plugin.saveConfig();
		}

		MessagePager.deletePager(player);

		SMSView.clearPlayer(player);
	}

	/**
	 * Main handler for PlayerInterfact events.
	 * 
	 * @param event		the event to handle
	 * @return			true if the event has been handled and should be cancelled now, false otherwise
	 * @throws SMSException	for any error that should be reported to the player
	 */
	private boolean handleInteraction(PlayerInteractEvent event) throws SMSException { 
		Player player = event.getPlayer();
		Block block = event.getClickedBlock();
		SMSMapView mapView = SMSMapView.getHeldMapView(player);
		PopupBook popupBook;
		try {
			popupBook = PopupBook.get(player);
		} catch (SMSException e) {
			// this means the player is holding a book, but it's no longer a valid one
			PopupBook.destroy(player);
			return true;
		}
	
		// If there is no mapView, book or selected block, there's nothing for us to do
		if (block == null && mapView == null && popupBook == null) {
			return false;
		}
	
		SMSView locView = block == null ? null : SMSView.getViewForLocation(block.getLocation());
	
		String playerName = player.getName();
		
		// left or right-clicking cancels any command substitution in progress
		if (plugin.responseHandler.isExpecting(playerName, ExpectCommandSubstitution.class)) {
			plugin.responseHandler.cancelAction(playerName, ExpectCommandSubstitution.class);
			MiscUtil.alertMessage(player, "&6Command execution cancelled.");
		} else if (plugin.responseHandler.isExpecting(playerName, ExpectViewCreation.class) && block != null) {
			// Handle the case where the player is creating a view interactively: left-click to create,
			// right-click to cancel.
			ExpectViewCreation c = plugin.responseHandler.getAction(playerName, ExpectViewCreation.class);
			if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
				c.setLocation(block.getLocation());
				c.handleAction();
			} else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
				c.cancelAction();
				MiscUtil.statusMessage(player, "&6View creation cancelled.");
			}
		} else if (plugin.responseHandler.isExpecting(playerName, ExpectSwitchAddition.class) && isHittingLeverWithSwitch(player, block) ) {
			Switch sw = Switch.getSwitchAt(block.getLocation());
			if (sw == null) {
				ExpectSwitchAddition swa = plugin.responseHandler.getAction(playerName, ExpectSwitchAddition.class);
				swa.setLocation(event.getClickedBlock().getLocation());
				swa.handleAction();
			} else {
				MiscUtil.statusMessage(player, String.format("&6Lever is an output switch already (&e%s / %s&-).",
				                                             sw.getView().getName(), sw.getTrigger()));
			}
		} else if (popupBook != null) {
			// A popup written book - toggle the book's associated poppable view
			popupBook.toggle();
			player.setItemInHand(popupBook.toItemStack());
		} else if (mapView != null) {
			// Holding an active map view
			LogUtils.fine("player interact event @ map_" + mapView.getMapView().getId() + ", " + player.getName() + " did " + event.getAction() +
			              ", menu=" + mapView.getActiveMenu(player.getName()).getName());
			Configuration cfg = plugin.getConfig();
			if (block != null && block.getTypeId() == cfg.getInt("sms.maps.break_block_id")) {
				// Hit the "break block" with active map - deactivate the map if it has a view on it
				tryToDeactivateMap(block, player);
			} else if (locView == null && block != null && block.getState() instanceof Sign) {
				// Hit a non-active sign with an active map - try to make the sign into a view
				tryToActivateSign(block, player, mapView);
			} else {
				SMSUserAction action = SMSUserAction.getAction(event);
				if (action != null) {
					action.execute(player, mapView);
				}
				mapView.setMapItemName(player.getItemInHand());
			}
		} else if (block != null) {
			ItemStack heldItem = player.getItemInHand();
			if (locView == null && block.getState() instanceof Sign && (heldItem == null || heldItem.getType() == Material.AIR)) {
				// No view present at this location, but a left-click could create a new sign view if the sign's
				// text is in the right format...
				return tryToActivateSign(block, player);
			} else if (locView != null && heldItem.getType() == Material.MAP && !SMSMapView.usedByOtherPlugin(player.getItemInHand())) {
				// Hit an existing view with a map - the map now becomes a view on the same menu
				tryToActivateMap(block, player);
			} else if (locView != null && heldItem.getType() == Material.BOOK_AND_QUILL) {
				// Hit an existing view with a book & quill - try to associate a written book with
				// an inventory view on the view's menu
				tryToAddInventoryView((SMSGlobalScrollableView) locView, player);
			} else if (event.getAction() == Action.LEFT_CLICK_BLOCK && isHittingViewWithSwitch(player, locView)) {
				// Hit a globally scrollable view with a button or lever - adding it as a redstone output
				tryToAddRedstoneOutput((SMSGlobalScrollableView) locView, player);
			} else if (locView != null && locView instanceof SMSScrollableView) {
				// There's an interactable view at the targeted block
				LogUtils.fine("player interact event @ " + block.getLocation() + ", " + player.getName() + " did " + event.getAction() +
				              ", menu=" + locView.getActiveMenu(player.getName()).getName());
				SMSUserAction action = SMSUserAction.getAction(event);
				if (action != null) {
					action.execute(player, locView);
				}
				if (event.getAction() == Action.LEFT_CLICK_BLOCK && player.getGameMode() == GameMode.CREATIVE) {
					// left clicking a sign in creative mode even once will blank the sign
					locView.update(locView.getActiveMenu(player.getName()), SMSMenuAction.REPAINT);
				}
			} else {
				return false;
			}
		}
		return true;
	}

	/**
	 * Player has hit an existing view with a book & quill.  Add an inventory view to that view's menu
	 * if it doesn't already have one, then convert the book & quill into a written popup book for the
	 * inventory view.
	 * 
	 * @param view	the view that's been hit
	 * @param player the player
	 */
	private void tryToAddInventoryView(SMSGlobalScrollableView view, Player player) {
		PermissionUtils.requirePerms(player, "scrollingmenusign.use.inventory");
		
		boolean newView = false;
		SMSMenu menu = view.getActiveMenu(player.getName());
		SMSView popView = SMSView.findView(menu, PoppableView.class);
		if (popView == null) {
			PermissionUtils.requirePerms(player, "scrollingmenusign.commands.sync");
			popView = SMSInventoryView.addInventoryViewToMenu(menu);
			newView = true;
		}
		PopupBook popup = new PopupBook(player, popView);
		player.setItemInHand(popup.toItemStack());
		
		if (newView) {
			MiscUtil.statusMessage(player, String.format("Associated book with new %s view &e%s&-", popView.getType(), popView.getName()));
		} else {
			MiscUtil.statusMessage(player, String.format("Associated book with existing %s view &e%s&-", popView.getType(), popView.getName()));
		}
	}

	/**
	 * Try to activate a sign by punching it.  The sign needs to contain "[sms]"
	 * on the first line, the menu name on the second line, and (only if a new menu
	 * is to be created) the menu title on the third line. 
	 * 
	 * @param b
	 * @param player
	 * @return true if the sign can be activated, false otherwise
	 * @throws SMSException
	 */
	private boolean tryToActivateSign(Block b, Player player) throws SMSException {
		Sign sign = (Sign) b.getState();
		if (!sign.getLine(0).equals("[sms]"))
			return false;

		String menuName = sign.getLine(1);
		String title = MiscUtil.parseColourSpec(player, sign.getLine(2));
		if (menuName.isEmpty())
			return false;

		SMSHandler handler = plugin.getHandler();
		if (handler.checkMenu(menuName)) {
			if (title.isEmpty()) {
				PermissionUtils.requirePerms(player, "scrollingmenusign.commands.sync");
				SMSMenu menu = handler.getMenu(menuName);
				SMSView view = SMSSignView.addSignToMenu(menu, b.getLocation());
				MiscUtil.statusMessage(player, String.format("Added new sign view &e%s&- @ &f%s&- to menu &e%s&-.",
				                                             view.getName(), MiscUtil.formatLocation(b.getLocation()), menuName));
			} else {
				MiscUtil.errorMessage(player, "A menu called '" + menuName + "' already exists.");
			}
		} else if (title.length() > 0) {
			PermissionUtils.requirePerms(player, "scrollingmenusign.commands.create");
			SMSMenu menu = plugin.getHandler().createMenu(menuName, title, player.getName());
			SMSView view = SMSSignView.addSignToMenu(menu, b.getLocation());
			MiscUtil.statusMessage(player, String.format("Added new sign view &e%s&- @ &f%s&- to new menu &e%s&-.",
			                                             view.getName(), MiscUtil.formatLocation(b.getLocation()), menuName));
		}
		return true;
	}

	/**
	 * Try to activate a sign by hitting it with an active map.  The map's menu will be "transferred"
	 * to the sign.
	 * 
	 * @param block
	 * @param player
	 * @param mapView
	 * @throws SMSException
	 */
	private void tryToActivateSign(Block block, Player player, SMSMapView mapView) throws SMSException {
		if (!plugin.getConfig().getBoolean("sms.maps.transfer.to_sign")) {
			return;
		}
		PermissionUtils.requirePerms(player, "scrollingmenusign.commands.sync");
		PermissionUtils.requirePerms(player, "scrollingmenusign.use.map");
		PermissionUtils.requirePerms(player, "scrollingmenusign.maps.toSign");
		SMSMenu menu = mapView.getActiveMenu(player.getName());
		SMSView view = SMSSignView.addSignToMenu(menu, block.getLocation());
		MiscUtil.statusMessage(player, String.format("Added new sign view &e%s&- @ &f%s&- to menu &e%s&-.",
		                                             view.getName(), MiscUtil.formatLocation(block.getLocation()), menu.getName()));
	}

	/**
	 * Try to deactivate an active map view by hitting glass with it.
	 * 
	 * @param block
	 * @param player
	 * @throws SMSException
	 */
	private void tryToDeactivateMap(Block block, Player player) throws SMSException {
		PermissionUtils.requirePerms(player, "scrollingmenusign.commands.break");
		PermissionUtils.requirePerms(player, "scrollingmenusign.use.map");
		short mapId = player.getItemInHand().getDurability();
		SMSMapView mapView = SMSMapView.getViewForId(mapId);
		if (mapView != null) {
			mapView.removeMapItemName(player.getItemInHand());
			mapView.deletePermanent();
			MiscUtil.statusMessage(player, String.format("Removed map view &e%s&- from menu &e%s&-.",
			                                             mapView.getName(), mapView.getActiveMenu(player.getName()).getName()));
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
		if (!plugin.getConfig().getBoolean("sms.maps.transfer.from_sign")) {
			return;
		}
		PermissionUtils.requirePerms(player, "scrollingmenusign.commands.sync");
		PermissionUtils.requirePerms(player, "scrollingmenusign.use.map");
		PermissionUtils.requirePerms(player, "scrollingmenusign.maps.fromSign");

		SMSView clickedView = SMSView.getViewForLocation(block.getLocation());
		if (clickedView == null || !(clickedView instanceof SMSGlobalScrollableView))
			return;

		short mapId = player.getItemInHand().getDurability();
		SMSMapView mapView = SMSMapView.addMapToMenu(clickedView.getActiveMenu(player.getName()), mapId);
		mapView.setMapItemName(player.getItemInHand());

		MiscUtil.statusMessage(player, String.format("Added new map view &e%s&- to menu &e%s&-.",
		                                             mapView.getName(), mapView.getActiveMenu(player.getName()).getName()));
	}

	/**
	 * Try to associate a redstone output lever with the currently selected item of the given view.
	 * 
	 * @param locView
	 * @param player
	 */
	private void tryToAddRedstoneOutput(SMSGlobalScrollableView locView, Player player) {
		PermissionUtils.requirePerms(player, "scrollingmenusign.create.switch");
		SMSMenuItem item = locView.getActiveMenu(player.getName()).getItemAt(locView.getScrollPos());
		if (item == null) return;
		
		String trigger = item.getLabel();
		MiscUtil.statusMessage(player, "Place your lever or hit an existing lever to add it as a");
		MiscUtil.statusMessage(player, String.format("  redstone output on view &e%s&- / &e%s&-.",
		                                             locView.getName(), trigger));
		MiscUtil.statusMessage(player, "Change your held item to cancel.");
		
		plugin.responseHandler.expect(player.getName(), new ExpectSwitchAddition(locView, trigger));
	}

	private boolean isHittingViewWithSwitch(Player player, SMSView locView) {
		if (!(locView instanceof SMSGlobalScrollableView))
			return false;
		
		return player.getItemInHand().getType() == Material.LEVER;
	}

	private boolean isHittingLeverWithSwitch(Player player, Block block) {
		if (block == null || block.getType() != Material.LEVER) 
			return false;
		if (player.getItemInHand().getType() != Material.LEVER)
			return false;
		
		return true;
	}

}
