package me.desht.scrollingmenusign;

import me.desht.dhutils.*;
import me.desht.scrollingmenusign.enums.SMSAccessRights;
import me.desht.scrollingmenusign.util.SMSUtil;
import me.desht.scrollingmenusign.views.action.MenuDeleteAction;
import me.desht.scrollingmenusign.views.action.RepaintAction;
import me.desht.scrollingmenusign.views.action.TitleAction;
import me.desht.scrollingmenusign.views.action.ViewUpdateAction;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.*;

/**
 * Represents a menu object
 */
public class SMSMenu extends Observable implements SMSPersistable, SMSUseLimitable, ConfigurationListener, Comparable<SMSMenu> {
    public static final String FAKE_SPACE = "\u203f";

    public static final String AUTOSORT = "autosort";
    public static final String DEFAULT_CMD = "defcmd";
    public static final String OWNER = "owner";
    public static final String TITLE = "title";
    public static final String ACCESS = "access";
    public static final String REPORT_USES = "report_uses";
    public static final String GROUP = "group";

    private final String name;
    private final List<SMSMenuItem> items = new ArrayList<SMSMenuItem>();
    private final Map<String, Integer> itemMap = new HashMap<String, Integer>();
    private final SMSRemainingUses uses;
    private final AttributeCollection attributes;    // menu attributes to be displayed and/or edited by players

    private String title;  // cache colour-parsed version of the title attribute
    private UUID ownerId;  // cache owner's UUID (could be null)
    private boolean autosave;
    private boolean inThaw;

    /**
     * Construct a new menu
     *
     * @param name  Name of the menu
     * @param title Title of the menu
     * @param owner Owner of the menu
     * @throws SMSException If there is already a menu at this location
     * @deprecated use {@link SMSMenu(String,String,Player)} or {@link SMSMenu(String,String, org.bukkit.plugin.Plugin )}
     */
    @Deprecated
    public SMSMenu(String name, String title, String owner) {
        this.name = name;
        this.uses = new SMSRemainingUses(this);
        this.attributes = new AttributeCollection(this);
        registerAttributes();
        setAttribute(OWNER, owner == null ? ScrollingMenuSign.CONSOLE_OWNER : owner);
        ownerId = ScrollingMenuSign.CONSOLE_UUID;
        setAttribute(TITLE, title);
        autosave = true;
    }

    /**
     * Construct a new menu
     *
     * @param name  Name of the menu
     * @param title Title of the menu
     * @param owner Owner of the menu
     * @throws SMSException If there is already a menu at this location
     */
    public SMSMenu(String name, String title, Player owner) {
        this.name = name;
        this.uses = new SMSRemainingUses(this);
        this.attributes = new AttributeCollection(this);
        registerAttributes();
        setAttribute(OWNER, owner == null ? ScrollingMenuSign.CONSOLE_OWNER : owner.getName());
        ownerId = owner == null ? ScrollingMenuSign.CONSOLE_UUID : owner.getUniqueId();
        setAttribute(TITLE, title);
        autosave = true;
    }

    /**
     * Construct a new menu
     *
     * @param name  Name of the menu
     * @param title Title of the menu
     * @param owner Owner of the menu
     * @throws SMSException If there is already a menu at this location
     */
    public SMSMenu(String name, String title, Plugin owner) {
        this.name = name;
        this.uses = new SMSRemainingUses(this);
        this.attributes = new AttributeCollection(this);
        registerAttributes();
        setAttribute(OWNER, owner == null ? ScrollingMenuSign.CONSOLE_OWNER : "[" + owner.getName() + "]");
        ownerId = ScrollingMenuSign.CONSOLE_UUID;
        setAttribute(TITLE, title);
        autosave = true;
    }

