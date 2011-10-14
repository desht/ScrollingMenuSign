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
		System.out.println("STUB: repaint spout gui for " + view.getMenu().getName());
		
	}

	public void popup() {
		System.out.println("STUB: pop up spout gui for"+ view.getMenu().getName());
	}

	public void popdown() {
		System.out.println("STUB: pop down spout gui for "+ view.getMenu().getName());
	}

}
