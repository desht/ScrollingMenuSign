package me.desht.scrollingmenusign.views;

import me.desht.dhutils.*;
import me.desht.dhutils.block.BlockUtil;
import me.desht.scrollingmenusign.*;
import me.desht.scrollingmenusign.enums.SMSAccessRights;
import me.desht.scrollingmenusign.enums.SMSUserAction;
import me.desht.scrollingmenusign.enums.ViewJustification;
import me.desht.scrollingmenusign.util.Substitutions;
import me.desht.scrollingmenusign.views.action.MenuDeleteAction;
import me.desht.scrollingmenusign.views.action.RepaintAction;
import me.desht.scrollingmenusign.views.action.ViewUpdateAction;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.util.Vector;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.*;

/**
 * Represents a base menu view from which all concrete views will inherit.
 */
public abstract class SMSView extends CommandTrigger implements Observer, SMSPersistable, ConfigurationListener, SMSInteractableBlock {
    // operations which were player-specific (active submenu, scroll position...)
    // need to be handled with a single global "player" here...
    protected static final String GLOBAL_PSEUDO_PLAYER = "&&global";

    // view attribute names
    public static final String OWNER = "owner";
    public static final String GROUP = "group";
    public static final String ITEM_JUSTIFY = "item_justify";
    public static final String TITLE_JUSTIFY = "title_justify";
    public static final String ACCESS = "access";

    private final SMSMenu menu;
    private final Set<PersistableLocation> locations = new HashSet<PersistableLocation>();
    private final String name;
    private final AttributeCollection attributes;   // view attributes to be displayed and/or edited by players
    private final Map<String, String> variables;    // view variables
    private final Map<UUID, MenuStack> menuStack;   // map player ID to menu stack (submenu support)

    private boolean autosave;
    private boolean dirty;
    private int maxLocations;
    private UUID ownerId;
    private boolean inThaw;

    // we can't use a Set here, since there are three possible values: 1) dirty, 2) clean, 3) unknown
    private final Map<UUID, Boolean> dirtyPlayers = new HashMap<UUID, Boolean>();
    // map a world name (for a world which hasn't been loaded yet) to a list of x,y,z positions
    private final Map<String, List<Vector>> deferredLocations = new HashMap<String, List<Vector>>();

    /**
     * Get a user-friendly string representing the type of this view.
     *
     * @return The type of view this is.
     */
    public abstract String getType();

    public SMSView(SMSMenu menu) {
        this(null, menu);
    }

    public SMSView(String name, SMSMenu menu) {
        if (name == null) {
            name = makeUniqueName(menu.getName());
        }
        this.name = name;
        this.menu = menu;
        this.dirty = true;
        this.autosave = true;
        this.attributes = new AttributeCollection(this);
        this.variables = new HashMap<String, String>();
        this.maxLocations = 1;
        this.menuStack = new HashMap<UUID, MenuStack>();

        attributes.registerAttribute(OWNER, ScrollingMenuSign.CONSOLE_OWNER, "Player who owns this view");
        attributes.registerAttribute(GROUP, "", "Permission group for this view");
        attributes.registerAttribute(TITLE_JUSTIFY, ViewJustification.DEFAULT, "Horizontal title positioning");
        attributes.registerAttribute(ITEM_JUSTIFY, ViewJustification.DEFAULT, "Horizontal item positioning");
        attributes.registerAttribute(ACCESS, SMSAccessRights.ANY, "Who may use this view");
    }

    private String makeUniqueName(String base) {
        int idx = 1;
        String s = String.format("%s-%d", base, idx);
        while (ScrollingMenuSign.getInstance().getViewManager().checkForView(s)) {
            idx++;
            s = String.format("%s-%d", base, idx);
        }
        return s;
    }

    /**
     * Get the view's autosave status - will the view be automatically saved to disk when modified?
     *
     * @return true if the view will be autosaved, false otherwise
     */
    public boolean isAutosave() {
        return autosave;
    }

    /**
     * Set the view's autosave status - will the view be automatically saved to disk when modified?
     *
     * @param autosave true if the view will be autosaved, false otherwise
     */
    public void setAutosave(boolean autosave) {
        this.autosave = autosave;
    }

