package me.desht.scrollingmenusign.expector;

import me.desht.dhutils.DHUtilsException;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.views.SMSMultiSignView;
import me.desht.scrollingmenusign.views.SMSRedstoneView;
import me.desht.scrollingmenusign.views.SMSSignView;
import me.desht.scrollingmenusign.views.SMSView;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ExpectViewCreation extends ExpectLocation {
	private final String viewName;
	private final SMSMenu menu;
	private final String arg;

	public ExpectViewCreation(String viewName, SMSMenu menu, String arg) {
		this.viewName = viewName;
		this.menu = menu;
		this.arg = arg;
	}

	@Override
	public void doResponse(String playerName) {
		SMSView view = null;

		Player player = Bukkit.getPlayer(playerName);
		try {
			// TODO: code smell
			if (arg.equals("sign")) {
				view = SMSSignView.addSignToMenu(viewName, menu, getLocation(), player);
			} else if (arg.equals("redstone")) {
				view = SMSRedstoneView.addRedstoneViewToMenu(viewName, menu, getLocation(), player); 
			} else if (arg.equals("multisign")) {
				view = SMSMultiSignView.addSignToMenu(viewName, menu, getLocation(), player);
			}
		} catch (SMSException e) {
			throw new DHUtilsException(e.getMessage());
		}

		if (view != null && player != null) {
			MiscUtil.statusMessage(player, String.format("Added %s view &e%s&- to menu &e%s&-.",
			                                             view.getType(), view.getName(), menu.getName()));
		}
	}
}