package me.desht.scrollingmenusign.views;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.Map.Entry;

import me.desht.dhutils.Debugger;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.block.BlockUtil;
import me.desht.scrollingmenusign.PopupBook;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSPersistence;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.scrollingmenusign.views.SMSMapView.SMSMapRenderer;
import me.desht.scrollingmenusign.views.SMSView.MenuStack;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.util.Vector;

public class ViewManager {
    // map view name to view object for registered views
    private final Map<String, SMSView> allViewNames = new HashMap<String, SMSView>();

    private final ScrollingMenuSign plugin;

    public ViewManager(ScrollingMenuSign plugin) {
        this.plugin = plugin;
    }

    /**
     * Instantiate a new view from a saved config file
     *
     * @param node The configuration
     * @return The view object
     */
    public SMSView loadView(ConfigurationSection node) {
        String viewName = null;
        try {
            SMSPersistence.mustHaveField(node, "class");
            SMSPersistence.mustHaveField(node, "name");
            SMSPersistence.mustHaveField(node, "menu");

            String className = node.getString("class");
            viewName = node.getString("name");

            Class<? extends SMSView> c = Class.forName(className).asSubclass(SMSView.class);
            Constructor<? extends SMSView> ctor = c.getDeclaredConstructor(String.class, SMSMenu.class);
            SMSView view = ctor.newInstance(viewName, SMSMenu.getMenu(node.getString("menu")));
            if (!node.contains("group") && node.getString(SMSView.ACCESS).equals("GROUP")) {
                // migration - GROUP access becomes OWNER_GROUP in v2.4.0
                LogUtils.info("view " + viewName + ": migrate GROUP -> OWNER_GROUP access");
                node.set(SMSView.ACCESS, "OWNER_GROUP");
            }
            view.thaw(node);
            registerView(view);
            return view;
        } catch (ClassNotFoundException e) {
            loadError(viewName, e);
        } catch (SMSException e) {
            loadError(viewName, e);
        } catch (InstantiationException e) {
            loadError(viewName, e);
        } catch (IllegalAccessException e) {
            loadError(viewName, e);
        } catch (SecurityException e) {
            loadError(viewName, e);
        } catch (NoSuchMethodException e) {
            loadError(viewName, e);
        } catch (IllegalArgumentException e) {
            loadError(viewName, e);
        } catch (InvocationTargetException e) {
            loadError(viewName, e.getCause());
        }
        return null;
    }

    private void loadError(String viewName, Throwable e) {
        LogUtils.warning("Caught " + e.getClass().getName() + " while loading view " + viewName);
        LogUtils.warning("  Exception message: " + e.getMessage());
    }

    /**
     * Register this view in the global view list and get it saved to disk.
     */
    public void registerView(SMSView view) {
        if (allViewNames.containsKey(view.getName())) {
            throw new SMSException("A view named '" + view.getName() + "' already exists.");
        }
        allViewNames.put(view.getName(), view);
        for (Location l : view.getLocations()) {
            plugin.getLocationManager().registerLocation(l, view);
        }
        view.getNativeMenu().addObserver(view);
        view.autosave();
    }

    /**
     * Unregister a view: remove it as an observer from its menu (and any active submenus),
     * and remove its name & location(s) from the manager.
     *
     * @param view the view to unregister
     */
    private void unregisterView(SMSView view) {
        view.getNativeMenu().deleteObserver(view);
        for (UUID playerId : view.getSubmenuPlayers()) {
            MenuStack mst = view.getMenuStack(playerId);
            for (WeakReference<SMSMenu> ref : mst.stack) {
                SMSMenu m = ref.get();
                if (m != null) {
                    m.deleteObserver(view);
                }
            }
        }
        allViewNames.remove(view.getName());
        for (Location l : view.getLocations()) {
            plugin.getLocationManager().unregisterLocation(l);
        }
    }

