package me.desht.scrollingmenusign;

import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.scrollingmenusign.views.SMSView;
import me.desht.scrollingmenusign.views.ViewUpdateAction;
import org.bukkit.Location;

import java.util.*;

public class MenuManager {
    private static final Map<String, SMSMenu> menus = new HashMap<String, SMSMenu>();
    private static final Map<String, SMSMenu> deletedMenus = new HashMap<String, SMSMenu>();

    private final ScrollingMenuSign plugin;

    public MenuManager(ScrollingMenuSign plugin) {
        this.plugin = plugin;
    }

    /**
     * Add a menu to the menu list, preserving a reference to it.
     *
     * @param menuName   The menu's name
     * @param menu       The menu object
     */
    public void registerMenu(String menuName, SMSMenu menu) {
        menus.put(menuName, menu);

        menu.addObserver(plugin.getVariablesManager());

        menu.autosave();
    }

    /**
     * Remove a menu from the list, destroying the reference to it.
     *
     * @param menuName the menu's name
     * @throws SMSException if there is no menu of the given name
     */
    public void unregisterMenu(String menuName) {
        SMSMenu menu = getMenu(menuName);
        menu.deleteObserver(plugin.getVariablesManager());
        deletedMenus.put(menuName, menu);
        menus.remove(menuName);
    }

    /**
     * Retrieve the menu with the given name.
     *
     * @param menuName the name of the menu to retrieve
     * @return the menu object
     * @throws SMSException if there is no menu of the given name
     */
    public SMSMenu getMenu(String menuName) {
        SMSValidate.isTrue(menus.containsKey(menuName), "No such menu '" + menuName + "'.");
        return menus.get(menuName);
    }

    /**
     * Force the views on all menus to be redrawn.
     */
    public void updateAllMenus() {
        for (SMSMenu menu : listMenus()) {
            menu.notifyObservers(new ViewUpdateAction(SMSMenuAction.REPAINT));
        }
    }

    /**
     * Restore the given deleted menu.
     *
     * @param menuName the name of the menu to restore
     * @return the restored menu
     * @throws SMSException if there is no deleted menu to restore
     */
    public SMSMenu restoreDeletedMenu(String menuName) {
        SMSValidate.isTrue(deletedMenus.containsKey(menuName), "No such deleted menu '" + menuName + "'.");
        SMSMenu menu = deletedMenus.get(menuName);
        registerMenu(menuName, menu);
        deletedMenus.remove(menuName);
        return menu;
    }

    /**
     * Get a list of the deleted menu names.
     *
     * @return a list of the deleted menu names
     */
    public List<String> listDeletedMenus() {
        return new ArrayList<String>(deletedMenus.keySet());
    }

    /**
     * Retrieve the deleted menu with the given name.
     *
     * @param menuName The name of the menu to retrieve
     * @return The menu object
     * @throws SMSException if the menu name is not found
     */
    public SMSMenu getDeletedMenu(String menuName) {
        SMSValidate.isTrue(deletedMenus.containsKey(menuName), "No such deleted menu '" + menuName + "'.");
        return deletedMenus.get(menuName);
    }

    /**
     * Get the name of the menu at the given location.
     *
     * @param loc The location
     * @return The menu name, or null if there is no menu sign at the location
     */
    public String getMenuNameAt(Location loc) {
        SMSView v = ScrollingMenuSign.getInstance().getViewManager().getViewForLocation(loc);
        return v == null ? null : v.getNativeMenu().getName();
    }

    /**
     * Get the menu at the given location
     *
     * @param loc The location
     * @return The menu object
     * @throws SMSException if there is no menu sign at the location
     */
    public SMSMenu getMenuAt(Location loc) {
        SMSView v = ScrollingMenuSign.getInstance().getViewManager().getViewForLocation(loc);
        return v == null ? null : v.getNativeMenu();
    }

    /**
     * Check to see if a menu with the given name exists
     *
     * @param menuName The menu name
     * @return true if the menu exists, false if it does not
     */
    public boolean checkForMenu(String menuName) {
        return menus.containsKey(menuName);
    }

    /**
     * Return an unsorted list of all the known menus
     * Equivalent to calling <b>listMenus(false)</b>
     *
     * @return A list of SMSMenu objects
     */
    public List<SMSMenu> listMenus() {
        return listMenus(false);
    }

    /**
     * Return a list of all the known menus, possibly sorted.
     *
     * @param isSorted true if the list should be sorted by menu name; false otherwise
     * @return a list of SMSMenu objects
     */
    public List<SMSMenu> listMenus(boolean isSorted) {
        if (isSorted) {
            SortedSet<SMSMenu> sorted = new TreeSet<SMSMenu>(menus.values());
            return new ArrayList<SMSMenu>(sorted);
        } else {
            return new ArrayList<SMSMenu>(menus.values());
        }
    }
}
