package me.desht.scrollingmenusign.views;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Observable;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.getspout.spoutapi.keyboard.Keyboard;
import org.getspout.spoutapi.player.SpoutPlayer;

import com.google.common.base.Joiner;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.scrollingmenusign.spout.ItemListGUI;
import me.desht.scrollingmenusign.spout.SpoutUtils;
import me.desht.util.MiscUtil;

public class SMSSpoutView extends SMSScrollableView {

	// list of all popups which are active at this time, keyed by player name
	private static final Map<String, ItemListGUI> activePopups = new HashMap<String, ItemListGUI>();

	// list of all popups which have been created for each view, keyed by player name
	private final Map<String, ItemListGUI> popups = new HashMap<String,ItemListGUI>();

	private static final Map<String, String> keyMap = new HashMap<String, String>();

	private Set<Keyboard> activationKeys;

	public SMSSpoutView(String name, SMSMenu menu) {
		super(name, menu);

		activationKeys = new HashSet<Keyboard>();
	}

	public SMSSpoutView(SMSMenu menu) {
		this(null, menu);
	}

	public Set<Keyboard> getActivationKeys() {
		return activationKeys;
	}

	public void setActivationKeys(Set<Keyboard> activationKeys) {
		this.activationKeys = activationKeys;
		String s = Joiner.on("+").join(activationKeys);
		keyMap.put(s, this.getName());
	}

	@Override
	public Map<String, Object> freeze() {
		Map<String, Object> map = super.freeze();

		map.put("activationKeys", Joiner.on("+").join(activationKeys));

		return map;
	}

	protected void thaw(ConfigurationSection node) {
		setActivationKeys(SpoutUtils.parseKeyDefinition(node.getString("activationKeys")));
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
	public void scrollDown() {
		super.scrollDown();
		update(getMenu(), SMSMenuAction.REPAINT);
	}

	@Override
	public void scrollUp() {
		super.scrollUp();
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

	public static boolean handleKeypress(SpoutPlayer sp, Set<Keyboard> pressed) {
		String s = Joiner.on("+").join(pressed);

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

	private void screenClosed(String playerName) {
//		popups.remove(playerName);
	}

	public static void screenClosed(SpoutPlayer player) {
		String playerName = player.getName();
		if (playerName != null && activePopups.containsKey(playerName)) {
			System.out.println("screen closed: remove popup for " + playerName);
			activePopups.get(playerName).getView().screenClosed(playerName);
			activePopups.remove(playerName);
		}		
	}
}
