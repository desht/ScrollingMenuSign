package me.desht.scrollingmenusign.spout;

import me.desht.scrollingmenusign.views.SMSSpoutView;

import org.getspout.spoutapi.player.SpoutPlayer;

public class ItemListGUI {
	private SpoutPlayer sp;
	private SMSSpoutView view;
	
	public ItemListGUI(SpoutPlayer sp, SMSSpoutView view) {
		this.sp = sp;
		this.view = view;	
	}

	public SMSSpoutView getView() {
		return view;
	}
	
	public SpoutPlayer getPlayer() {
		return sp;
	}
	
	public void repaint() {
		// TODO Auto-generated method stub
		
	}

	public void popup() {
		// TODO Auto-generated method stub
		
	}

	public void popdown() {
		// TODO Auto-generated method stub
		
	}

}
