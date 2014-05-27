package me.desht.scrollingmenusign.views;

import me.desht.dhutils.ConfigurationManager;
import me.desht.scrollingmenusign.*;
import me.desht.scrollingmenusign.views.icon.IconMenu;
import me.desht.scrollingmenusign.views.icon.IconMenu.OptionClickEvent;
import me.desht.scrollingmenusign.views.icon.IconMenu.OptionClickEventHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.UUID;

/**
 * This view uses Minecraft inventories and icons to draw menus.
 * With thanks to Nisovin: http://forums.bukkit.org/threads/icon-menu.108342/
 */
public class SMSInventoryView extends SMSView implements PoppableView, OptionClickEventHandler {

    public static final String WIDTH = "width";
    public static final String AUTOPOPDOWN = "autopopdown";
    public static final String SPACING = "spacing";
    public static final String NO_ESCAPE = "noescape";

    private final Map<UUID, IconMenu> popups = new HashMap<UUID, IconMenu>();

    public SMSInventoryView(String name, SMSMenu menu) {
        super(name, menu);

        registerAttribute(WIDTH, 9, "Number of icons per inventory row");
        registerAttribute(AUTOPOPDOWN, true, "Auto-popdown after item click?");
        registerAttribute(SPACING, 1, "Distance (in slots) between each icon");
        registerAttribute(NO_ESCAPE, false, "Prevent menu being closed with Escape");
    }

    @Override
    public void update(Observable obj, Object arg1) {
        ViewUpdateAction vu = ViewUpdateAction.getAction(arg1);
        switch (vu.getAction()) {
            case REPAINT:
                if (vu.getPlayer() == null) {
                    for (IconMenu im : popups.values()) {
                        im.repaint();
                    }
                } else {
                    IconMenu im = popups.get(vu.getPlayer().getUniqueId());
                    if (im != null) {
                        im.repaint();
                    }
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onDeleted(boolean temporary) {
        for (IconMenu iconMenu : popups.values()) {
            iconMenu.destroy();
        }
    }

    @Override
    public String getType() {
        return "inventory";
    }

    @Override
    public void showGUI(Player player) {
        IconMenu im = popups.get(player.getUniqueId());
        if (im == null) {
            im = new IconMenu(this, player);
            popups.put(player.getUniqueId(), im);
        }
        im.popup();
    }

    @Override
    public void hideGUI(Player player) {
        IconMenu im = popups.get(player.getUniqueId());
        if (im != null) {
            im.popdown();
        }
    }

    @Override
    public void toggleGUI(Player player) {
        if (hasActiveGUI(player)) {
            hideGUI(player);
        } else {
            showGUI(player);
        }
    }

    @Override
    public boolean hasActiveGUI(Player player) {
        SMSPopup popup = getActiveGUI(player);
        return popup != null && popup.isPoppedUp();
    }

    @Override
    public SMSPopup getActiveGUI(Player player) {
        SMSPopup popup = popups.get(player.getUniqueId());
        if (popup != null && popup.isPoppedUp()) {
            return popup;
        } else {
            return null;
        }
    }

    @Override
    public void onOptionClick(final OptionClickEvent event) {
        final Player player = event.getPlayer();
        SMSMenu m = getActiveMenu(player);
        SMSMenuItem item = getActiveMenuItemAt(player, event.getIndex());
        if (item == null) {
            throw new SMSException("icon menu: index " + event.getIndex() + " out of range for " + getActiveMenu(player).getName() + " ?");
        }
        event.setWillClose((Boolean) getAttribute(AUTOPOPDOWN));
        item.executeCommand(player, this, event.getClickType().isRightClick() || event.getClickType().isShiftClick());
        item.feedbackMessage(player);
        onExecuted(player);
        if (item.getCommand().isEmpty() && item.getMessage().isEmpty()) {
            // an item with no command or feedback in an inventory view is basically a label and
            // won't cause the view to close if clicked
            event.setWillClose(false);
            return;
        }
        if (m != getActiveMenu(player)) {
            // just pushed or popped a submenu
            // need to pop this inventory down and pop up a new one with the right title
            event.setWillClose(true);
            Bukkit.getScheduler().runTaskLater(ScrollingMenuSign.getInstance(), new Runnable() {
                @Override
                public void run() {
                    showGUI(player);
                }
            }, 2L);
        }
    }

    @Override
    public Object onConfigurationValidate(ConfigurationManager configurationManager, String key, Object oldVal, Object newVal) {
        super.onConfigurationValidate(configurationManager, key, oldVal, newVal);
        if (key.equals(SPACING)) {
            SMSValidate.isTrue((Integer) newVal >= 1, "Spacing must be 1 or more");
        } else if (key.equals(WIDTH)) {
            SMSValidate.isTrue((Integer) newVal >= 1 && (Integer) newVal <= 9, "Width must be in the range 1 .. 9");
        } else if (key.equals(AUTOPOPDOWN) && !((Boolean) newVal)) {
            SMSValidate.isFalse((Boolean) getAttribute(NO_ESCAPE), "Cannot set 'autopopdown' to false if 'noescape' is true");
        } else if (key.equals(NO_ESCAPE) && ((Boolean) newVal)) {
            SMSValidate.isTrue((Boolean) getAttribute(AUTOPOPDOWN), "Cannot set 'noescape' to true if 'autopopdown' is false");
        }
        return newVal;
    }

    @Override
    public String toString() {
        return "inventory: " + popups.size() + " icon menus created";
    }
}
