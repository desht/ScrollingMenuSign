package me.desht.scrollingmenusign.listeners;

import java.util.logging.Level;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.spout.ItemListGUI;
import me.desht.scrollingmenusign.views.SMSSpoutView;
import me.desht.util.MiscUtil;

import org.getspout.spoutapi.event.screen.ButtonClickEvent;
import org.getspout.spoutapi.event.screen.ScreenCloseEvent;
import org.getspout.spoutapi.event.screen.ScreenListener;
import org.getspout.spoutapi.gui.ScreenType;

public class SMSSpoutScreenListener extends ScreenListener {
	
	@Override
	public void onScreenClose(ScreenCloseEvent event) {
		System.out.println("screen closed: " + event.getPlayer() + " - " + event.getScreenType());
		
		if (event.getScreenType() == ScreenType.CUSTOM_SCREEN && SMSSpoutView.hasActiveGUI(event.getPlayer())) {
			SMSSpoutView.screenClosed(event.getPlayer());
		}
	}
	
	@Override
	public void onButtonClick(ButtonClickEvent event) {
		if (event.getScreenType() == ScreenType.CUSTOM_SCREEN) {
			String label = event.getButton().getText();
			System.out.println("button clicked: " + label);
			ItemListGUI gui = SMSSpoutView.getActiveGUI(event.getPlayer());
			if (gui != null) {
				SMSMenu menu = gui.getView().getMenu();
				int idx = menu.indexOfItem(label);
				if (idx > 0 && idx <= menu.getItemCount()) {
					try {
						menu.getItem(idx).execute(event.getPlayer());
					} catch (SMSException e) {
						MiscUtil.statusMessage(event.getPlayer(), e.getMessage());
					}
				} else {
					MiscUtil.log(Level.WARNING, "Unexpected index " + idx + " for [" + label + "], menu " + menu.getName());
				}
			} else {
				MiscUtil.log(Level.WARNING, "Can't find active GUI for player " + event.getPlayer());
			}
		}
	}
}
