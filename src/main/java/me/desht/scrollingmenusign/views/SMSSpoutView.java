package me.desht.scrollingmenusign.views;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.gui.Color;
import org.getspout.spoutapi.player.SpoutPlayer;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.scrollingmenusign.spout.SpoutViewPopup;
import me.desht.scrollingmenusign.spout.SMSSpoutKeyMap;
import me.desht.scrollingmenusign.spout.TextEntryPopup;
import me.desht.dhutils.ConfigurationManager;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.PermissionUtils;

public class SMSSpoutView extends SMSScrollableView implements Poppable {

	// attributes
	public static final String AUTOPOPDOWN = "autopopdown";
	public static final String SPOUTKEYS = "spoutkeys";
	public static final String BACKGROUND = "background";
	public static final String ALPHA = "alpha";
	public static final String TEXTURE = "texture";

	// list of all popups which are active at this time, keyed by player name
	private static final Map<String, SpoutViewPopup> activePopups = new HashMap<String, SpoutViewPopup>();

	// list of all popups which have been created for each view, keyed by player name
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
		registerAttribute(SPOUTKEYS, new SMSSpoutKeyMap());
		registerAttribute(AUTOPOPDOWN, true);
		registerAttribute(BACKGROUND, "");
		registerAttribute(ALPHA, "");
		registerAttribute(TEXTURE, "");
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

		LogUtils.fine("showing Spout GUI for " + getName());

		if (!popups.containsKey(sp.getName())) {
			// create a new gui for this player
			popups.put(sp.getName(), new SpoutViewPopup(sp, this));
		}

		SpoutViewPopup gui = popups.get(sp.getName());
		activePopups.put(sp.getName(), gui);
		gui.popup();
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

		if (!popups.containsKey(sp.getName()) || !activePopups.containsKey(sp.getName())) {
			return;
		}

		LogUtils.fine("hiding Spout GUI for " + getName());
		activePopups.remove(sp.getName());
		popups.get(sp.getName()).popdown();

