package me.desht.scrollingmenusign.expector;

import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.views.SMSGlobalScrollableView;
import me.desht.scrollingmenusign.views.redout.Switch;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ExpectSwitchAddition extends ExpectLocation {
	private final String trigger;
	private final SMSGlobalScrollableView view;

	public ExpectSwitchAddition(SMSGlobalScrollableView view, String trigger) {
		this.view = view;
		this.trigger = trigger;
	}

	@Override
	public void doResponse(String playerName) {
		Switch sw = new Switch(view, trigger, getLocation());
		view.addSwitch(sw);
		view.updateSwitchPower();
		view.autosave();

		Player player = Bukkit.getPlayer(playerName);
		if (player != null) {
			MiscUtil.statusMessage(player, String.format("Added output lever at %s to %s view &e%s / %s&-.",
			                                             MiscUtil.formatLocation(sw.getLocation()),
			                                             view.getType(), view.getName(), trigger));
		}
	}
}