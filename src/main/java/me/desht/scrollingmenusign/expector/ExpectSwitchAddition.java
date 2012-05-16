package me.desht.scrollingmenusign.expector;

import me.desht.scrollingmenusign.util.MiscUtil;
import me.desht.scrollingmenusign.views.SMSGlobalScrollableView;
import me.desht.scrollingmenusign.views.redout.Switch;

import org.bukkit.entity.Player;

public class ExpectSwitchAddition extends ExpectLocation {
	private final String trigger;
	private final SMSGlobalScrollableView view;
	
	public ExpectSwitchAddition(SMSGlobalScrollableView view, String trigger) {
		this.view = view;
		this.trigger = trigger;
	}
	
	@Override
	public void doResponse(Player p) {
		Switch sw = new Switch(view, trigger, getLocation());
		view.updateSwitchPower();
		
		MiscUtil.statusMessage(p, String.format("Added output lever at %s to %s view &e%s / %s&-.",
		                                        MiscUtil.formatLocation(sw.getLocation()),
		                                        view.getType(), view.getName(), trigger));
	}
}
