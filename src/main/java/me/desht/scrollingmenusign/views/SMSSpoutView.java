package me.desht.scrollingmenusign.views;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.UUID;

import me.desht.dhutils.ConfigurationManager;
import me.desht.dhutils.Debugger;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.PermissionUtils;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.scrollingmenusign.spout.HexColor;
import me.desht.scrollingmenusign.spout.SMSSpoutKeyMap;
import me.desht.scrollingmenusign.spout.SpoutViewPopup;
import me.desht.scrollingmenusign.spout.TextEntryPopup;

import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.gui.PopupScreen;
import org.getspout.spoutapi.player.SpoutPlayer;

/**
 * This view draws menus on a popped-up Spout view.
 */
public class SMSSpoutView extends SMSScrollableView implements PoppableView {

    // attributes
    public static final String AUTOPOPDOWN = "autopopdown";
    public static final String SPOUTKEYS = "spoutkeys";
    public static final String BACKGROUND = "background";
    public static final String ALPHA = "alpha";
    public static final String TEXTURE = "texture";

    // list of all popups which have been created for this view, keyed by player ID
    private final Map<UUID, SpoutViewPopup> popups = new HashMap<UUID, SpoutViewPopup>();

    // map a set of keypresses to the view which handles them
    private static final Map<String, String> keyMap = new HashMap<String, String>();

    /**
     * Construct a new SMSSPoutView object
     *
     * @param name The view name
     * @param menu The menu to attach the object to
     * @throws SMSException
     */
    public SMSSpoutView(String name, SMSMenu menu) throws SMSException {
        super(name, menu);
        setWrap(false);

        if (!ScrollingMenuSign.getInstance().isSpoutEnabled()) {
            throw new SMSException("Spout view cannot be created - server does not have Spout enabled");
        }
        Configuration config = ScrollingMenuSign.getInstance().getConfig();
        String defColor = config.getString("sms.spout.list_background");
        Double defAlpha = config.getDouble("sms.spout.list_alpha");
        registerAttribute(SPOUTKEYS, new SMSSpoutKeyMap(), "Key(s) to toggle view visibility");
        registerAttribute(AUTOPOPDOWN, true, "Auto-popdown after item click?");
        registerAttribute(BACKGROUND, new HexColor(defColor), "Background colour of view");
        registerAttribute(ALPHA, defAlpha, "Transparency of view");
        registerAttribute(TEXTURE, "", "Image to use as view background");
    }

    public SMSSpoutView(SMSMenu menu) throws SMSException {
        this(null, menu);
    }

    // NOTE: explicit freeze() and thaw() methods not needed.  No new object fields which are not attributes.

    /**
     * Show the given player's GUI for this view.
     *
     * @param p The player object
     */
    @Override
    public void showGUI(Player p) {
        SpoutPlayer sp = SpoutManager.getPlayer(p);
        if (!sp.isSpoutCraftEnabled())
            return;

        Debugger.getInstance().debug("showing Spout GUI for " + getName() + " to " + sp.getDisplayName());

        if (!popups.containsKey(sp.getUniqueId())) {
            // create a new gui for this player
            popups.put(sp.getUniqueId(), new SpoutViewPopup(sp, this));
        }

        SpoutViewPopup gui = popups.get(sp.getUniqueId());
        gui.popup();
    }

    /**
     * Hide the given player's GUI for this view.
     *
     * @param p The player object
     */
    @Override
    public void hideGUI(Player p) {
        SpoutPlayer sp = SpoutManager.getPlayer(p);
        if (!sp.isSpoutCraftEnabled())
            return;

        if (!popups.containsKey(sp.getUniqueId())) {
            return;
        }

        Debugger.getInstance().debug("hiding Spout GUI for " + getName() + " from " + sp.getDisplayName());
        popups.get(sp.getUniqueId()).popdown();

        // decision: destroy the gui object or not?
        //		popups.remove(sp.getName());
    }

