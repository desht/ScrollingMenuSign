package me.desht.scrollingmenusign.views;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;

import org.getspout.spoutapi.player.SpoutPlayer;

import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.scrollingmenusign.spout.ItemListGUI;

public class SMSSpoutView extends SMSScrollableView {

	private static final Map<String, ItemListGUI> activeGUIs = new HashMap<String,ItemListGUI>();

	public SMSSpoutView(String name, SMSMenu menu) {
		super(name, menu);
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

}
