package me.desht.scrollingmenusign.listeners;

import me.desht.dhutils.*;
import me.desht.scrollingmenusign.*;
import me.desht.scrollingmenusign.enums.SMSUserAction;
import me.desht.scrollingmenusign.expector.ExpectCommandSubstitution;
import me.desht.scrollingmenusign.expector.ExpectSwitchAddition;
import me.desht.scrollingmenusign.expector.ExpectViewCreation;
import me.desht.scrollingmenusign.views.*;
import me.desht.scrollingmenusign.views.action.RepaintAction;
import me.desht.scrollingmenusign.views.redout.Switch;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

public class SMSPlayerListener extends SMSListenerBase {

    public SMSPlayerListener(ScrollingMenuSign plugin) {
        super(plugin);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL) {
            // We're not interested in physical actions here
            return;
        }
        try {
            boolean shouldCancel = handleInteraction(event);
            if (shouldCancel) {
                event.setCancelled(true);
            }
        } catch (DHUtilsException e) {
            MiscUtil.errorMessage(event.getPlayer(), e.getMessage());
        }
    }

    @EventHandler
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();

        if (plugin.responseHandler.isExpecting(player, ExpectSwitchAddition.class)) {
            plugin.responseHandler.cancelAction(player, ExpectSwitchAddition.class);
            MiscUtil.statusMessage(player, "&6Switch placement cancelled.");
            return;
        }

        try {
            SMSView view = plugin.getViewManager().getTargetedView(player);
            SMSUserAction action = null;
            if (view != null) {
                Debugger.getInstance().debug(String.format("PlayerItemHeldChangeEvent @ %s, %s did %d->%d, menu = %s",
                        view.getName(), player.getDisplayName(),
                        event.getPreviousSlot(), event.getNewSlot(), view.getNativeMenu().getName()));
                action = SMSUserAction.getAction(event);
                if (action != null) {
                    action.execute(player, view);
                }
            } else {
                if (ActiveItem.isActiveItem(player.getItemInHand()) && player.isSneaking()) {
                    action = SMSUserAction.getAction(event);
                    new ActiveItem(player.getItemInHand()).processAction(player, action);
                }
            }
            if ((action == SMSUserAction.SCROLLDOWN || action == SMSUserAction.SCROLLUP) && player.isSneaking()) {
                // Bukkit 1.5.1+ PlayerItemHeldEvent is now cancellable
                event.setCancelled(true);
            }
        } catch (DHUtilsException e) {
            LogUtils.warning(e.getMessage());
        } catch (IllegalStateException e) {
            // ignore
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (plugin.responseHandler.isExpecting(player, ExpectCommandSubstitution.class)) {
            try {
                ExpectCommandSubstitution cs = plugin.responseHandler.getAction(player, ExpectCommandSubstitution.class);
                cs.setSub(event.getMessage());
                cs.handleAction(player);
                event.setCancelled(true);
            } catch (DHUtilsException e) {
                MiscUtil.errorMessage(player, e.getMessage());
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        MessagePager.deletePager(player);

        plugin.getViewManager().clearPlayer(player);
    }

    @EventHandler
    public void onItemFrameRightClicked(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (entity.getType() == EntityType.ITEM_FRAME) {
            ItemStack item = ((ItemFrame) entity).getItem();
            if (item.getType() == Material.MAP && plugin.getViewManager().checkForMapId(item.getDurability())) {
                SMSUserAction action = SMSUserAction.getAction(event);
                SMSMapView mapView = plugin.getViewManager().getMapViewForId(item.getDurability());
                try {
                    action.execute(event.getPlayer(), mapView);
                } catch (SMSException e) {
                    MiscUtil.errorMessage(event.getPlayer(), e.getMessage());
                }
                event.setCancelled(true);
            }
        }
    }

    /**
     * Main handler for PlayerInteract events.
     *
     * @param event the event to handle
     * @return true if the event has been handled and should be cancelled now, false otherwise
     * @throws SMSException for any error that should be reported to the player
     */
    private boolean handleInteraction(PlayerInteractEvent event) throws SMSException {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        PopupBook popupBook = null;
        PopupItem popupItem = null;
        ActiveItem activeItem = null;
        SMSMapView mapView = plugin.getViewManager().getHeldMapView(player);

        // check the various special items that the player might be holding
        if (mapView == null) {
            activeItem = ActiveItem.get(player);
            if (activeItem == null) {
                popupItem = PopupItem.get(player);
                if (popupItem == null) {
                    popupBook = PopupBook.get(player);
                }
            }
        }
        if (block == null && mapView == null && popupBook == null && activeItem == null && popupItem == null) {
            // nothing for us to do here
            return false;
        }

        SMSView clickedView = block == null ? null : plugin.getViewManager().getViewForLocation(block.getLocation());

        if (plugin.responseHandler.isExpecting(player, ExpectCommandSubstitution.class)) {
            // left or right-clicking cancels any command substitution in progress
            plugin.responseHandler.cancelAction(player, ExpectCommandSubstitution.class);
            MiscUtil.alertMessage(player, "&6Command execution cancelled.");
        } else if (plugin.responseHandler.isExpecting(player, ExpectViewCreation.class) && block != null) {
            handleViewCreation(event, player, block);
        } else if (plugin.responseHandler.isExpecting(player, ExpectSwitchAddition.class) && isHittingLeverWithSwitch(player, block)) {
            handleSwitchCreation(player, block);
        } else if (popupItem != null) {
            popupItem.toggle(player);
        } else if (popupBook != null) {
            // A popup written book - toggle the book's associated poppable view
            popupBook.toggle();
            player.setItemInHand(popupBook.toItemStack());
        } else if (activeItem != null) {
            SMSUserAction action = SMSUserAction.getAction(event);
            activeItem.processAction(player, action);
        } else if (mapView != null) {
            handleMapInteraction(event, player, block, mapView, clickedView);
        } else if (block != null) {
            ItemStack heldItem = player.getItemInHand();
            if (clickedView != null && heldItem.getType() == Material.MAP) {
                // Hit an existing view with a map - the map now becomes a view on the same menu
                tryToActivateMap(clickedView, player);
            } else if (clickedView != null && heldItem.getType() == Material.BOOK_AND_QUILL) {
                // Hit an existing view with a book & quill - try to associate a written book with
                // an inventory view on the view's menu
                tryToAddInventoryView((SMSGlobalScrollableView) clickedView, player);
            } else if (event.getAction() == Action.LEFT_CLICK_BLOCK && isHittingViewWithSwitch(player, clickedView)) {
                // Hit a globally scrollable view with a button or lever - adding it as a redstone output
                tryToAddRedstoneOutput((SMSGlobalScrollableView) clickedView, player);
            } else if (clickedView != null && clickedView instanceof SMSScrollableView && clickedView.isClickable()) {
                // There's an interactable view at the targeted block
                Debugger.getInstance().debug("player interact event @ " + block.getLocation() +
                        ", " + player.getDisplayName() + " did " + event.getAction() +
                        ", menu=" + clickedView.getActiveMenu(player).getName());
                SMSUserAction action = SMSUserAction.getAction(event);
                if (action != null) {
                    action.execute(player, clickedView);
                }
                if (event.getAction() == Action.LEFT_CLICK_BLOCK && player.getGameMode() == GameMode.CREATIVE) {
                    // left clicking a sign in creative mode even once will blank the sign
                    clickedView.update(clickedView.getActiveMenu(player), new RepaintAction());
                }
            } else {
                return false;
            }
        }
        return true;
    }

    private void handleMapInteraction(PlayerInteractEvent event, Player player, Block block, SMSMapView mapView, SMSView clickedView) {
        // Holding an active map view
        Debugger.getInstance().debug("player interact event @ map_" + mapView.getMapView().getId() +
                ", " + player.getDisplayName() + " did " + event.getAction() +
                ", menu=" + mapView.getActiveMenu(player).getName());
        if (clickedView == null && block != null && (block.getType() == Material.WALL_SIGN || block.getType() == Material.SIGN_POST)) {
            // Hit a non-active sign with an active map - try to make the sign into a view
            tryToActivateSign(block, player, mapView.getActiveMenu(player));
        } else {
            SMSUserAction action = SMSUserAction.getAction(event);
            if (action != null) {
                action.execute(player, mapView);
            }
            mapView.setMapItemName(player.getItemInHand());
        }
    }

    private void handleViewCreation(PlayerInteractEvent event, Player player, Block block) {
        // Handle the case where the player is creating a view interactively:
        // left-click to create, right-click to cancel.
        ExpectViewCreation c = plugin.responseHandler.getAction(player, ExpectViewCreation.class);
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            c.setLocation(block.getLocation());
            c.handleAction(player);
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            c.cancelAction(player);
            MiscUtil.statusMessage(player, "&6View creation cancelled.");
        }
    }

    private void handleSwitchCreation(Player player, Block block) {
        Switch sw = plugin.getLocationManager().getInteractableAt(block.getLocation(), Switch.class);
        if (sw == null) {
            ExpectSwitchAddition swa = plugin.responseHandler.getAction(player, ExpectSwitchAddition.class);
            swa.setLocation(block.getLocation());
            swa.handleAction(player);
        } else {
            MiscUtil.statusMessage(player, String.format("&6Lever is an output switch already (&e%s / %s&-).",
                    sw.getView().getName(), sw.getTrigger()));
        }
    }

    /**
     * Player has hit an existing view with a book & quill.  Add an inventory view to that view's menu
     * if it doesn't already have one, then convert the book & quill into a written popup book for the
     * inventory view.
     *
     * @param view   the view that's been hit
     * @param player the player
     */
    private void tryToAddInventoryView(SMSGlobalScrollableView view, Player player) {
        PermissionUtils.requirePerms(player, "scrollingmenusign.use.inventory");

        boolean newView = false;
        SMSMenu menu = view.getActiveMenu(player);
        SMSView popView = plugin.getViewManager().findView(menu, PoppableView.class);
        if (popView == null) {
            PermissionUtils.requirePerms(player, "scrollingmenusign.commands.sync");
            popView = plugin.getViewManager().addInventoryViewToMenu(menu, player);
            newView = true;
        }
        PopupBook popup = new PopupBook(player, popView);
        ItemStack stack = player.getItemInHand();
        if (stack.getAmount() == 1) {
            player.setItemInHand(popup.toItemStack());
        } else {
            player.setItemInHand(new ItemStack(stack.getType(), stack.getAmount() - 1));
            player.getInventory().addItem(popup.toItemStack());
            //noinspection deprecation
            player.updateInventory();
        }

        if (newView) {
            MiscUtil.statusMessage(player, String.format("Associated book with new %s view &e%s&-", popView.getType(), popView.getName()));
        } else {
            MiscUtil.statusMessage(player, String.format("Associated book with existing %s view &e%s&-", popView.getType(), popView.getName()));
        }
    }

    /**
     * Try to activate a sign by hitting it with an active map.  The map's menu will be "transferred"
     * to the sign.
     *
     * @param block  the block being hit
     * @param player the player doing the hitting
     * @param menu   the menu to add the sign to
     * @throws SMSException
     */
    private void tryToActivateSign(Block block, Player player, SMSMenu menu) throws SMSException {
        if (!plugin.getConfig().getBoolean("sms.maps.transfer.to_sign")) {
            return;
        }
        PermissionUtils.requirePerms(player, "scrollingmenusign.commands.sync");
        PermissionUtils.requirePerms(player, "scrollingmenusign.use.map");
        PermissionUtils.requirePerms(player, "scrollingmenusign.maps.to.sign");
        SMSView view = plugin.getViewManager().addSignToMenu(menu, block.getLocation(), player);
        MiscUtil.statusMessage(player, String.format("Added new sign view &e%s&- @ &f%s&- to menu &e%s&-.",
                view.getName(), MiscUtil.formatLocation(block.getLocation()), menu.getName()));
    }

    /**
     * Try to activate a map by hitting an active sign view with it.
     *
     * @param view   the view for the sign
     * @param player the player doing the hitting
     * @throws SMSException
     */
    private void tryToActivateMap(SMSView view, Player player) throws SMSException {
        if (!plugin.getConfig().getBoolean("sms.maps.transfer.from_sign")) {
            return;
        }
        short mapId = player.getItemInHand().getDurability();
        if (plugin.getViewManager().isMapUsedByOtherPlugin(mapId)) {
            return;
        }
        PermissionUtils.requirePerms(player, "scrollingmenusign.commands.sync");
        PermissionUtils.requirePerms(player, "scrollingmenusign.use.map");
        PermissionUtils.requirePerms(player, "scrollingmenusign.maps.from.sign");

        SMSMapView mapView = plugin.getViewManager().addMapToMenu(view.getActiveMenu(player), mapId, player);
        mapView.setMapItemName(player.getItemInHand());

        MiscUtil.statusMessage(player, String.format("Added new map view &e%s&- to menu &e%s&-.",
                mapView.getName(), mapView.getActiveMenu(player).getName()));
    }

    /**
     * Try to associate a redstone output lever with the currently selected item of the given view.
     *
     * @param view   the view
     * @param player the player adding the output
     */
    private void tryToAddRedstoneOutput(SMSGlobalScrollableView view, Player player) {
        PermissionUtils.requirePerms(player, "scrollingmenusign.create.switch");
        view.ensureAllowedToModify(player);
        SMSMenuItem item = view.getActiveMenuItemAt(player, view.getScrollPos());
        if (item == null) return;

        String trigger = item.getLabel();
        MiscUtil.statusMessage(player, "Place your lever or hit an existing lever to add it as a");
        MiscUtil.statusMessage(player, String.format("  redstone output on view &e%s&- / &e%s&-.",
                view.getName(), trigger));
        MiscUtil.statusMessage(player, "Change your held item to cancel.");

        plugin.responseHandler.expect(player, new ExpectSwitchAddition(view, trigger));
    }

    private boolean isHittingViewWithSwitch(Player player, SMSView view) {
        return view instanceof SMSGlobalScrollableView && player.getItemInHand().getType() == Material.LEVER;
    }

    private boolean isHittingLeverWithSwitch(Player player, Block block) {
        return block != null && block.getType() == Material.LEVER && player.getItemInHand().getType() == Material.LEVER;
    }

}