		// decision: destroy the gui object or not?
		//		popups.remove(sp.getName());
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
			SMSSpoutView otherView = (SMSSpoutView)gui.getView();
			if (otherView != this) {
				// the active GUI for the player belongs to a different view, so we pop down that one and 
				// pop up the player's GUI for this view
				otherView.hideGUI(sp);
				// just popping the GUI up immediately doesn't work - we need to defer it
				Bukkit.getScheduler().scheduleSyncDelayedTask(ScrollingMenuSign.getInstance(), new Runnable() {
					@Override
					public void run() {
						showGUI(sp);	
					}
				}, 3L);
			} else {
				hideGUI(sp);
			}
		} else {
			showGUI(sp);
		}
	}

	/* (non-Javadoc)
	 * @see me.desht.scrollingmenusign.views.SMSScrollableView#update(java.util.Observable, java.lang.Object)
	 */
	@Override
	public void update(Observable menu, Object arg) {
		if (arg instanceof SMSMenuAction) {
			SMSMenuAction act = (SMSMenuAction)arg;
			if (act == SMSMenuAction.SCROLLED)
				return;		// spout view will have repainted itself if it was scrolled
		}
		for (SMSPopup gui : popups.values()) {
			gui.repaint();
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
		return "spout (" + popups.size() + " popups created)";
	}

	/* (non-Javadoc)
	 * @see me.desht.scrollingmenusign.views.SMSView#deletePermanent()
	 */
	@Override
	public void deletePermanent() {
		for (Entry<String, SpoutViewPopup> e : popups.entrySet()) {
			if (e.getValue().isPoppedUp()) {
				hideGUI(e.getValue().getPlayer());
			}
		};
		super.deletePermanent();
	}

	private void screenClosed(String playerName) {
		// popups.remove(playerName);
	}

	/* (non-Javadoc)
	 * @see me.desht.scrollingmenusign.views.SMSView#onConfigurationValidate(me.desht.dhutils.ConfigurationManager, java.lang.String, java.lang.String)
	 */
	@Override
	public void onConfigurationValidate(ConfigurationManager configurationManager, String attribute, String newVal) {
		super.onConfigurationValidate(configurationManager, attribute, newVal);

		String err = null;
		if (attribute.equals(SPOUTKEYS) && !newVal.isEmpty()) {
			try {
				SMSSpoutKeyMap sp = new SMSSpoutKeyMap(newVal);
				if (keyMap.containsKey(sp.toString())) {
					String otherView = keyMap.get(sp.toString());
					if (SMSView.checkForView(otherView)) {
						err = sp.toString() + " is already used as the hotkey for another view (" + keyMap.get(sp.toString()) + ")";
					}
				}
			} catch (IllegalArgumentException e) {
				throw new SMSException("Invalid key binding: " + newVal);
			}

		} else if (attribute.equals(ALPHA) && !newVal.isEmpty()) {
			try {
				double d = Double.parseDouble(newVal);
				if (d < 0.0 || d > 1.0)
					err = "Invalid value for alpha channel (must be a floating point number between 0.0 and 1.0)";
			} catch (NumberFormatException e) {
				err = "Invalid value for alpha channel (must be a floating point number between 0.0 and 1.0)";
			}
		} else if (attribute.equals(BACKGROUND) && !newVal.isEmpty()) {
			try {
				new Color(newVal);
			} catch (NumberFormatException e) {
				err = "Invalid value for background (must be RRGGBB hex string, e.g. ffffff for white)";
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
		} else if (attribute.equals(AUTOPOPDOWN)) {
			// nothing
		} else if (attribute.equals(SMSView.TITLE_JUSTIFY)) {
			rejustify();
		} else {
			// all other attributes affect the appearance and require a redraw
			update(getMenu(), SMSMenuAction.REPAINT);
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
	public void setScrollPos(String playerName, int scrollPos) {
		super.setScrollPos(playerName, scrollPos);
		if (popups.containsKey(playerName)) {
			popups.get(playerName).scrollTo(scrollPos);
		}
	}

	public void rejustify() {
		for (SMSPopup popup: popups.values()) {
			popup.updateTitleJustification();
		}
	}

	/**
	 * Check if the given player has an active GUI
	 * 
	 * @param sp	The Spout player to check for
	 * @return		True if a GUI is currently popped up, false otherwise
	 */
	@Override
	public boolean hasActiveGUI(Player p) {
		return activePopups.containsKey(p.getName());
	}

	/**
	 * Get the active GUI for the given player, if any.
	 * 
	 * @param sp	The Spout player to check for
	 * @return		The GUI object if one is currently popped up, null otherwise
	 */
	@Override
	public SMSPopup getActiveGUI(Player p) {
		return activePopups.get(p.getName());
	}

	/**
	 * Convenience method.  Create a new spout view and add it to the given menu.
	 * 
	 * @param menu	The menu to add the view to
	 * @return		The view that was just created
	 * @throws SMSException 
	 */
	public static SMSView addSpoutViewToMenu(SMSMenu menu) throws SMSException {
		return addSpoutViewToMenu(null, menu);
	}
	public static SMSView addSpoutViewToMenu(String viewName, SMSMenu menu) throws SMSException {
		SMSView view = new SMSSpoutView(viewName, menu);
		view.register();
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

	/**
	 * A spout view screen was closed by the player pressing ESC.  We need to mark it
	 * as being closed.
	 * 
	 * @param player	Player who closed the screen
	 */
	public static void screenClosed(SpoutPlayer player) {
		String playerName = player.getName();
		activePopups.get(playerName).getView().screenClosed(playerName);
		activePopups.remove(playerName);
	}

	/* (non-Javadoc)
	 * @see me.desht.scrollingmenusign.views.SMSView#clearPlayerForView(org.bukkit.entity.Player)
	 */
	@Override
	public void clearPlayerForView(Player player) {
		super.clearPlayerForView(player);
		popups.remove(player.getName());

		// a little ugly, since this gets called once for every spout view when it only needs to be called once...
		activePopups.remove(player.getName());
	}

	public static SMSPopup getAnyActiveGUI(Player p) {
		for (SMSView v : listViews()) {
			if (v instanceof SMSSpoutView && ((SMSSpoutView) v).hasActiveGUI(p)) {
				return ((SMSSpoutView) v).getActiveGUI(p);
			}
		}
		return null;
	}
}