    /**
     * Construct a new menu from a frozen configuration object.
     *
     * @param node A ConfigurationSection containing the menu's properties
     * @throws SMSException If there is already a menu at this location
     */
    @SuppressWarnings("unchecked")
    public SMSMenu(ConfigurationSection node) {
        SMSPersistence.mustHaveField(node, "name");
        SMSPersistence.mustHaveField(node, "title");
        SMSPersistence.mustHaveField(node, "owner");

        inThaw = true;

        this.name = node.getString("name");
        this.uses = new SMSRemainingUses(this, node.getConfigurationSection("usesRemaining"));
        this.attributes = new AttributeCollection(this);
        registerAttributes();
        String id = node.getString("owner_id");
        if (id != null && !id.isEmpty()) {
            this.ownerId = UUID.fromString(id);
        } else {
            this.ownerId = ScrollingMenuSign.CONSOLE_UUID;
        }

        // migration of group -> owner_group access in 2.4.0
        if (!node.contains("group") && node.contains(ACCESS) && node.getString(ACCESS).equals("GROUP")) {
            LogUtils.info("menu " + name + ": migrate GROUP -> OWNER_GROUP access");
            node.set("access", "OWNER_GROUP");
        }

        for (String k : node.getKeys(false)) {
            if (!node.isConfigurationSection(k) && attributes.hasAttribute(k)) {
                setAttribute(k, node.getString(k));
            }
        }

        List<Map<String, Object>> items = (List<Map<String, Object>>) node.getList("items");
        if (items != null) {
            for (Map<String, Object> item : items) {
                MemoryConfiguration itemNode = new MemoryConfiguration();
                // need to expand here because the item may contain a usesRemaining object - item could contain a nested map
                SMSPersistence.expandMapIntoConfig(itemNode, item);
                SMSMenuItem menuItem = new SMSMenuItem(this, itemNode);
                SMSMenuItem actual = menuItem.uniqueItem();
                if (!actual.getLabel().equals(menuItem.getLabel()))
                    LogUtils.warning("Menu '" + getName() + "': duplicate item '" + menuItem.getLabelStripped() + "' renamed to '" + actual.getLabelStripped() + "'");
                addItem(actual);
            }
        }

        inThaw = false;
        autosave = true;
    }

    public void setAttribute(String k, String val) {
        SMSValidate.isTrue(attributes.contains(k), "No such view attribute: " + k);
        attributes.set(k, val);
    }

    private void registerAttributes() {
        attributes.registerAttribute(AUTOSORT, false, "Always keep the menu sorted?");
        attributes.registerAttribute(DEFAULT_CMD, "", "Default command to run if item has no command");
        attributes.registerAttribute(OWNER, "", "Player who owns this menu");
        attributes.registerAttribute(GROUP, "", "Permission group for this menu");
        attributes.registerAttribute(TITLE, "", "The menu's displayed title");
        attributes.registerAttribute(ACCESS, SMSAccessRights.ANY, "Who may use this menu");
        attributes.registerAttribute(REPORT_USES, true, "Tell the player when remaining uses have changed?");
    }

    public Map<String, Object> freeze() {
        HashMap<String, Object> map = new HashMap<String, Object>();

        List<Map<String, Object>> l = new ArrayList<Map<String, Object>>();
        for (SMSMenuItem item : items) {
            l.add(item.freeze());
        }
        for (String key : attributes.listAttributeKeys(false)) {
            if (key.equals(TITLE)) {
                map.put(key, SMSUtil.escape(attributes.get(key).toString()));
            } else {
                map.put(key, attributes.get(key).toString());
            }
        }
        map.put("name", getName());
        map.put("items", l);
        map.put("usesRemaining", uses.freeze());
        map.put("owner_id", getOwnerId() == null ? "" : getOwnerId().toString());

        return map;
    }

    public AttributeCollection getAttributes() {
        return attributes;
    }

    /**
     * Get the menu's unique name
     *
     * @return Name of this menu
     */
    public String getName() {
        return name;
    }

    /**
     * Get the menu's title string
     *
     * @return The title string
     */
    public String getTitle() {
        return title;
    }

    /**
     * Set the menu's title string
     *
     * @param newTitle The new title string
     */
    public void setTitle(String newTitle) {
        attributes.set(TITLE, newTitle);
    }

    /**
     * Get the menu's owner string
     *
     * @return Name of the menu's owner
     */
    public String getOwner() {
        return attributes.get(OWNER).toString();
    }