    /* (non-Javadoc)
     * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
     */
    @Override
    public void update(Observable o, Object arg1) {
        if (o == null) {
            return;
        }
        SMSMenu m = (SMSMenu) o;
        ViewUpdateAction vu = ViewUpdateAction.getAction(arg1);
        Debugger.getInstance().debug("update: view=" + getName() + " action=" + vu.getClass().getSimpleName()
                + " player=" + vu.getSender() + " menu=" + m.getName() + ", nativemenu=" + getNativeMenu().getName());
        if (m == getNativeMenu()) {
            if (vu instanceof MenuDeleteAction) {
                ScrollingMenuSign.getInstance().getViewManager().deleteView(this, ((MenuDeleteAction) vu).isPermanent());
            }
        }
    }

    /* (non-Javadoc)
     * @see me.desht.scrollingmenusign.Freezable#getName()
     */
    /* (non-Javadoc)
	 * @see me.desht.scrollingmenusign.views.CommandTrigger#getName()
	 */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Get the native menu associated with the view.  The native menu is the menu that
     * the view was originally created for.
     *
     * @return The native SMSMenu object for this view.
     */
    public SMSMenu getNativeMenu() {
        return menu;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
        if (ownerId == null || ownerId.equals(ScrollingMenuSign.CONSOLE_UUID)) {
            setAttribute(OWNER, ScrollingMenuSign.CONSOLE_OWNER);
        } else {
            String name = Bukkit.getOfflinePlayer(ownerId).getName();
            setAttribute(OWNER, name == null ? "???" : name);
        }
        autosave();
    }

    /**
     * Get the currently active menu for this view for the given player.  This is not necessarily the
     * same as the view's native menu, if the player has a submenu open in this view.
     *
     * @return the active SMSMenu object for this view
     */
    public SMSMenu getActiveMenu(Player player) {
        UUID key = getPlayerContext(player);

        if (!menuStack.containsKey(key)) {
            menuStack.put(key, new MenuStack());
        }
        MenuStack mst = menuStack.get(key);
        return mst.isEmpty() ? getNativeMenu() : mst.peek();
    }

    /**
     * Push the given menu onto the view, making it the active menu as returned by {@link #getActiveMenu(Player)}
     *
     * @param player    player to push the menu for
     * @param newActive the menu to make active
     */
    public void pushMenu(Player player, SMSMenu newActive) {
        UUID key = getPlayerContext(player);

        getActiveMenu(player).deleteObserver(this);
        if (!menuStack.containsKey(key)) {
            menuStack.put(key, new MenuStack());
        }
        menuStack.get(key).pushMenu(newActive);
        newActive.addObserver(this);
        Debugger.getInstance().debug("pushed menu " + newActive.getName() + " onto " + getName()
                + " player=" + player.getName() + " player-context = " + key);
        update(newActive, new RepaintAction());
    }

    /**
     * Pop the active menu off the view, making the previously active menu the new active menu.
     *
     * @param player player to pop the menu for
     * @return the active menu that has just been popped off
     */
    public SMSMenu popMenu(Player player) {
        UUID key = getPlayerContext(player);

        if (!menuStack.containsKey(key)) {
            menuStack.put(key, new MenuStack());
        }
        MenuStack mst = menuStack.get(key);
        SMSMenu oldActive = mst.popMenu();
        if (oldActive == null) {
            return null;
        }
        oldActive.deleteObserver(this);
        SMSMenu newActive = getActiveMenu(player);
        newActive.addObserver(this);
        Debugger.getInstance().debug("popped menu " + oldActive.getName() + " off " + getName() + " new active = " + newActive.getName()
                + " player=" + player.getName() + " player-context = " + key);
        update(newActive, new RepaintAction());
        return oldActive;
    }

    /**
     * Get the set of player IDs who have a submenu open for this view.
     *
     * @return a set of player IDs who have a submenu open for this view
     */
    public Set<UUID> getSubmenuPlayers() {
        return menuStack.keySet();
    }

    MenuStack getMenuStack(UUID playerId) {
        return menuStack.get(playerId);
    }

    @Override
    public String getActiveItemLabel(Player player, int pos) {
        String label = super.getActiveItemLabel(player, pos);
        return label == null ? null : doVariableSubstitutions(player, label);
    }

