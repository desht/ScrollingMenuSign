package me.desht.scrollingmenusign.views;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.getspout.spoutapi.player.SpoutPlayer;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.scrollingmenusign.spout.ItemListGUI;
import me.desht.scrollingmenusign.spout.SMSSpoutKeyMap;
import me.desht.util.MiscUtil;

public class SMSSpoutView extends SMSScrollableView {

	// list of all popups which are active at this time, keyed by player name
	private static final Map<String, ItemListGUI> activePopups = new HashMap<String, ItemListGUI>();

	// list of all popups which have been created for each view, keyed by player name
	private final Map<String, ItemListGUI> popups = new HashMap<String,ItemListGUI>();

	// map a set of keypresses to the view which handles them
	private static final Map<String, String> keyMap = new HashMap<String, String>();

	private SMSSpoutKeyMap activationKeys;

	public SMSSpoutView(String name, SMSMenu menu) {
		super(name, menu);

		activationKeys = new SMSSpoutKeyMap("");
	}

	public SMSSpoutView(SMSMenu menu) {
		this(null, menu);
	}

	public SMSSpoutKeyMap getActivationKeys() {
		return activationKeys;
	}

	public void setActivationKeys(SMSSpoutKeyMap activationKeys) throws SMSException {
		this.activationKeys = activationKeys;
		String s = activationKeys.toString();

		if (keyMap.containsKey(s) && !getName().equals(keyMap.get(s)))
			throw new SMSException("This key mapping is already used by the view: " + keyMap.get(s));

		keyMap.put(s, this.getName());
	}

	@Override
	public Map<String, Object> freeze() {
		Map<String, Object> map = super.freeze();

		map.put("activationKeys", activationKeys.toString());

		return map;
	}

	protected void thaw(ConfigurationSection node) {
		try {
			setActivationKeys(new SMSSpoutKeyMap(node.getString("activationKeys")));
		} catch (SMSException e) {
			MiscUtil.log(Level.WARNING, "Exception caught while thawing spout view '" + getName() + "': " + e.getMessage());
		}
	}

	public void showGUI(SpoutPlayer sp) {
		if (!popups.containsKey(sp.getName())) {
			// create a new gui for this player
			popups.put(sp.getName(), new ItemListGUI(sp, this));
		}

		ItemListGUI gui = popups.get(sp.getName());
		activePopups.put(sp.getName(), gui);
		gui.popup();
	}

	public void hideGUI(SpoutPlayer sp) {
		if (!popups.containsKey(sp.getName())) {
			return;
		}

		activePopups.remove(sp.getName());
		popups.get(sp.getName()).popdown();

		// decision: destroy the gui object or not?
		//		popups.remove(sp.getName());
	}

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

	@Override
	public void update(Observable menu, Object arg1) {
		for (ItemListGUI gui : popups.values()) {
			gui.repaint();
		}
	}

	@Override
	public String getType() {
		return "spout";
	}

	@Override
	public void setScrollPos(int scrollPos) {
		super.setScrollPos(scrollPos);
		update(getMenu(), SMSMenuAction.REPAINT);
	}

	@Override
	public void scrollDown(String playerName) {
		super.scrollDown(playerName);
		update(getMenu(), SMSMenuAction.REPAINT);
	}

	@Override
	public void scrollUp(String playerName) {
		super.scrollUp(playerName);
		update(getMenu(), SMSMenuAction.REPAINT);
	}

	public static boolean hasActiveGUI(SpoutPlayer sp) {
		return activePopups.containsKey(sp.getName());
	}

	public static ItemListGUI getActiveGUI(SpoutPlayer sp) {
		return activePopups.get(sp.getName());
	}

	public static SMSView addSpoutViewToMenu(SMSMenu menu) {
		SMSView view = new SMSSpoutView(menu);
		view.update(menu, SMSMenuAction.REPAINT);
		return view;
	}

	public String toString() {
		return "spout (" + popups.size() + " popups created)";
	}

	public static boolean handleKeypress(SpoutPlayer sp, SMSSpoutKeyMap pressed) {
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

	@Override
	public void deletePermanent() {
		for (Entry<String, ItemListGUI> e : popups.entrySet()) {
			if (e.getValue().isPoppedUp()) {
				hideGUI(e.getValue().getPlayer());
			}
		}
		super.deletePermanent();
	}

	private void screenClosed(String playerName) {
		//		popups.remove(playerName);
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
