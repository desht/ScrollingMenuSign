package me.desht.scrollingmenusign.views;

import java.util.Observable;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSMenuItem;
import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.scrollingmenusign.views.icon.IconMenu;
import me.desht.scrollingmenusign.views.icon.IconMenu.OptionClickEvent;
import me.desht.scrollingmenusign.views.icon.IconMenu.OptionClickEventHandler;

import org.bukkit.entity.Player;

public class SMSInventoryView extends SMSView implements PoppableView, OptionClickEventHandler {

	public static final String WIDTH = "width";
	public static final String AUTOPOPDOWN = "autopopdown";

	private IconMenu iconMenu;
	
	public SMSInventoryView(String name, SMSMenu menu) {
		super(name, menu);
		
		registerAttribute(WIDTH, 9);
		registerAttribute(AUTOPOPDOWN, true);

		iconMenu = new IconMenu(this);
	}

	@Override
	public void update(Observable menu, Object arg1) {
		iconMenu.repaint();
	}

	@Override
	public String getType() {
		return "inventory";
	}

	@Override
	public void showGUI(Player p) {
		iconMenu.popup(p);
	}

	@Override
	public void hideGUI(Player p) {
		iconMenu.popdown(p);
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
		return iconMenu.isPoppedUp(p);
	}

	@Override
	public SMSPopup getActiveGUI(Player p) {
		return hasActiveGUI(p) ? iconMenu : null;
	}

	@Override
	public void onOptionClick(OptionClickEvent event) {
		SMSMenuItem item = getActiveMenu().getItemAt(event.getIndex());
		if (item == null) {
			throw new SMSException("icon menu: index " + event.getIndex() + " out of range for " + getActiveMenu().getName() + " ?");
		}
		item.executeCommand(event.getPlayer(), this);
		item.feedbackMessage(event.getPlayer());
		onExecuted(event.getPlayer());
		event.setWillClose((Boolean)getAttribute(AUTOPOPDOWN));
	}
	
	@Override
	public void deleteTemporary() {
		iconMenu.destroy();
		super.deleteTemporary();
	}
	
	@Override
	public void deletePermanent() {
		iconMenu.destroy();
		super.deletePermanent();
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
		return "inventory: " + iconMenu.getSlots() + " slots";
	}
}
