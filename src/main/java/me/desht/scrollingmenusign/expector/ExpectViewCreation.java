package me.desht.scrollingmenusign.expector;

import me.desht.dhutils.DHUtilsException;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.views.SMSRedstoneView;
import me.desht.scrollingmenusign.views.SMSSignView;
import me.desht.scrollingmenusign.views.SMSView;

import org.bukkit.entity.Player;

public class ExpectViewCreation extends ExpectLocation {
	private SMSMenu menu;
	private String arg;

	public ExpectViewCreation(SMSMenu menu, String arg) {
		this.menu = menu;
		this.arg = arg;
	}

	@Override
	public void doResponse(Player p) {
		SMSView view = null;
		
		try {
			if (arg.equals("-sign")) {
				view = SMSSignView.addSignToMenu(menu, getLocation());
			} else if (arg.equals("-redstone")) {
				view = SMSRedstoneView.addRedstoneViewToMenu(menu, getLocation()); 
			}
		} catch (SMSException e) {
			throw new DHUtilsException(e.getMessage());
		}

		if (view != null) 
			MiscUtil.statusMessage(p, String.format("Added %s view &e%s&- to menu &e%s&-.",
					view.getType(), view.getName(), menu.getName()));
	}
}
