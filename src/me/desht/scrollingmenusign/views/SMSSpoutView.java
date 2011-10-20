package me.desht.scrollingmenusign.views;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.player.SpoutPlayer;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.scrollingmenusign.spout.ItemListGUI;
import me.desht.scrollingmenusign.spout.SMSSpoutKeyMap;
import me.desht.util.MiscUtil;

public class SMSSpoutView extends SMSScrollableView {

	// attributes
	protected static final String AUTOPOPDOWN = "autopopdown";
	protected static final String SPOUTKEYS = "spoutkeys";

	// list of all popups which are active at this time, keyed by player name
	private static final Map<String, ItemListGUI> activePopups = new HashMap<String, ItemListGUI>();

	// list of all popups which have been created for each view, keyed by player name
	private final Map<String, ItemListGUI> popups = new HashMap<String,ItemListGUI>();

	// map a set of keypresses to the view which handles them
	private static final Map<String, String> keyMap = new HashMap<String, String>();

	/**
	 * Construct a new SMSSPoutView object
	 * 
	 * @param name	The view name
	 * @param menu	The menu to attach the object to
	 */
	public SMSSpoutView(String name, SMSMenu menu) {
		super(name, menu);
		
		registerAttribute(SPOUTKEYS, new SMSSpoutKeyMap());
		registerAttribute(AUTOPOPDOWN, true);
	}

	public SMSSpoutView(SMSMenu menu) {
		this(null, menu);
	}

	// NOTE: explicit freeze() and thaw() methods not needed.  No new object fields which are not attributes.
	
	/**
	 * Show the given player's GUI for this view.
	 * 
	 * @param sp
	 */
	public void showGUI(SpoutPlayer sp) {
		if (!popups.containsKey(sp.getName())) {
			// create a new gui for this player
			popups.put(sp.getName(), new ItemListGUI(sp, this));
		}

		ItemListGUI gui = popups.get(sp.getName());
		activePopups.put(sp.getName(), gui);
		gui.popup();
	}

	/**
	 * Hide the given player's GUI for this view.
	 * 
	 * @param sp	The Spout player
	 */
	public void hideGUI(SpoutPlayer sp) {
		if (!popups.containsKey(sp.getName())) {
			return;
		}

		activePopups.remove(sp.getName());
		popups.get(sp.getName()).popdown();

		// decision: destroy the gui object or not?
		//		popups.remove(sp.getName());
	}

	/**
	 * Toggle the given player's visibility of the GUI for this view.  If a GUI for a different view
	 * is currently showing, pop that one down, and pop this one up.
	 * 
	 * @param sp	The Spout player
	 */
	public void toggleGUI(final SpoutPlayer sp) {
		if (hasActiveGUI(sp)) {
			ItemListGUI gui = getActiveGUI(sp);
			if (gui.getView() != this) {
				// the active GUI for the player belongs to a different view, so we pop down that one and 
				// pop up the player's GUI for this view
				gui.getView().hideGUI(sp);
				// just popping the GUI up immediately doesn't work - we need to defer it
				Bukkit.getScheduler().scheduleSyncDelayedTask(ScrollingMenuSign.getInstance(), new Runnable() {
					@Override
					public void run() {
						showGUI(sp);	
					}
				});
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
	public void update(Observable menu, Object arg1) {
		for (ItemListGUI gui : popups.values()) {
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
		for (Entry<String, ItemListGUI> e : popups.entrySet()) {
			if (e.getValue().isPoppedUp()) {
				hideGUI(e.getValue().getPlayer());
			}
		};
		super.deletePermanent();
	}

	private void screenClosed(String playerName) {
		// popups.remove(playerName);
	}

	@Override
	protected void onAttributeChanged(String attribute, String oldVal, String newVal) {
		super.onAttributeChanged(attribute, oldVal, newVal);
		
		if (attribute.equals(SPOUTKEYS)) {
			keyMap.remove(oldVal);
			if (!newVal.isEmpty()) {
				keyMap.put(newVal, getName());
			}
		}
	}

	@Override
	public void onExecuted(Player player) {
		super.onExecuted(player);
		
		try {
			boolean popdown = (Boolean) getAttribute(AUTOPOPDOWN);
			if (popdown) {
				hideGUI(SpoutManager.getPlayer(player));
			}
		} catch (SMSException e) {
			// shouldn't get here - we should always have a autopopdown attribute
			MiscUtil.log(Level.WARNING, "can't find popdown attribute in view " + getName());
		}
	}

	/**
	 * Check if the given player has an active GUI
	 * 
	 * @param sp	The Spout player to check for
	 * @return		True if a GUI is currently popped up, false otherwise
	 */
	public static boolean hasActiveGUI(SpoutPlayer sp) {
		return activePopups.containsKey(sp.getName());
	}

	/**
	 * Get the active GUI for the given player, if any.
	 * 
	 * @param sp	The Spout player to check for
	 * @return		The GUI object if one is currently popped up, null otherwise
	 */
	public static ItemListGUI getActiveGUI(SpoutPlayer sp) {
		return activePopups.get(sp.getName());
	}

	/**
	 * Convenience method.  Create a new spout view and add it to the given menu.
	 * 
	 * @param menu	The menu to add the view to
	 * @return		The view that was just created
	 */
	public static SMSView addSpoutViewToMenu(SMSMenu menu) {
		SMSView view = new SMSSpoutView(menu);
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
		
		String s = pressed.toString();

		String viewName = keyMap.get(s);
		if (viewName != null) {
			if (SMSView.checkForView(viewName)) {
				try {
					SMSView v = SMSView.getView(viewName);
					if (v instanceof SMSSpoutView) {
						((SMSSpoutView) v).toggleGUI(sp);
						return true;
					} else {
						MiscUtil.log(Level.WARNING, "Key mapping was added for a non-spout view?");
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
		System.out.println("screen closed: remove popup for " + playerName);
		activePopups.get(playerName).getView().screenClosed(playerName);
		activePopups.remove(playerName);
	}
}