    /**
     * Check if the given player has an active GUI (for any Spout view, not
     * necessarily this one).
     *
     * @param player the player to check for
     * @return true if a GUI is currently popped up, false otherwise
     */
    @Override
    public boolean hasActiveGUI(Player player) {
        final SpoutPlayer sp = SpoutManager.getPlayer(player);
        if (!sp.isSpoutCraftEnabled())
            return false;
        PopupScreen popup = sp.getMainScreen().getActivePopup();
        return popup != null && popup instanceof SpoutViewPopup;
    }

    /**
     * Get the active GUI for the given player, if any (for any Spout view, not
     * necessarily this one).
     *
     * @param player the player to check for
     * @return the GUI object if one is currently popped up, null otherwise
     */
    @Override
    public SMSPopup getActiveGUI(Player player) {
        final SpoutPlayer sp = SpoutManager.getPlayer(player);
        if (!sp.isSpoutCraftEnabled())
            return null;
        PopupScreen popup = sp.getMainScreen().getActivePopup();
        return popup instanceof SpoutViewPopup ? (SMSPopup) popup : null;
    }

    /**
     * Toggle the given player's visibility of the GUI for this view.  If a GUI for a different view
     * is currently showing, pop that one down, and pop this one up.
     *
     * @param p The player object
     */
    @Override
    public void toggleGUI(Player p) {
        final SpoutPlayer sp = SpoutManager.getPlayer(p);
        if (!sp.isSpoutCraftEnabled())
            return;

        if (hasActiveGUI(sp)) {
            SMSPopup gui = getActiveGUI(sp);
            SMSSpoutView poppedUpView = (SMSSpoutView) gui.getView();
            if (poppedUpView == this) {
                // the player has an active GUI from this view - just pop it down
                hideGUI(sp);
            } else {
                // the player has an active GUI, but it belongs to a different spout view, so pop down
                // that one and pop up the GUI for this view
                poppedUpView.hideGUI(sp);
                // just popping the GUI up immediately doesn't appear to work - we need to defer it by a few ticks
                Bukkit.getScheduler().scheduleSyncDelayedTask(ScrollingMenuSign.getInstance(), new Runnable() {
                    @Override
                    public void run() {
                        showGUI(sp);
                    }
                }, 3L);
            }
        } else {
            // no GUI shown right now - just pop this one up
            showGUI(sp);
        }
    }

    /* (non-Javadoc)
     * @see me.desht.scrollingmenusign.views.SMSScrollableView#update(java.util.Observable, java.lang.Object)
     */
    @Override
    public void update(Observable menu, Object arg1) {
        super.update(menu, arg1);
        ViewUpdateAction vu = ViewUpdateAction.getAction(arg1);
        switch (vu.getAction()) {
            case REPAINT:
                for (SpoutViewPopup gui : popups.values()) {
                    if (vu.getPlayer() == null || gui.getPlayer().equals(vu.getPlayer())) {
                        gui.repaint();
                    }
                }
                break;
            default:
                break;
        }
        // although this is a scrollable view, we don't need to do anything if the action was SCROLLED,
        // since the Spout list widget handles its own repainting
    }

