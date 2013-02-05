package me.desht.scrollingmenusign.views;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Observable;
import java.util.Set;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSMenuItem;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.scrollingmenusign.views.icon.IconMenu;
import me.desht.scrollingmenusign.views.icon.IconMenu.OptionClickEvent;
import me.desht.scrollingmenusign.views.icon.IconMenu.OptionClickEventHandler;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class SMSInventoryView extends SMSView implements PoppableView, OptionClickEventHandler {

	public static final String WIDTH = "width";
	public static final String AUTOPOPDOWN = "autopopdown";
	
	private final Map<String, IconMenu> iconMenus;	// map menu name to the icon menu object
	private final Map<String, Set<String>> users;	// map menu name to list of players using it
	
	public SMSInventoryView(String name, SMSMenu menu) {
		super(name, menu);
		
		registerAttribute(WIDTH, 9);
		registerAttribute(AUTOPOPDOWN, true);

		iconMenus = new HashMap<String, IconMenu>();
		iconMenus.put(getNativeMenu().getName(), new IconMenu(this));
		
		users = new HashMap<String, Set<String>>();
	}

	@Override
	public void update(Observable obj, Object arg1) {
		SMSMenu menu = (SMSMenu) obj;
		switch ((SMSMenuAction) arg1) {
		case REPAINT:
			if (menu == null) {
				for (IconMenu iconMenu : iconMenus.values()) {
					iconMenu.repaint();
				}
			} else if (iconMenus.containsKey(menu.getName())) {
				iconMenus.get(menu.getName()).repaint();
			}
			break;
		default:
			break;
		}
	}

	public Set<String> playersUsing(String menuName) {
		if (!users.containsKey(menuName)) {
			users.put(menuName, new HashSet<String>());
		}
		return users.get(menuName);
	}
	
	@Override
	public void pushMenu(String playerName, SMSMenu newActive) {
		super.pushMenu(playerName, newActive);
		String menuName = newActive.getName();
		
		if (playersUsing(menuName).isEmpty()) {
			// this menu was not used by anyone else yet - create it
			iconMenus.put(menuName, new IconMenu(this));
		}
		playersUsing(menuName).add(playerName);
	}

	@Override
	public SMSMenu popMenu(String playerName) {
		SMSMenu oldActive = super.popMenu(playerName);
		
		String menuName = oldActive.getName();
		playersUsing(menuName).remove(playerName);
		if (playersUsing(menuName).isEmpty()) {
			// no one using this menu any more - destroy it
			iconMenus.get(menuName).destroy();
			iconMenus.remove(menuName);
		}
		
		return oldActive;
	}
	
	@Override
	public void erase() {
		for (IconMenu iconMenu : iconMenus.values()) {
			iconMenu.destroy();
		}
	}

	@Override
	public String getType() {
		return "inventory";
	}

	@Override
	public void showGUI(Player p) {
		String menuName = getActiveMenu(p.getName()).getName();
		iconMenus.get(menuName).popup(p);
	}

	@Override
	public void hideGUI(Player p) {
		String menuName = getActiveMenu(p.getName()).getName();
		iconMenus.get(menuName).popdown(p);
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
		String menuName = getActiveMenu(p.getName()).getName();
		return iconMenus.get(menuName).isPoppedUp(p);
	}

	@Override
	public SMSPopup getActiveGUI(Player p) {
		String menuName = getActiveMenu(p.getName()).getName();
		return hasActiveGUI(p) ? iconMenus.get(menuName) : null;
	}

	@Override
	public void onOptionClick(final OptionClickEvent event) {
		final String playerName = event.getPlayer().getName();
		SMSMenu m = getActiveMenu(playerName);
		SMSMenuItem item = getActiveMenuItemAt(playerName, event.getIndex());
		if (item == null) {
			throw new SMSException("icon menu: index " + event.getIndex() + " out of range for " + getActiveMenu(playerName).getName() + " ?");
		}
		item.executeCommand(event.getPlayer(), this);
		item.feedbackMessage(event.getPlayer());
		onExecuted(event.getPlayer());
		if (m != getActiveMenu(playerName)) {
			// just pushed or popped a submenu
			// need to pop this inventory down and pop up a new one with the right title
			event.setWillClose(true);
			Bukkit.getScheduler().runTaskLater(ScrollingMenuSign.getInstance(), new Runnable() {
				@Override
				public void run() {
					showGUI(event.getPlayer());
				}
			}, 2L);
		} else {
			event.setWillClose((Boolean)getAttribute(AUTOPOPDOWN));
		}
	}
	
	@Override
	public void deleteTemporary() {
		for (IconMenu iconMenu : iconMenus.values()) {
			iconMenu.destroy();
		}
		super.deleteTemporary();
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
		view.update(view.getNativeMenu(), SMSMenuAction.REPAINT);
		return view;
	}
	
	@Override
	public String toString() {
		return "inventory: " + users.size() + " using " + iconMenus.size() + " menus";
	}
}
