package me.desht.scrollingmenusign.listeners;

import me.desht.scrollingmenusign.views.SMSSpoutView;

import org.getspout.spoutapi.event.screen.ScreenCloseEvent;
import org.getspout.spoutapi.event.screen.ScreenListener;
import org.getspout.spoutapi.gui.ScreenType;

public class SMSSpoutScreenListener extends ScreenListener {
	
	@Override
	public void onScreenClose(ScreenCloseEvent event) {		
		if (event.getScreenType() == ScreenType.CUSTOM_SCREEN && SMSSpoutView.hasActiveGUI(event.getPlayer())) {
			SMSSpoutView.screenClosed(event.getPlayer());
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
