package me.desht.scrollingmenusign.views;

import java.util.*;

import me.desht.dhutils.ConfigurationManager;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSMenuItem;
import me.desht.scrollingmenusign.SMSValidate;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.scrollingmenusign.views.icon.IconMenu;
import me.desht.scrollingmenusign.views.icon.IconMenu.OptionClickEvent;
import me.desht.scrollingmenusign.views.icon.IconMenu.OptionClickEventHandler;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * This view uses Minecraft inventories and icons to draw menus.
 * With thanks to Nisovin: http://forums.bukkit.org/threads/icon-menu.108342/
 */
public class SMSInventoryView extends SMSView implements PoppableView, OptionClickEventHandler {

    public static final String WIDTH = "width";
    public static final String AUTOPOPDOWN = "autopopdown";
    public static final String SPACING = "spacing";
    public static final String NO_ESCAPE = "noescape";

    private final Map<String, IconMenu> iconMenus;    // map menu name to the icon menu object
    private final Map<String, Set<UUID>> users;    // map menu name to list of players using it

    public SMSInventoryView(String name, SMSMenu menu) {
        super(name, menu);

        registerAttribute(WIDTH, 9, "Number of icons per inventory row");
        registerAttribute(AUTOPOPDOWN, true, "Auto-popdown after item click?");
        registerAttribute(SPACING, 1, "Distance (in slots) between each icon");
        registerAttribute(NO_ESCAPE, false, "Prevent menu being closed with Escape");

        iconMenus = new HashMap<String, IconMenu>();
        iconMenus.put(getNativeMenu().getName(), new IconMenu(this, getNativeMenu().getName()));

        users = new HashMap<String, Set<UUID>>();
    }

    @Override
    public void update(Observable obj, Object arg1) {
        SMSMenu menu = (SMSMenu) obj;
        switch ((SMSMenuAction) arg1) {
            case REPAINT:
                if (menu == null) {
                    for (IconMenu iconMenu : iconMenus.values()) {
                        iconMenu.repaint();
                    }
                } else if (iconMenus.containsKey(menu.getName())) {
                    iconMenus.get(menu.getName()).repaint();
                }
                break;
            default:
                break;
        }
    }

    public Set<UUID> playersUsing(String menuName) {
        if (!users.containsKey(menuName)) {
            users.put(menuName, new HashSet<UUID>());
        }
        return users.get(menuName);
    }

    @Override
    public void pushMenu(Player player, SMSMenu newActive) {
        super.pushMenu(player, newActive);
        String menuName = newActive.getName();

        if (playersUsing(menuName).isEmpty()) {
            // this menu was not used by anyone else yet - create it
            iconMenus.put(menuName, new IconMenu(this, menuName));
        }
        playersUsing(menuName).add(player.getUniqueId());
    }

    @Override
    public SMSMenu popMenu(Player player) {
        SMSMenu oldActive = super.popMenu(player);

        String menuName = oldActive.getName();
        playersUsing(menuName).remove(player.getUniqueId());
        if (playersUsing(menuName).isEmpty()) {
            // no one using this menu any more - destroy it
            iconMenus.get(menuName).destroy();
            iconMenus.remove(menuName);
        }

        return oldActive;
    }

    @Override
    public void onDeleted(boolean temporary) {
        for (IconMenu iconMenu : iconMenus.values()) {
            iconMenu.destroy();
        }
    }

    @Override
    public String getType() {
        return "inventory";
    }

    @Override
    public void showGUI(Player player) {
        String menuName = getActiveMenu(player).getName();
        iconMenus.get(menuName).popup(player);
    }

    @Override
    public void hideGUI(Player player) {
        String menuName = getActiveMenu(player).getName();
        iconMenus.get(menuName).popdown(player);
    }

    @Override
    public void toggleGUI(Player p) {
        if (hasActiveGUI(p)) {
            hideGUI(p);
        } else {
            showGUI(p);
        }
    }

    @Override
    public boolean hasActiveGUI(Player player) {
        String menuName = getActiveMenu(player).getName();
        return iconMenus.get(menuName).isPoppedUp(player);
    }

    @Override
    public SMSPopup getActiveGUI(Player player) {
        String menuName = getActiveMenu(player).getName();
        return hasActiveGUI(player) ? iconMenus.get(menuName) : null;
    }

    @Override
    public void onOptionClick(final OptionClickEvent event) {
        final Player player = event.getPlayer();
        SMSMenu m = getActiveMenu(player);
        SMSMenuItem item = getActiveMenuItemAt(player, event.getIndex());
        if (item == null) {
            throw new SMSException("icon menu: index " + event.getIndex() + " out of range for " + getActiveMenu(player).getName() + " ?");
        }
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
        } else {
            event.setWillClose((Boolean) getAttribute(AUTOPOPDOWN));
        }
    }

    @Override
    public void onConfigurationValidate(ConfigurationManager configurationManager, String key, Object oldVal, Object newVal) {
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
    }

    @Override
    public String toString() {
        return "inventory: " + users.size() + " using " + iconMenus.size() + " menus";
    }
}