    public void deleteView(SMSView view, boolean permanent) {
        unregisterView(view);
        view.onDeleted(permanent);
        if (permanent) {
            SMSPersistence.unPersist(view);
        }
    }

    public void registerLocation(Location loc, SMSView view) {
        plugin.getLocationManager().registerLocation(loc, view);
    }

    public void unregisterLocation(Location loc) {
        plugin.getLocationManager().unregisterLocation(loc);
    }

    /**
     * Check to see if the name view exists
     *
     * @param name The view name
     * @return true if the named view exists, false otherwise
     */
    public boolean checkForView(String name) {
        return allViewNames.containsKey(name);
    }

    /**
     * Get all known view objects as a List
     *
     * @return A list of all known views
     */
    public List<SMSView> listViews() {
        return new ArrayList<SMSView>(allViewNames.values());
    }

    /**
     * Get all known view objects as a Java array
     *
     * @return An array of all known views
     */
    public SMSView[] getViewsAsArray() {
        return allViewNames.values().toArray(new SMSView[allViewNames.size()]);
    }

    /**
     * Get the named SMSView object
     *
     * @param name The view name
     * @return The SMSView object of that name
     * @throws SMSException if there is no such view with the given name
     */
    public SMSView getView(String name) throws SMSException {
        if (!checkForView(name))
            throw new SMSException("No such view: " + name);

        return allViewNames.get(name);
    }

    /**
     * Get the view object at the given location, if any.
     *
     * @param loc The location to check
     * @return The SMSView object at that location, or null if there is none
     */
    public SMSView getViewForLocation(Location loc) {
        return plugin.getLocationManager().getInteractableAt(loc, SMSView.class);
    }

    /**
     * Find all the views for the given menu.
     *
     * @param menu The menu object to check
     * @return A list of SMSView objects which are views for that menu
     */
    public List<SMSView> getViewsForMenu(SMSMenu menu) {
        return getViewsForMenu(menu, false);
    }

    /**
     * Find all the views for the given menu, optionally sorting the resulting list.
     *
     * @param menu     The menu object to check
     * @param isSorted If true, sort the returned view list by view name
     * @return A list of SMSView objects which are views for that menu
     */
    public List<SMSView> getViewsForMenu(SMSMenu menu, boolean isSorted) {
        if (isSorted) {
            SortedSet<String> sorted = new TreeSet<String>(allViewNames.keySet());
            List<SMSView> res = new ArrayList<SMSView>();
            for (String name : sorted) {
                SMSView v = allViewNames.get(name);
                if (v.getNativeMenu() == menu) {
                    res.add(v);
                }
            }
            return res;
        } else {
            return new ArrayList<SMSView>(allViewNames.values());
        }
    }

    /**
     * Get a count of views used, keyed by view type.  Used for metrics gathering.
     *
     * @return a map of type -> count of views of that type
     */
    public Map<String, Integer> getViewCounts() {
        Map<String, Integer> map = new HashMap<String, Integer>();
        for (Entry<String, SMSView> e : allViewNames.entrySet()) {
            String type = e.getValue().getType();
            if (!map.containsKey(type)) {
                map.put(type, 1);
            } else {
                map.put(type, map.get(type) + 1);
            }
        }
        return map;
    }

