package me.desht.scrollingmenusign.views;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;

import me.desht.dhutils.ConfigurationManager;
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
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.gui.PopupScreen;
import org.getspout.spoutapi.player.SpoutPlayer;

public class SMSSpoutView extends SMSScrollableView implements PoppableView {

	// attributes
	public static final String AUTOPOPDOWN = "autopopdown";
	public static final String SPOUTKEYS = "spoutkeys";
	public static final String BACKGROUND = "background";
	public static final String ALPHA = "alpha";
	public static final String TEXTURE = "texture";

	// list of all popups which have been created for this view, keyed by player name
	private final Map<String, SpoutViewPopup> popups = new HashMap<String,SpoutViewPopup>();

	// map a set of keypresses to the view which handles them
	private static final Map<String, String> keyMap = new HashMap<String, String>();

	/**
	 * Construct a new SMSSPoutView object
	 *
	 * @param name	The view name
	 * @param menu	The menu to attach the object to
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
	 * @param p		The player object
	 */
	@Override
	public void showGUI(Player p) {
		SpoutPlayer sp = SpoutManager.getPlayer(p);
		if (!sp.isSpoutCraftEnabled())
			return;

		LogUtils.fine("showing Spout GUI for " + getName() + " to " + sp.getName());

		if (!popups.containsKey(sp.getName())) {
			// create a new gui for this player
			popups.put(sp.getName(), new SpoutViewPopup(sp, this));
		}

		SpoutViewPopup gui = popups.get(sp.getName());
		gui.popup(p);
	}

	/**
	 * Hide the given player's GUI for this view.
	 * 
	 * @param p		The player object
	 */
	@Override
	public void hideGUI(Player p) {
		SpoutPlayer sp = SpoutManager.getPlayer(p);
		if (!sp.isSpoutCraftEnabled())
			return;

		if (!popups.containsKey(sp.getName())) {
			return;
		}

		LogUtils.fine("hiding Spout GUI for " + getName() + " from " + sp.getName());
		popups.get(sp.getName()).popdown(p);

		// decision: destroy the gui object or not?
		//		popups.remove(sp.getName());
	}

	/**
	 * Check if the given player has an active GUI (for any Spout view, not
	 * necessarily this one).
	 * 
	 * @param sp	The Spout player to check for
	 * @return		True if a GUI is currently popped up, false otherwise
	 */
	@Override
	public boolean hasActiveGUI(Player p) {
		final SpoutPlayer sp = SpoutManager.getPlayer(p);
		if (!sp.isSpoutCraftEnabled())
			return false;
		PopupScreen popup = sp.getMainScreen().getActivePopup();
		return popup != null && popup instanceof SpoutViewPopup;
	}

	/**
	 * Get the active GUI for the given player, if any (for any Spout view, not
	 * necessarily this one).
	 * 
	 * @param sp	The Spout player to check for
	 * @return		The GUI object if one is currently popped up, null otherwise
	 */
	@Override
	public SMSPopup getActiveGUI(Player p) {
		final SpoutPlayer sp = SpoutManager.getPlayer(p);
		if (!sp.isSpoutCraftEnabled())
			return null;
		PopupScreen popup = sp.getMainScreen().getActivePopup();
		return popup instanceof SpoutViewPopup ? (SMSPopup) popup : null;
	}

	/**
	 * Toggle the given player's visibility of the GUI for this view.  If a GUI for a different view
	 * is currently showing, pop that one down, and pop this one up.
	 * 
	 * @param p		The player object
	 */
	@Override
	public void toggleGUI(Player p) {
		final SpoutPlayer sp = SpoutManager.getPlayer(p);
		if (!sp.isSpoutCraftEnabled())
			return;

		if (hasActiveGUI(sp)) {
			SMSPopup gui = getActiveGUI(sp);
			SMSSpoutView poppedUpView = (SMSSpoutView)gui.getView();
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
	public void update(Observable menu, Object arg) {
		switch ((SMSMenuAction) arg) {
		case REPAINT:
			for (SMSPopup gui : popups.values()) {
				gui.repaint();
			}
			break;
		default:
			break;
		}
		// although this is a scrollable view, we don't need to do anything if the action was SCROLLED,
		// since the Spout list widget handles its own repainting
	}

	@Override
	public void onDeletion() {
		super.onDeletion();
		for (Entry<String, SpoutViewPopup> e : popups.entrySet()) {
			if (e.getValue().isPoppedUp(null)) {
				hideGUI(e.getValue().getPlayer());
			}
		};
	}

	/* (non-Javadoc)
	 * @see me.desht.scrollingmenusign.views.SMSView#getType()
	 */
	@Override
	public String getType() {
		return "spout";
	}

	public String toString() {
		return "spout (" + popups.size() + " popups created)";
	}

	/* (non-Javadoc)
	 * @see me.desht.scrollingmenusign.views.SMSView#onConfigurationValidate(me.desht.dhutils.ConfigurationManager, java.lang.String, java.lang.String)
	 */
	@Override
	public void onConfigurationValidate(ConfigurationManager configurationManager, String attribute, Object oldVal, Object newVal) {
		super.onConfigurationValidate(configurationManager, attribute, oldVal, newVal);

		String err = null;
		if (attribute.equals(SPOUTKEYS) && !newVal.toString().isEmpty()) {
			try {
				SMSSpoutKeyMap sp = new SMSSpoutKeyMap(newVal.toString());
				if (keyMap.containsKey(sp.toString())) {
					String otherView = keyMap.get(sp.toString());
					if (SMSView.checkForView(otherView)) {
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
	public void scrollDown(String playerName) {
		super.scrollDown(playerName);
		scrollPopup(playerName);
	}

	@Override
	public void scrollUp(String playerName) {
		super.scrollUp(playerName);
		scrollPopup(playerName);
	}

	private void scrollPopup(String playerName) {
		if (popups.containsKey(playerName)) {
			SpoutViewPopup popup = popups.get(playerName);
			popup.scrollTo(getScrollPos(playerName));
			popup.ignoreNextSelection();
		}
	}

	/**
	 * Convenience method.  Create a new spout view and add it to the given menu.
	 * 
	 * @param menu	the menu to add the view to
	 * @param owner the owner of the view
	 * @return		the view that was just created
	 * @throws SMSException 
	 */
	public static SMSView addSpoutViewToMenu(SMSMenu menu, CommandSender owner) throws SMSException {
		return addSpoutViewToMenu(null, menu, owner);
	}

	public static SMSView addSpoutViewToMenu(String viewName, SMSMenu menu, CommandSender owner) throws SMSException {
		SMSView view = new SMSSpoutView(viewName, menu);
		view.register();
		view.setAttribute(OWNER, view.getOwnerName(owner));
		view.update(menu, SMSMenuAction.REPAINT);
		return view;
	}

	/**
	 * A Spout keypress event was received.
	 * 
	 * @param sp		The Spout player who pressed the key(s)
	 * @param pressed	Represents the set of keys currently pressed
	 * @return			True if a spout view was actually popped up or down, false otherwise
	 */
	public static boolean handleKeypress(SpoutPlayer sp, SMSSpoutKeyMap pressed) {
		if (pressed.keysPressed() == 0)
			return false;

		if (TextEntryPopup.hasActivePopup(sp.getName())) {
			return false;
		}

		String s = pressed.toString();

		String viewName = keyMap.get(s);
		if (viewName != null) {
			if (SMSView.checkForView(viewName)) {
				try {
					SMSView v = SMSView.getView(viewName);
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
		popups.remove(player.getName());
	}
}
