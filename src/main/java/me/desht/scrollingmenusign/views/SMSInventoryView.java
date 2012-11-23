package me.desht.scrollingmenusign.views;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;

import me.desht.dhutils.LogUtils;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSMenuItem;
import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.scrollingmenusign.views.icon.IconMenu;
import me.desht.scrollingmenusign.views.icon.IconMenu.OptionClickEvent;
import me.desht.scrollingmenusign.views.icon.IconMenu.OptionClickEventHandler;

import org.bukkit.entity.Player;

public class SMSInventoryView extends SMSView implements Poppable, OptionClickEventHandler {

	public static final String WIDTH = "width";

//	private static final Map<String,IconMenu> activeIconMenus = new HashMap<String, IconMenu>();

	private final Map<String, IconMenu> iconMenus = new HashMap<String, IconMenu>();

	public SMSInventoryView(String name, SMSMenu menu) {
		super(name, menu);

		registerAttribute(WIDTH, 9);
	}

	@Override
	public void update(Observable menu, Object arg1) {
		for (IconMenu iconMenu : iconMenus.values()) {
			iconMenu.repaint();
		}
	}

	@Override
	public String getType() {
		return "inventory";
	}

	@Override
	public void showGUI(Player p) {
		if (!iconMenus.containsKey(p.getName())) {
			iconMenus.put(p.getName(), new IconMenu(p, this));
		}
		
		LogUtils.fine("showing icon menu for " + getName());

		IconMenu iconMenu = iconMenus.get(p.getName());

//		activeIconMenus.put(p.getName(), iconMenu);
		iconMenu.popup();
	}

	@Override
	public void hideGUI(Player p) {
		if (!iconMenus.containsKey(p.getName())) {
			return;
		}
		
		LogUtils.fine("hiding icon menu for " + getName());
//		activeIconMenus.remove(p.getName());
		iconMenus.get(p.getName()).popdown();
	}

	@Override
	public void toggleGUI(Player p) {
		if (hasActiveGUI(p)) {
			hideGUI(p);
		} else {
			showGUI(p);
		}
	}

	@Override
	public boolean hasActiveGUI(Player p) {
		return iconMenus.containsKey(p.getName()) && iconMenus.get(p.getName()).isPoppedUp();
	}

	@Override
	public SMSPopup getActiveGUI(Player p) {
		return hasActiveGUI(p) ? iconMenus.get(p.getName()) : null;
	}

	@Override
	public void onOptionClick(OptionClickEvent event) {
		SMSMenuItem item = getMenu().getItemAt(event.getPosition());
		if (item == null) {
			throw new SMSException("icon menu: index " + event.getPosition() + " out of range for " + getMenu().getName() + " ?");
		}
		item.executeCommand(event.getPlayer(), this);
		item.feedbackMessage(event.getPlayer());
		onExecuted(event.getPlayer());
	}

	/**
	 * Convenience method.
	 * 
	 * @param menu
	 * @return
	 */
	public static SMSInventoryView addInventoryViewToMenu(SMSMenu menu) {
		return addInventoryViewToMenu(null, menu);
	}

	public static SMSInventoryView addInventoryViewToMenu(String viewName, SMSMenu menu) {
		SMSInventoryView view = new SMSInventoryView(viewName, menu);
		view.register();
		view.update(view.getMenu(), SMSMenuAction.REPAINT);
		return view;
	}
}