    @Override
    public void onDeleted(boolean permanent) {
        super.onDeleted(permanent);
        if (permanent) {
            for (Entry<UUID, SpoutViewPopup> e : popups.entrySet()) {
                if (e.getValue().isPoppedUp()) {
                    hideGUI(e.getValue().getPlayer());
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see me.desht.scrollingmenusign.views.SMSView#getType()
     */
    @Override
    public String getType() {
        return "spout";
    }

    public String toString() {
        return "spout - " + popups.size() + " popups created";
    }

    /* (non-Javadoc)
     * @see me.desht.scrollingmenusign.views.SMSView#onConfigurationValidate(me.desht.dhutils.ConfigurationManager, java.lang.String, java.lang.String)
     */
    @Override
    public Object onConfigurationValidate(ConfigurationManager configurationManager, String attribute, Object oldVal, Object newVal) {
        newVal = super.onConfigurationValidate(configurationManager, attribute, oldVal, newVal);

        String err = null;
        if (attribute.equals(SPOUTKEYS) && !newVal.toString().isEmpty()) {
            try {
                SMSSpoutKeyMap sp = new SMSSpoutKeyMap(newVal.toString());
                if (keyMap.containsKey(sp.toString())) {
                    String otherView = keyMap.get(sp.toString());
                    if (ScrollingMenuSign.getInstance().getViewManager().checkForView(otherView)) {
                        err = sp.toString() + " is already used as the hotkey for another view (" + keyMap.get(sp.toString()) + ")";
                    }
                }
            } catch (IllegalArgumentException e) {
                throw new SMSException("Invalid key binding: " + newVal);
            }

        } else if (attribute.equals(ALPHA)) {
            try {
                double d = (Double) newVal;
                if (d < 0.0 || d > 1.0)
                    err = "Invalid value for alpha channel (must be a floating point number between 0.0 and 1.0)";
            } catch (NumberFormatException e) {
                err = "Invalid value for alpha channel (must be a floating point number between 0.0 and 1.0)";
            }
        }
        if (err != null) {
            throw new SMSException(err);
        }

        return newVal;
    }

    /* (non-Javadoc)
     * @see me.desht.scrollingmenusign.views.SMSView#onConfigurationChanged(me.desht.dhutils.ConfigurationManager, java.lang.String, java.lang.Object, java.lang.Object)
     */
    @Override
    public void onConfigurationChanged(ConfigurationManager configurationManager, String attribute, Object oldVal, Object newVal) {
        super.onConfigurationChanged(configurationManager, attribute, oldVal, newVal);

        if (attribute.equals(SPOUTKEYS)) {
            // cache a new stringified key mapping definition for this view
            keyMap.remove(oldVal.toString());
            if (!newVal.toString().isEmpty()) {
                keyMap.put(newVal.toString(), getName());
            }
        }
    }

    @Override
    public void onExecuted(Player player) {
        super.onExecuted(player);

        Boolean popdown = (Boolean) getAttribute(AUTOPOPDOWN);
        if (popdown != null && popdown) {
            hideGUI(SpoutManager.getPlayer(player));
        }
    }

    @Override
    public void scrollDown(Player player) {
        super.scrollDown(player);
        scrollPopup(player);
    }

    @Override
    public void scrollUp(Player player) {
        super.scrollUp(player);
        scrollPopup(player);
    }

    private void scrollPopup(Player player) {
        if (popups.containsKey(player.getUniqueId())) {
            SpoutViewPopup popup = popups.get(player.getUniqueId());
            popup.scrollTo(getScrollPos(player));
            popup.ignoreNextSelection();
        }
    }

    /**
     * A Spout keypress event was received.
     *
     * @param sp      The Spout player who pressed the key(s)
     * @param pressed Represents the set of keys currently pressed
     * @return True if a spout view was actually popped up or down, false otherwise
     */
    public static boolean handleKeypress(SpoutPlayer sp, SMSSpoutKeyMap pressed) {
        if (pressed.keysPressed() == 0)
            return false;

        if (TextEntryPopup.hasActivePopup(sp.getUniqueId())) {
            return false;
        }

        String s = pressed.toString();
        ViewManager vm = ScrollingMenuSign.getInstance().getViewManager();
        String viewName = keyMap.get(s);
        if (viewName != null) {
            if (vm.checkForView(viewName)) {
                try {
                    SMSView v = vm.getView(viewName);
                    if (v instanceof SMSSpoutView) {
                        if (!PermissionUtils.isAllowedTo(sp, "scrollingmenusign.use.spout"))
                            return false;
                        if (!v.hasOwnerPermission(sp))
                            return false;
                        ((SMSSpoutView) v).toggleGUI(sp);
                        return true;
                    } else {
                        LogUtils.warning("Got non-Spout view " + v.getName() + " for keymap " + s);
                    }
                } catch (SMSException e) {
                    // shouldn't get here - we checked for the view
                }
            } else {
                // the view was probably deleted - remove the key mapping
                keyMap.remove(s);
            }
        }

        return false;
    }

    /* (non-Javadoc)
     * @see me.desht.scrollingmenusign.views.SMSView#clearPlayerForView(org.bukkit.entity.Player)
     */
    @Override
    public void clearPlayerForView(Player player) {
        super.clearPlayerForView(player);
        popups.remove(player.getUniqueId());
    }
}