    /**
     * Get the view that the player is looking at (or holding), if any.
     *
     * @param player    The player
     * @param mustExist if true and no view is found, throw an exception
     * @return the view being looked at, or null if no view is targeted
     * @throws SMSException if mustExist is true and no view is targeted
     */
    public SMSView getTargetedView(Player player, boolean mustExist) {
        SMSView view = null;

        if (player.getItemInHand().getType() == Material.MAP) {
            view = getHeldMapView(player);
        }

        if (view == null && PopupBook.holding(player)) {
            // popup book (spout/inventory)
            PopupBook book = PopupBook.get(player);
            view = book.getView();
        }

        Block b = null;
        if (view == null) {
            // targeted view (sign/multisign/redstone)
            try {
                b = player.getTargetBlock(null, ScrollingMenuSign.BLOCK_TARGET_DIST);
                view = getViewForLocation(b.getLocation());
            } catch (IllegalStateException e) {
                // the block iterator can throw this sometimes - we can ignore it
            }
        }

        if (view == null && b != null) {
            // maybe there's a map view item frame attached to the block we're looking at
            ItemFrame frame = getMapFrame(b, player.getEyeLocation());
            if (frame != null) {
                view = getMapViewForId(frame.getItem().getDurability());
            }
        }

        if (view == null && mustExist) {
            throw new SMSException("You are not looking at a menu view.");
        }

        return view;
    }

    /**
     * Get the view that the player is looking at (or holding), if any.
     *
     * @param player The player
     * @return the view being looked at, or null if no view is targeted
     */
    public SMSView getTargetedView(Player player) {
        return getTargetedView(player, false);
    }

    /**
     * Find a view for the given menu.
     *
     * @param menu the SMS menu
     * @return the first view found for the menu
     */
    public SMSView findView(SMSMenu menu) {
        return findView(menu, null);
    }


    /**
     * Find a view for the given menu.
     *
     * @param menu the SMS menu
     * @return the first view found of the given class (or interface) for the menu
     */
    public SMSView findView(SMSMenu menu, Class<?> c) {
        for (SMSView view : listViews()) {
            if (view.getNativeMenu() == menu && (c == null || c.isAssignableFrom(view.getClass()))) {
                return view;
            }
        }
        return null;
    }

    /**
     * Called when a player logs out.  Call the clearPlayerForView() method on all
     * known views.
     *
     * @param player the player object
     */
    public void clearPlayer(Player player) {
        for (SMSView v : listViews()) {
            v.clearPlayerForView(player);
        }
    }

    /**
     * Load any deferred locations for the given world.  This is called by the WorldLoadEvent handler.
     *
     * @param world The world that's just been loaded.
     */
    public void loadDeferred(World world) {
        for (SMSView view : listViews()) {
            List<Vector> l = view.getDeferredLocations(world.getName());
            if (l == null) {
                continue;
            }

            for (Vector vec : l) {
                try {
                    view.addLocation(new Location(world, vec.getBlockX(), vec.getBlockY(), vec.getBlockZ()));
                    Debugger.getInstance().debug("added loc " + world.getName() + ", " + vec + " to view " + view.getName());
                } catch (SMSException e) {
                    LogUtils.warning("Can't add location " + world.getName() + ", " + vec + " to view " + view.getName());
                    LogUtils.warning("  Exception message: " + e.getMessage());
                }
            }
            l.clear();
        }
    }

    /**
     * Given a map ID, return the map view object for that ID, if any.
     *
     * @param mapId The ID of the map
     * @return The SMSMapView object for the ID, or null if this map ID isn't used for a SMSMapView
     */
    public SMSMapView getMapViewForId(short mapId) {
        MapView mv = Bukkit.getMap(mapId);
        if (mv != null) {
            for (MapRenderer r : mv.getRenderers()) {
                if (r instanceof SMSMapRenderer) {
                    return ((SMSMapRenderer) r).getView();
                }
            }
        }
        return null;
    }