    /**
     * Set an arbitrary string of tagged data on this view.
     *
     * @param key the variable name (must contain only alphanumeric or underscore)
     * @param val the variable value (may contain any character)
     */
    public void setVariable(String key, String val) {
        SMSValidate.isTrue(key.matches("[A-Za-z0-9_]+"), "Invalid variable name: " + key);
        if (val == null) {
            variables.remove(key);
        } else {
            variables.put(key, val);
        }
    }

    /**
     * Get an arbitrary string of tagged data from this view
     *
     * @param key the variable name (must contain only alphanumeric or underscore)
     * @return the variable value
     */
    public String getVariable(String key) {
        SMSValidate.isTrue(key.matches("[A-Za-z0-9_]+"), "Invalid variable name: " + key);
        SMSValidate.isTrue(variables.containsKey(key), "View " + getName() + " has no variable: " + key);
        return variables.get(key);
    }

    /**
     * Check if the given view variable exists in this view.
     *
     * @param key the variable name (must contain only alphanumeric or underscore)
     * @return true if the variable exists, false otherwise
     */
    public boolean checkVariable(String key) {
        return variables.containsKey(key);
    }

    /**
     * Get a list of all variable names for this view.
     *
     * @return a list of variable names for this view
     */
    public Set<String> listVariables() {
        return variables.keySet();
    }

    /**
     * Get the justification for menu items in this view
     *
     * @return the justification for menu items in this view
     */
    public ViewJustification getItemJustification() {
        return getJustification("sms.item_justify", ITEM_JUSTIFY, ViewJustification.LEFT);
    }

    /**
     * Get the justification for the menu title in this view
     *
     * @return the justification for the menu title in this view
     */
    public ViewJustification getTitleJustification() {
        return getJustification("sms.title_justify", TITLE_JUSTIFY, ViewJustification.CENTER);
    }

