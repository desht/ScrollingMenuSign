package me.desht.scrollingmenusign.views;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Observable;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.configuration.ConfigurationSection;
import org.getspout.spoutapi.keyboard.Keyboard;
import org.getspout.spoutapi.player.SpoutPlayer;

import com.google.common.base.Joiner;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.scrollingmenusign.spout.ItemListGUI;
import me.desht.scrollingmenusign.spout.SpoutUtils;
import me.desht.util.MiscUtil;

public class SMSSpoutView extends SMSScrollableView {

	private static final Map<String, ItemListGUI> activeGUIs = new HashMap<String,ItemListGUI>();
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
		if (activeGUIs.containsKey(sp.getName())) {
			return;
		}

		ItemListGUI gui = new ItemListGUI(sp, this);
		activeGUIs.put(sp.getName(), gui);
		gui.popup();
	}

	public void hideGUI(SpoutPlayer sp) {
		if (!activeGUIs.containsKey(sp.getName())) {
			return;
		}

		activeGUIs.get(sp.getName()).popdown();
		activeGUIs.remove(sp.getName());
	}

	public void toggleGUI(SpoutPlayer sp) {
		if (activeGUIs.containsKey(sp.getName())) {
			hideGUI(sp);
		} else {
			showGUI(sp);
		}
	}

	@Override
	public void update(Observable menu, Object arg1) {
		if (isDirty()) {
			for (ItemListGUI gui : activeGUIs.values()) {
				gui.repaint();
			}
			setDirty(false);
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
		return activeGUIs.containsKey(sp.getName());
	}

	public static ItemListGUI getGUI(SpoutPlayer sp) {
		return activeGUIs.get(sp.getName());
	}

	public static SMSView addSpoutViewToMenu(SMSMenu menu) {
		SMSView view = new SMSSpoutView(menu);
		view.update(menu, SMSMenuAction.REPAINT);
		return view;
	}
	
	public String toString() {
		return "spout (" + activeGUIs.size() + " active GUIs)";
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
}