    /**
     * Check if the given map ID is used for a SMSMapView
     *
     * @param mapId The ID of the map
     * @return true if the ID is used for a SMSMapView, false otherwise
     */
    public boolean checkForMapId(short mapId) {
        MapView mv = Bukkit.getMap(mapId);
        if (mv != null) {
            for (MapRenderer r : mv.getRenderers()) {
                if (r instanceof SMSMapRenderer) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Convenience routine.  Add the given mapId as a view on the given menu.
     *
     * @param menu  The menu to add the view to
     * @param mapId ID of the map that will be used as a view
     * @return The SMSMapView object that was just created
     * @throws SMSException if the given mapId is already a view
     */
    public SMSMapView addMapToMenu(String viewName, SMSMenu menu, short mapId, CommandSender owner) throws SMSException {
        if (checkForMapId(mapId)) {
            throw new SMSException("Map #" + mapId + " already has a menu view associated with it");
        }
        if (isMapUsedByOtherPlugin(mapId)) {
            throw new SMSException("Map #" + mapId + " is used by another plugin");
        }

        SMSMapView mapView = new SMSMapView(viewName, menu);
        registerView(mapView);
        mapView.setAttribute(SMSView.OWNER, mapView.makeOwnerName(owner));
        mapView.setOwnerId(getUniqueId(owner));
        mapView.setMapId(mapId);
        mapView.update(menu, SMSMenuAction.REPAINT);

        return mapView;
    }

    public SMSMapView addMapToMenu(SMSMenu menu, short mapId, CommandSender owner) throws SMSException {
        return addMapToMenu(null, menu, mapId, owner);
    }

    /**
     * Check to see if this map ID is used by another plugin, to avoid toe-stepping-upon...
     * The check is for any renderers on the map of a class outside the org.bukkit namespace.
     *
     * @param mapId ID of the map to check
     * @return true if it's used by someone else, false otherwise
     */
    public boolean isMapUsedByOtherPlugin(short mapId) {
        MapView mapView = Bukkit.getServer().getMap(mapId);

        for (MapRenderer r : mapView.getRenderers()) {
            if (!r.getClass().getPackage().getName().startsWith("org.bukkit")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Convenience routine.  Get the map view that the player is holding, if any.
     *
     * @param player The player to check for
     * @return A SMSMapView object if the player is holding one, null otherwise
     */
    public SMSMapView getHeldMapView(Player player) {
        if (player.getItemInHand().getType() == Material.MAP) {
            return getMapViewForId(player.getItemInHand().getDurability());
        } else {
            return null;
        }
    }

    /**
     * Get the item frame attached to the given block, if any, on the
     * side of the block facing most directly toward the given location
     * (typically a player's eye location).
     * <p/>
     * The item frame must be holding a map which is a SMSMapView.
     *
     * @param block     the block to check
     * @param viewerLoc the location to check from
     * @return the item frame object, or null if none was found
     */
    public ItemFrame getMapFrame(Block block, Location viewerLoc) {
        BlockFace face = BlockUtil.getNearestFace(block, viewerLoc);
        for (Entity entity : block.getWorld().getEntitiesByClass(ItemFrame.class)) {
            ItemFrame frame = (ItemFrame) entity;
            if (frame.getItem() == null || frame.getItem().getType() != Material.MAP || !checkForMapId(frame.getItem().getDurability())) {
                continue;
            }
            if (frame.getLocation().getBlock().getRelative(frame.getAttachedFace()).equals(block) && frame.getAttachedFace() == face.getOppositeFace()) {
                return frame;
            }
        }
        return null;
    }

    /**
     * Convenience method.  Create a new inventory view for the given menu.
     *
     * @param menu  the menu to add the new view to
     * @param owner owner of the new view
     * @return the newly-created view
     */
    public SMSInventoryView addInventoryViewToMenu(SMSMenu menu, CommandSender owner) {
        return addInventoryViewToMenu(null, menu, owner);
    }

    public SMSInventoryView addInventoryViewToMenu(String viewName, SMSMenu menu, CommandSender owner) {
        SMSInventoryView view = new SMSInventoryView(viewName, menu);
        registerView(view);
        view.setAttribute(SMSView.OWNER, view.makeOwnerName(owner));
        view.setOwnerId(getUniqueId(owner));
        view.update(view.getNativeMenu(), SMSMenuAction.REPAINT);
        return view;
    }

    /**
     * Convenience method.  Create a new multi-sign view at the given location.
     *
     * @param viewName name for the new view
     * @param menu     the menu to add the new view to
     * @param location location of one of the signs in the new view
     * @param owner    owner of the new view
     * @return the newly-created view
     * @throws SMSException
     */
    public SMSView addMultiSignToMenu(String viewName, SMSMenu menu, Location location, CommandSender owner) throws SMSException {
        SMSView view = new SMSMultiSignView(viewName, menu, location);
        registerView(view);
        view.setAttribute(SMSView.OWNER, view.makeOwnerName(owner));
        view.setOwnerId(getUniqueId(owner));
        view.update(menu, SMSMenuAction.REPAINT);
        return view;
    }

    /**
     * Convenience method.  Create a new multi-sign view at the given location.
     *
     * @param menu     the menu to add the view to
     * @param location location of one of the signs in the view
     * @param owner    owner of the new view
     * @return the newly-created view
     * @throws SMSException
     */
    public SMSView addMultiSignToMenu(SMSMenu menu, Location location, CommandSender owner) throws SMSException {
        return addMultiSignToMenu(null, menu, location, owner);
    }

    /**
     * Convenience method.  Create a new redstone view at the given location and add it
     * to the given menu.
     *
     * @param menu The menu to add the view to.
     * @param loc  The location for the view.
     * @throws SMSException if the location is not suitable for this view
     */
    public SMSView addRedstoneViewToMenu(String viewName, SMSMenu menu, Location loc, CommandSender owner) throws SMSException {
        SMSView view = new SMSRedstoneView(viewName, menu);
        view.addLocation(loc);
        registerView(view);
        view.setAttribute(SMSView.OWNER, view.makeOwnerName(owner));
        view.setOwnerId(getUniqueId(owner));
        return view;
    }

    public SMSView addRedstoneViewToMenu(SMSMenu menu, Location loc, CommandSender owner) throws SMSException {
        return addRedstoneViewToMenu(null, menu, loc, owner);
    }


    /**
     * Convenience method.  Create and register a new SMSSignView object, and attach it to
     * the given menu.  A sign must already exist at the given location, and it must not be
     * an already-existing view.
     *
     * @param menu The menu to attach the new view to
     * @param loc  Location of the new view
     * @return The newly-created view
     * @throws SMSException if the given location is not a suitable location for a new view
     */
    public SMSView addSignToMenu(String viewName, SMSMenu menu, Location loc, CommandSender owner) throws SMSException {
        SMSView view = new SMSSignView(viewName, menu, loc);
        registerView(view);
        view.setAttribute(SMSView.OWNER, view.makeOwnerName(owner));
        view.setOwnerId(getUniqueId(owner));
        view.update(menu, SMSMenuAction.REPAINT);
        return view;
    }

    public SMSView addSignToMenu(SMSMenu menu, Location loc, CommandSender owner) throws SMSException {
        return addSignToMenu(null, menu, loc, owner);
    }

    /**
     * Convenience method.  Create a new spout view and add it to the given menu.
     *
     * @param menu  the menu to add the view to
     * @param owner the owner of the view
     * @return the view that was just created
     * @throws SMSException
     */
    public SMSView addSpoutViewToMenu(SMSMenu menu, CommandSender owner) throws SMSException {
        return addSpoutViewToMenu(null, menu, owner);
    }

    public SMSView addSpoutViewToMenu(String viewName, SMSMenu menu, CommandSender owner) throws SMSException {
        SMSView view = new SMSSpoutView(viewName, menu);
        registerView(view);
        view.setAttribute(SMSView.OWNER, view.makeOwnerName(owner));
        view.setOwnerId(getUniqueId(owner));
        view.update(menu, SMSMenuAction.REPAINT);
        return view;
    }

    private UUID getUniqueId(CommandSender owner) {
        return owner instanceof Player ? ((Player) owner).getUniqueId() : null;
    }
}