    private ViewJustification getJustification(String configItem, String attrName, ViewJustification fallback) {
        ViewJustification viewJust = (ViewJustification) getAttribute(attrName);
        if (viewJust != ViewJustification.DEFAULT) {
            return viewJust;
        }

        String j = ScrollingMenuSign.getInstance().getConfig().getString(configItem, fallback.toString());
        try {
            return ViewJustification.valueOf(j.toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    public String doVariableSubstitutions(Player player, String text) {
        String s = Substitutions.viewVariableSubs(this, text);
        s = Substitutions.userVariableSubs(player, s, null);
        return s;
    }

    public List<String> doVariableSubstitutions(Player player, List<String> l) {
        List<String> res = new ArrayList<String>(l.size());
        for (String s : l) {
            res.add(doVariableSubstitutions(player, s));
        }
        return res;
    }

    public Map<String, Object> freeze() {
        Map<String, Object> map = new HashMap<String, Object>();
        Map<String, String> vars = new HashMap<String, String>();

        map.put("name", name);
        map.put("menu", menu.getName());
        map.put("class", getClass().getName());
        map.put("owner_id", getOwnerId() == null ? "" : getOwnerId().toString());
        for (String key : listAttributeKeys(false)) {
            map.put(key, attributes.get(key).toString());
        }
        List<PersistableLocation> locs = new ArrayList<PersistableLocation>();
        for (Location l : getLocations()) {
            PersistableLocation pl = new PersistableLocation(l);
            pl.setSavePitchAndYaw(false);
            locs.add(pl);
        }
        map.put("locations", locs);
        for (String key : listVariables()) {
            vars.put(key, getVariable(key));
        }
        map.put("vars", vars);
        return map;
    }

    @SuppressWarnings("unchecked")
    protected void thaw(ConfigurationSection node) throws SMSException {
        inThaw = true;

        List<Object> locs = (List<Object>) node.getList("locations");
        for (Object o : locs) {
            if (o instanceof PersistableLocation) {
                PersistableLocation pl = (PersistableLocation) o;
                try {
                    addLocation(pl.getLocation());
                } catch (IllegalStateException e) {
                    // world not loaded? we'll defer adding this location to the view for now
                    // perhaps the world will get loaded later
                    addDeferredLocation(pl.getWorldName(), new Vector(pl.getX(), pl.getY(), pl.getZ()));
                }
            } else {
                throw new SMSException("invalid location in view " + getName() + " (corrupted file?)");
            }
        }

        String id = node.getString("owner_id");
        if (id != null && !id.isEmpty()) {
            setOwnerId(UUID.fromString(id));
        }

        // temporarily disable validation while attributes are loaded from saved data
        attributes.setValidate(false);
        for (String key : node.getKeys(false)) {
            if (!node.isConfigurationSection(key) && attributes.hasAttribute(key)) {
                String val = node.getString(key);
                try {
                    setAttribute(key, val);
                } catch (SMSException e) {
                    LogUtils.warning("View " + getName() + ": can't set " + key + "='" + val + "': " + e.getMessage());
                }
            }
        }
        // ensure view has an owner (pre-2.0, views did not)
        String owner = getAttributeAsString(OWNER);
        if (owner.isEmpty()) {
            setAttribute(OWNER, getNativeMenu().getOwner());
        }

        ConfigurationSection vars = node.getConfigurationSection("vars");
        if (vars != null) {
            for (String k : vars.getKeys(false)) {
                setVariable(k, vars.getString(k));
            }
        }
        attributes.setValidate(true);
        inThaw = false;
    }

    /**
     * Mark a location (actually a world name and a x,y,z vector) as deferred - the world isn't
     * currently available.
     *
     * @param worldName name of the world
     * @param v         a vector describing the location
     */
    private void addDeferredLocation(String worldName, Vector v) {
        List<Vector> l = deferredLocations.get(worldName);
        if (l == null) {
            l = new ArrayList<Vector>();
            deferredLocations.put(worldName, l);
        }
        l.add(v);
    }

    /**
     * Get a list of the locations (x,y,z vectors) that have been deferred for the given world name.
     *
     * @param worldName the name of the world to check for
     * @return a list of vectors; the locations that have been deferred
     */
    public List<Vector> getDeferredLocations(String worldName) {
        return deferredLocations.get(worldName);
    }

    /**
     * Get the "dirty" status for this view - whether or not a repaint is needed for all players.
     *
     * @return true if a repaint is needed, false otherwise
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * Get the "dirty" status for this view - whether or not a repaint is needed for the given player.
     *
     * @param player The player to check for
     * @return true if a repaint is needed, false otherwise
     */
    public boolean isDirty(Player player) {
        UUID key = getPlayerContext(player);
        return dirtyPlayers.containsKey(key) ? dirtyPlayers.get(key) : dirty;
    }

    /**
     * Set the global "dirty" status for this view - whether or not a repaint is needed for all players.
     *
     * @param dirty true if a repaint is needed, false otherwise
     */
    public void setDirty(boolean dirty) {
        setDirty(null, dirty);
    }

    /**
     * Set the per-player "dirty" status for this view - whether or not a repaint is needed for the given player.
     * If a null player is passed, the view is marked as dirty for all players.
     *
     * @param player The player to check for, may be null
     * @param dirty  Whether or not a repaint is needed
     */
    public void setDirty(Player player, boolean dirty) {
        if (player == null) {
            this.dirty = dirty;
            if (dirty) {
                dirtyPlayers.clear();
            }
        } else {
            dirtyPlayers.put(getPlayerContext(player), dirty);
        }
    }

    /**
     * Get a set of all locations for this view.  Views may have zero or more locations (e.g. a sign
     * view has one location, a map view has zero locations, a multisign view has several locations...)
     *
     * @return A Set of all locations for this view object
     * @throws IllegalStateException if the world for this view has become unloaded
     */
    public Set<Location> getLocations() {
        Set<Location> res = new HashSet<Location>();
        for (PersistableLocation l : locations) {
            res.add(l.getLocation());
        }
        return res;
    }

    /**
     * Get a list of all locations for this view as a Java array.
     *
     * @return An array of all locations for this view object
     */
    public Location[] getLocationsArray() {
        Set<Location> locs = getLocations();
        return locs.toArray(new Location[locs.size()]);
    }

    /**
     * Set the maximum number of locations which are allowed.  Subclass constructors should call this
     * as appropriate.
     *
     * @param maxLocations the maximum number of locations to be allowed
     */
    protected void setMaxLocations(int maxLocations) {
        this.maxLocations = maxLocations;
    }

    /**
     * Get the maximum number of locations that this view may occupy.
     *
     * @return The maximum number of locations
     */
    public int getMaxLocations() {
        return maxLocations;
    }

    /**
     * Build the name of the player (or possibly the console) which owns this view.
     *
     * @param sender the command sender object
     * @return the name of the command sender
     */
    String makeOwnerName(CommandSender sender) {
        return sender != null && sender instanceof Player ? sender.getName() : ScrollingMenuSign.CONSOLE_OWNER;
    }

    /**
     * Register a new location as being part of this view object
     *
     * @param loc The location to register
     * @throws SMSException if the location is not suitable for adding to this view
     */
    public void addLocation(Location loc) throws SMSException {
        SMSValidate.isTrue(getLocations().size() < getMaxLocations(),
                "View " + getName() + " already occupies the maximum number of locations (" + getMaxLocations() + ")");

        ViewManager viewManager = ScrollingMenuSign.getInstance().getViewManager();
        SMSView v = viewManager.getViewForLocation(loc);
        if (v != null) {
            throw new SMSException("Location " + MiscUtil.formatLocation(loc) + " already contains a view on menu: " + v.getNativeMenu().getName());
        }
        locations.add(new PersistableLocation(loc));
        if (viewManager.checkForView(getName())) {
            viewManager.registerLocation(loc, this);
        }
        autosave();
    }

    /**
     * Unregister a location from the given view.
     *
     * @param loc The location to unregister.
     */
    public void removeLocation(Location loc) {
        ViewManager viewManager = ScrollingMenuSign.getInstance().getViewManager();
        locations.remove(new PersistableLocation(loc));
        viewManager.unregisterLocation(loc);

        autosave();
    }

    /**
     * Save this view's contents to disk (if autosaving is enabled, and the view
     * is registered).
     */
    public void autosave() {
        if (isAutosave() && ScrollingMenuSign.getInstance().getViewManager().checkForView(getName()))
            SMSPersistence.save(this);
    }

    /* (non-Javadoc)
     * @see me.desht.scrollingmenusign.SMSPersistable#getSaveFolder()
     */
    public File getSaveFolder() {
        return DirectoryStructure.getViewsFolder();
    }

    /**
     * Check if the given player has access rights for this view.
     *
     * @param player The player to check
     * @return True if the player may use this view, false if not
     */
    public boolean hasOwnerPermission(Player player) {
        if (!getActiveMenu(player).hasOwnerPermission(player)) {
            return false;
        }
        SMSAccessRights access = (SMSAccessRights) getAttribute(ACCESS);
        return access.isAllowedToUse(player, ownerId, getAttributeAsString(OWNER), getAttributeAsString(GROUP));
    }

    /**
     * Check if this view is owned by the given player.
     *
     * @param player the player to check
     * @return true if the view is owned by the player, false otherwise
     */
    public boolean isOwnedBy(Player player) {
        return player.getUniqueId().equals(ownerId);
    }

    /**
     * Require that the given command sender is allowed to use this view, and throw a SMSException if not.
     *
     * @param sender the command sender to check
     * @throws SMSException if the command sender is not allowed to use this view
     */
    public void ensureAllowedToUse(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (!hasOwnerPermission(player)) {
                throw new SMSException("That menu view is private to someone else.");
            }
            if (!isTypeUsable(player)) {
                throw new SMSException("You don't have permission to use " + getType() + " menu views.");
            }
        }
    }

    protected boolean isTypeUsable(Player player) {
        return PermissionUtils.isAllowedTo(player, "scrollingmenusign.use." + getType());
    }

    /**
     * Require that the given command sender is allowed to modify this view, and throw a SMSException if not.
     *
     * @param sender the command sender to check
     * @throws SMSException if the command sender is not allowed to modify this view
     */
    public void ensureAllowedToModify(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (!PermissionUtils.isAllowedTo(player, "scrollingmenusign.edit.any") && !isOwnedBy(player)) {
                throw new SMSException("That view is owned by someone else.");
            }
        }
    }

    protected void registerAttribute(String attr, Object def, String desc) {
        attributes.registerAttribute(attr, def, desc);
    }

    protected void registerAttribute(String attr, Object def) {
        attributes.registerAttribute(attr, def);
    }

    public AttributeCollection getAttributes() {
        return attributes;
    }

    public Object getAttribute(String k) {
        return attributes.get(k);
    }

    public String getAttributeAsString(String k, String def) {
        Object o = getAttribute(k);
        return o == null || o.toString().isEmpty() ? def : o.toString();
    }

    public String getAttributeAsString(String k) {
        return getAttributeAsString(k, "");
    }

    public void setAttribute(String k, String val) throws SMSException {
        SMSValidate.isTrue(attributes.contains(k), "No such view attribute: " + k);
        attributes.set(k, val);
    }

    public Set<String> listAttributeKeys(boolean isSorted) {
        return attributes.listAttributeKeys(isSorted);
    }

    /* (non-Javadoc)
     * @see me.desht.dhutils.ConfigurationListener#onConfigurationChanged(me.desht.dhutils.ConfigurationManager, java.lang.String, java.lang.Object, java.lang.Object)
     */
    @Override
    public void onConfigurationChanged(ConfigurationManager configurationManager, String key, Object oldVal, Object newVal) {
        if (key.equals(OWNER) && !inThaw) {
            // try to get the owner UUID matching the owner name if possible
            final String owner = newVal.toString();
            if (owner.isEmpty() || owner.equals(ScrollingMenuSign.CONSOLE_OWNER)) {
                ownerId = ScrollingMenuSign.CONSOLE_UUID;
            } else if (MiscUtil.looksLikeUUID(owner)) {
                ownerId = UUID.fromString(owner);
                String name = Bukkit.getOfflinePlayer(ownerId).getName();
                setAttribute(OWNER, name == null ? "?" : name);
            } else if (!owner.equals("?")) {
                @SuppressWarnings("deprecation") Player p = Bukkit.getPlayer(owner);
                if (p != null) {
                    ownerId = p.getUniqueId();
                } else {
                    updateOwnerAsync(owner);
                }
            }
        }

        // Don't do updates on views that haven't been registered yet (which will be the case
        // when restoring saved views from disk)
        if (ScrollingMenuSign.getInstance().getViewManager().checkForView(getName())) {
            update(null, new RepaintAction());
        }
    }

    private void updateOwnerAsync(final String owner) {
        final UUIDFetcher uf = new UUIDFetcher(Arrays.asList(owner));
        Bukkit.getScheduler().runTaskAsynchronously(ScrollingMenuSign.getInstance(), new Runnable() {
            @Override
            public void run() {
                try {
                    Map<String,UUID> res = uf.call();
                    if (res.containsKey(owner)) {
                        ownerId = res.get(owner);
                    } else {
                        LogUtils.warning("View [" + getName() + "]: no known UUID for player: " + owner);
                        ownerId = ScrollingMenuSign.CONSOLE_UUID;
                    }
                } catch (Exception e) {
                    LogUtils.warning("View [" + getName() + "]: can't retrieve UUID for player: " + owner + ": " + e.getMessage());
                }
            }
        });
    }

    /* (non-Javadoc)
     * @see me.desht.dhutils.ConfigurationListener#onConfigurationValidate(me.desht.dhutils.ConfigurationManager, java.lang.String, java.lang.String)
     */
    @Override
    public Object onConfigurationValidate(ConfigurationManager configurationManager, String key, Object oldVal, Object newVal) {
        if (key.equals(ACCESS)) {
            SMSAccessRights access = (SMSAccessRights) newVal;
            if (access != SMSAccessRights.ANY && ownerId == null) {
                throw new SMSException("View must be owned by a player to change access control to " + access);
            } else if (access == SMSAccessRights.GROUP && ScrollingMenuSign.permission == null) {
                throw new SMSException("Cannot use GROUP access control (no permission group support available)");
            }
        }
        return newVal;
    }

    /**
     * Erase the view's contents and perform any housekeeping; called when it's about to be deleted.
     */
    public void onDeleted(boolean permanent) {
        // does nothing by default: override in subclasses
    }

    /**
     * Called automatically when the view is used to execute a menu item.  Override and extend this
     * in subclasses.
     *
     * @param player The player who did the execution
     */
    public void onExecuted(Player player) {
        // does nothing by default: override in subclasses
    }

    /**
     * Called automatically when the view is scrolled.  Override and extend this
     * in subclasses.
     *
     * @param player The player who did the scrolling
     * @param action The scroll direction: SCROLLDOWN or SCROLLUP
     */
    public void onScrolled(Player player, SMSUserAction action) {
        // does nothing by default: override in subclasses
    }

    /**
     * Called automatically when a player logs out.  Perform any cleardown work to remove player
     * records from the view.  Override and extend this in subclasses.
     *
     * @param player The player who logged out
     */
    public void clearPlayerForView(Player player) {
        // does nothing by default: override in subclasses
    }

    public void processEvent(ScrollingMenuSign plugin, BlockDamageEvent event) {
        Block b = event.getBlock();
        Player player = event.getPlayer();

        SMSMenu menu = getNativeMenu();
        if (Debugger.getInstance().getLevel() > 0) {
            Debugger.getInstance().debug("block damage event @ " + MiscUtil.formatLocation(b.getLocation()) + ", view = " + getName() + ", menu=" + menu.getName());
        }
        if (plugin.getConfigCache().isBreakProtected() ||
                !menu.isOwnedBy(player) && !PermissionUtils.isAllowedTo(player, "scrollingmenusign.edit.any")) {
            event.setCancelled(true);
        }
    }

    public void processEvent(ScrollingMenuSign plugin, BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block b = event.getBlock();

        if (Debugger.getInstance().getLevel() > 0) {
            Debugger.getInstance().debug("block break event @ " + b.getLocation() + ", view = " + getName() + ", menu=" + getNativeMenu().getName());
        }

        if (plugin.getConfigCache().isBreakProtected()) {
            event.setCancelled(true);
            update(getActiveMenu(player), new RepaintAction());
        } else {
            removeLocation(b.getLocation());
            if (getLocations().isEmpty()) {
                plugin.getViewManager().deleteView(this, true);
            }
            MiscUtil.statusMessage(player,
                    String.format("%s block @ &f%s&- was removed from view &e%s&- (menu &e%s&-).",
                            b.getType(), MiscUtil.formatLocation(b.getLocation()), getName(), getNativeMenu().getName()));
        }
    }

    public void processEvent(ScrollingMenuSign plugin, BlockPhysicsEvent event) {
        Block b = event.getBlock();

        if (Debugger.getInstance().getLevel() > 0) {
            Debugger.getInstance().debug("block physics event @ " + b.getLocation() + ", view = " + getName() + ", menu=" + getNativeMenu().getName());
        }
        if (plugin.getConfigCache().isPhysicsProtected()) {
            event.setCancelled(true);
        } else if (BlockUtil.isAttachableDetached(b)) {
            // attached to air? looks like the sign (or other attachable) has become detached
            // NOTE: for multi-block views, the loss of *any* block due to physics causes the view to be removed
            LogUtils.info("Attachable view block " + getName() + " @ " + b.getLocation() + " has become detached: deleting");
            plugin.getViewManager().deleteView(this, true);
        }
    }

    public void processEvent(ScrollingMenuSign plugin, BlockRedstoneEvent event) {
        Block b = event.getBlock();

        if (Debugger.getInstance().getLevel() > 0) {
            Debugger.getInstance().debug("block redstone event @ " + b.getLocation() + ", view = "
                    + getName() + ", menu = " + getNativeMenu().getName()
                    + ", current = " + event.getOldCurrent() + "->" + event.getNewCurrent());
        }
    }

    /**
     * Get a view by name.  Backwards-compatibility for other plugins which need it.
     *
     * @param viewName name of the view to get
     * @return the view object
     * @throws SMSException if there is no such view
     * @deprecated use ViewManager#getView(String)
     */
    @Deprecated
    public static SMSView getView(String viewName) {
        return ScrollingMenuSign.getInstance().getViewManager().getView(viewName);
    }

    /**
     * Register a view with the view manager.  Backwards-compatibility for other plugins which need it.
     *
     * @deprecated use ViewManager#registerView(SMSView)
     */
    @Deprecated
    public void register() {
        ScrollingMenuSign.getInstance().getViewManager().registerView(this);
    }

    /**
     * Check if this view responds to player clicks to scroll/execute it.
     *
     * @return true if the view is responsive to clicks, false otherwise
     */
    public boolean isClickable() {
        return true;
    }

    /**
     * Represents a stack of menus, for submenu support.  The currently-active
     * menu is at the top of the stack.
     */
    public class MenuStack {
        final Deque<WeakReference<SMSMenu>> stack;

        public MenuStack() {
            stack = new ArrayDeque<WeakReference<SMSMenu>>();
        }

        public void pushMenu(SMSMenu menu) {
            stack.push(new WeakReference<SMSMenu>(menu));
        }

        public SMSMenu popMenu() {
            return stack.pop().get();
        }

        public SMSMenu peek() {
            return stack.peek().get();
        }

        public boolean isEmpty() {
            return stack.isEmpty();
        }
    }
}
