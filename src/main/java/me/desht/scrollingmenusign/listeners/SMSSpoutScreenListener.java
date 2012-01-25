package me.desht.scrollingmenusign.listeners;

import me.desht.scrollingmenusign.views.SMSSpoutView;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.getspout.spoutapi.event.screen.ScreenCloseEvent;
import org.getspout.spoutapi.gui.ScreenType;

public class SMSSpoutScreenListener implements Listener {
	
	@EventHandler
	public void onScreenClose(ScreenCloseEvent event) {		
		if (event.getScreenType() == ScreenType.CUSTOM_SCREEN) {
			if (SMSSpoutView.hasActiveGUI(event.getPlayer())) {
				SMSSpoutView.screenClosed(event.getPlayer());
			}
//			else if (TextEntryPopup.isPoppedUp(event.getPlayer())) {
//				TextEntryPopup.screenClosed(event.getPlayer());
//			}
		}
	}
	
//	@Override
//	public void onButtonClick(ButtonClickEvent event) {
//		if (event.getScreenType() == ScreenType.CUSTOM_SCREEN) {
//			SpoutViewPopup gui = SMSSpoutView.getActiveGUI(event.getPlayer());
//			if (gui != null) {
//				gui.handleButtonClick(event);
//			}
//		}
//	}
}