    /**
     * Set the menu's owner string.
     *
     * @param owner Name of the menu's owner
     */
    public void setOwner(String owner) {
        attributes.set(OWNER, owner);
    }

    /**
     * Get the menu's permission group.
     *
     * @return the group
     */
    public String getGroup() {
        return attributes.get(GROUP).toString();
    }

    /**
     * Set the menu's permission group.
     *
     * @param group Name of the menu's group
     */
    public void setGroup(String group) {
        attributes.set(GROUP, group);
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    /**
     * Get the menu's autosave status - will menus be automatically saved to disk when modified?
     *
     * @return true or false
     */
    public boolean isAutosave() {
        return autosave;
    }

    /**
     * Set the menu's autosave status - will menus be automatically saved to disk when modified?
     *
     * @param autosave true or false
     * @return the previous autosave status - true or false
     */
    public boolean setAutosave(boolean autosave) {
        boolean prevAutosave = this.autosave;
        this.autosave = autosave;
        if (autosave) {
            autosave();
        }
        return prevAutosave;
    }

    /**
     * Get the menu's autosort status - will menu items be automatically sorted when added?
     *
     * @return true or false
     */
    public boolean isAutosort() {
        return (Boolean) attributes.get(AUTOSORT);
    }

    /**
     * Set the menu's autosort status - will menu items be automatically sorted when added?
     *
     * @param autosort true or false
     */
    public void setAutosort(boolean autosort) {
        setAttribute(AUTOSORT, Boolean.toString(autosort));
    }

    /**
     * Get the menu's default command.  This command will be used if the menu item
     * being executed has a missing command.
     *
     * @return The default command string
     */
    public String getDefaultCommand() {
        return attributes.get(DEFAULT_CMD).toString();
    }

    /**
     * Set the menu's default command.  This command will be used if the menu item
     * being executed has a missing command.
     *
     * @param defaultCommand the default command to set
     */
    public void setDefaultCommand(String defaultCommand) {
        setAttribute(DEFAULT_CMD, defaultCommand);
    }

    /**
     * Get a list of all the items in the menu
     *
     * @return A list of the items
     */
    public List<SMSMenuItem> getItems() {
        return items;
    }

    /**
     * Get the number of items in the menu
     *
     * @return The number of items
     */
    public int getItemCount() {
        return items.size();
    }

    /**
     * Get the item at the given numeric index
     *
     * @param index 1-based numeric index
     * @return The menu item at that index or null if out of range and mustExist is false
     * @throws SMSException if the index is out of range and mustExist is true
     */
    public SMSMenuItem getItemAt(int index, boolean mustExist) {
        if (index < 1 || index > items.size()) {
            if (mustExist) {
                throw new SMSException("Index " + index + " out of range.");
            } else {
                return null;
            }
        } else {
            return items.get(index - 1);
        }
    }

    /**
     * Get the item at the given numeric index.
     *
     * @param index 1-based numeric index
     * @return the menu item at that index or null if out of range
     */
    public SMSMenuItem getItemAt(int index) {
        return getItemAt(index, false);
    }

    /**
     * Get the menu item matching the given label.
     *
     * @param wanted the label to match (case-insensitive)
     * @return the menu item with that label, or null if no matching item
     */
    public SMSMenuItem getItem(String wanted) {
        return getItem(wanted, false);
    }

    /**
     * Get the menu item matching the given label
     *
     * @param wanted    The label to match (case-insensitive)
     * @param mustExist If true and the label is not in the menu, throw an exception
     * @return The menu item with that label, or null if no matching item and mustExist is false
     * @throws SMSException if no matching item and mustExist is true
     */
    public SMSMenuItem getItem(String wanted, boolean mustExist) {
        if (items.size() != itemMap.size())
            rebuildItemMap();    // workaround for Heroes 1.4.8 which calls menu.getItems().clear

        Integer idx = itemMap.get(ChatColor.stripColor(wanted.replace(FAKE_SPACE, " ")));
        if (idx == null) {
            if (mustExist) {
                throw new SMSException("No such item '" + wanted + "' in menu " + getName());
            } else {
                return null;
            }
        }
        return getItemAt(idx);
    }

    /**
     * Get the index of the item matching the given label
     *
     * @param wanted The label to match (case-insensitive)
     * @return 1-based item index, or -1 if no matching item
     */
    public int indexOfItem(String wanted) {
        if (items.size() != itemMap.size())
            rebuildItemMap();    // workaround for Heroes 1.4.8 which calls menu.getItems().clear

        int index = -1;
        try {
            index = Integer.parseInt(wanted);
        } catch (NumberFormatException e) {
            String l = ChatColor.stripColor(wanted.replace(FAKE_SPACE, " "));
            if (itemMap.containsKey(l))
                index = itemMap.get(l);
        }
        return index;
    }

    /**
     * Append a new item to the menu
     *
     * @param label   Label of the item to add
     * @param command Command to be run when the item is selected
     * @param message Feedback text to be shown when the item is selected
     * @deprecated use {@link #addItem(SMSMenuItem)}
     */
    @Deprecated
    public void addItem(String label, String command, String message) {
        addItem(new SMSMenuItem(this, label, command, message));
    }

    /**
     * Append a new item to the menu
     *
     * @param item The item to be added
     */
    public void addItem(SMSMenuItem item) {
        insertItem(items.size() + 1, item);
    }

    /**
     * Insert new item in the menu, at the given position.
     *
     * @param pos     the position to insert at
     * @param label   label of the new item
     * @param command command to be run
     * @param message feedback message text
     * @deprecated use {@link #insertItem(int, SMSMenuItem)}
     */
    @Deprecated
    public void insertItem(int pos, String label, String command, String message) {
        insertItem(pos, new SMSMenuItem(this, label, command, message));
    }

    /**
     * Insert a new item in the menu, at the given position.
     *
     * @param item The item to insert
     * @param pos  The position to insert (1-based index)
     */
    public void insertItem(int pos, SMSMenuItem item) {
        if (items.size() != itemMap.size())
            rebuildItemMap();    // workaround for Heroes 1.4.8 which calls menu.getItems().clear

        if (item == null)
            throw new NullPointerException();
        String l = item.getLabelStripped();
        if (itemMap.containsKey(l)) {
            throw new SMSException("Duplicate label '" + l + "' not allowed in menu '" + getName() + "'.");
        }

        if (pos > items.size()) {
            items.add(item);
            itemMap.put(l, items.size());
        } else {
            items.add(pos - 1, item);
            rebuildItemMap();
        }

        if (isAutosort()) {
            Collections.sort(items);
            if (pos <= items.size()) rebuildItemMap();
        }

        setChanged();
        autosave();
    }

    /**
     * Replace an existing menu item.  The label must already be present in the menu,
     * or an exception will be thrown.
     *
     * @param label   Label of the menu item
     * @param command The command to be run
     * @param message The feedback message
     * @throws SMSException if the label isn't present in the menu
     * @deprecated use {@link #replaceItem(SMSMenuItem)}
     */
    @Deprecated
    public void replaceItem(String label, String command, String message) {
        replaceItem(new SMSMenuItem(this, label, command, message));
    }

    /**
     * Replace an existing menu item.  The new item's label must already be present in
     * the menu.
     *
     * @param item the replacement menu item
     * @throws SMSException if the new item's label isn't present in the menu
     */
    public void replaceItem(SMSMenuItem item) {
        if (items.size() != itemMap.size())
            rebuildItemMap();    // workaround for Heroes 1.4.8 which calls menu.getItems().clear

        String l = item.getLabelStripped();
        if (!itemMap.containsKey(l)) {
            throw new SMSException("Label '" + l + "' is not in the menu.");
        }
        int idx = itemMap.get(l);
        items.set(idx - 1, item);
        itemMap.put(l, idx);

        setChanged();
        autosave();
    }

    /**
     * Replace the menu item at the given 1-based position.  The new label must not already be
     * present in the menu or an exception will be thrown - duplicates are not allowed.
     *
     * @param pos     the position to replace at
     * @param label   label of the replacement item
     * @param command command for the replacement item
     * @param message feedback message text for the replacement item
     * @deprecated use {@link #replaceItem(int, SMSMenuItem)}
     */
    @Deprecated
    public void replaceItem(int pos, String label, String command, String message) {
        replaceItem(pos, new SMSMenuItem(this, label, command, message));
    }

    /**
     * Replace the menu item at the given 1-based position.  The new label must not already be
     * present in the menu or an exception will be thrown - duplicates are not allowed.
     *
     * @param pos  the position to replace at
     * @param item the new menu item
     * @throws SMSException if the new menu item's label already exists in this menu
     */
    public void replaceItem(int pos, SMSMenuItem item) {
        if (items.size() != itemMap.size())
            rebuildItemMap();    // workaround for Heroes 1.4.8 which calls menu.getItems().clear

        String l = item.getLabelStripped();
        if (pos < 1 || pos > items.size()) {
            throw new SMSException("Index " + pos + " out of range.");
        }
        if (itemMap.containsKey(l) && pos != itemMap.get(l)) {
            throw new SMSException("Duplicate label '" + l + "' not allowed in menu '" + getName() + "'.");
        }
        itemMap.remove(items.get(pos - 1).getLabelStripped());
        items.set(pos - 1, item);
        itemMap.put(l, pos);

        setChanged();
        autosave();
    }

    /**
     * Rebuild the label->index mapping for the menu.  Needed if the menu order changes
     * (insertion, removal, sorting...)
     */
    private void rebuildItemMap() {
        itemMap.clear();
        for (int i = 0; i < items.size(); i++) {
            itemMap.put(items.get(i).getLabelStripped(), i + 1);
        }
    }

    /**
     * Sort the menu's items by label text - see {@link SMSMenuItem#compareTo(SMSMenuItem)}
     */
    public void sortItems() {
        Collections.sort(items);
        rebuildItemMap();
        setChanged();
        autosave();
    }

    /**
     * Remove an item from the menu by matching label.  If the label string is
     * just an integer value, remove the item at that 1-based numeric index.
     *
     * @param indexStr The label to search for and remove
     * @throws IllegalArgumentException if the label does not exist in the menu
     */
    public void removeItem(String indexStr) {
        if (StringUtils.isNumeric(indexStr)) {
            removeItem(Integer.parseInt(indexStr));
        } else {
            String stripped = ChatColor.stripColor(indexStr);
            SMSValidate.isTrue(itemMap.containsKey(stripped), "No such label '" + indexStr + "' in menu '" + getName() + "'.");
            removeItem(itemMap.get(stripped));
        }
    }

    /**
     * Remove an item from the menu by numeric index
     *
     * @param index 1-based index of the item to remove
     */
    public void removeItem(int index) {
        // Java lists are 0-indexed, our signs are 1-indexed
        items.remove(index - 1);
        rebuildItemMap();
        setChanged();
        autosave();
    }

    /**
     * Remove all items from a menu
     */
    public void removeAllItems() {
        items.clear();
        itemMap.clear();
        setChanged();
        autosave();
    }

    /**
     * Permanently delete a menu, dereferencing the object and removing saved data from disk.
     */
    void deletePermanent() {
        try {
            setChanged();
            notifyObservers(new MenuDeleteAction(null, true));
            ScrollingMenuSign.getInstance().getMenuManager().unregisterMenu(getName());
            SMSPersistence.unPersist(this);
        } catch (SMSException e) {
            // Should not get here
            LogUtils.warning("Impossible: deletePermanent got SMSException?" + e.getMessage());
        }
    }

    /**
     * Temporarily delete a menu.  The menu object is dereferenced but saved menu data is not
     * deleted from disk.
     */
    void deleteTemporary() {
        try {
            setChanged();
            notifyObservers(new MenuDeleteAction(null, false));
            ScrollingMenuSign.getInstance().getMenuManager().unregisterMenu(getName());
        } catch (SMSException e) {
            // Should not get here
            LogUtils.warning("Impossible: deleteTemporary got SMSException? " + e.getMessage());
        }
    }

    public void autosave() {
        // we only save menus which have been registered via SMSMenu.addMenu()
        if (isAutosave() && ScrollingMenuSign.getInstance().getMenuManager().checkForMenu(getName())) {
            SMSPersistence.save(this);
        }
    }

    /**
     * Check if the given player has access right for this menu.
     *
     * @param player The player to check
     * @return True if the player may use this view, false if not
     */
    public boolean hasOwnerPermission(Player player) {
        SMSAccessRights access = (SMSAccessRights) getAttributes().get(ACCESS);
        return access.isAllowedToUse(player, ownerId, getOwner(), getGroup());
    }

    /**
     * Get the usage limit details for this menu.
     *
     * @return The usage limit details
     */
    public SMSRemainingUses getUseLimits() {
        return uses;
    }

    @Override
    public String getLimitableName() {
        return getName();
    }

    /**
     * Returns a printable representation of the number of uses remaining for this item.
     *
     * @return Formatted usage information
     */
    String formatUses() {
        return uses.toString();
    }

    /**
     * Returns a printable representation of the number of uses remaining for this item, for the given player.
     *
     * @param sender Command sender to retrieve the usage information for
     * @return Formatted usage information
     */
    @Override
    public String formatUses(CommandSender sender) {
        if (sender instanceof Player) {
            return uses.toString((Player) sender);
        } else {
            return formatUses();
        }
    }

    @Override
    public File getSaveFolder() {
        return DirectoryStructure.getMenusFolder();
    }

    @Override
    public String getDescription() {
        return "menu";
    }

    @Override
    public Object onConfigurationValidate(ConfigurationManager configurationManager, String key, Object oldVal, Object newVal) {
        if (key.equals(ACCESS)) {
            SMSAccessRights access = (SMSAccessRights) newVal;
            if (access != SMSAccessRights.ANY && ownerId == null && !inThaw) {
                throw new SMSException("View must be owned by a player to change access control to " + access);
            } else if (access == SMSAccessRights.GROUP && ScrollingMenuSign.permission == null) {
                throw new SMSException("Cannot use GROUP access control (no permission group support available)");
            }
        } else if (key.equals(TITLE)) {
            return SMSUtil.unEscape(newVal.toString());
        } else if (key.equals(OWNER) && newVal.toString().equals("&console")) {
            // migration of owner field from pre-2.0.0: "&console" => "[console]"
            return ScrollingMenuSign.CONSOLE_OWNER;
        }

        return newVal;
    }

    @Override
    public void onConfigurationChanged(ConfigurationManager configurationManager, String key, Object oldVal, Object newVal) {
        if (key.equals(AUTOSORT) && (Boolean) newVal) {
            sortItems();
        } else if (key.equals(TITLE)) {
            title = newVal.toString();
            setChanged();
            notifyObservers(new TitleAction(null, oldVal.toString(), newVal.toString()));
        } else if (key.equals(OWNER) && !inThaw) {
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

        autosave();
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
                        LogUtils.warning("Menu [" + getName() + "]: no known UUID for player: " + owner);
                        ownerId = ScrollingMenuSign.CONSOLE_UUID;
                    }
                } catch (Exception e) {
                    LogUtils.warning("Menu [" + getName() + "]: can't retrieve UUID for player: " + owner + ": " + e.getMessage());
                }
            }
        });
    }

    /**
     * Check if this menu is owned by the given player.
     *
     * @param player the player to check
     * @return true if the menu is owned by the given player, false otherwise
     */
    public boolean isOwnedBy(Player player) {
        return player.getUniqueId().equals(ownerId);
    }

    /**
     * Require that the given command sender is allowed to modify this menu, and throw a SMSException if not.
     *
     * @param sender The command sender to check
     */
    public void ensureAllowedToModify(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (!isOwnedBy(player) && !PermissionUtils.isAllowedTo(player, "scrollingmenusign.edit.any")) {
                throw new SMSException("You don't have permission to modify that menu.");
            }
        }
    }

    @Override
    public int compareTo(SMSMenu o) {
        return getName().compareTo(o.getName());
    }

    public void forceUpdate(ViewUpdateAction action) {
        setChanged();
        notifyObservers(action);
    }
}
