package me.desht.scrollingmenusign;

import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;

public enum SMSAction {
	NONE, SCROLLDOWN, SCROLLUP, EXECUTE;
	
	static SMSAction getAction(PlayerInteractEvent event) {
		Action a = event.getAction();
		Player player = event.getPlayer();
		
		String key;
		if (a == Action.RIGHT_CLICK_BLOCK ) {
			key = "sms.actions.rightclick.";
		} else if (a == Action.LEFT_CLICK_BLOCK) {
			key = "sms.actions.leftclick.";
		} else {
			return null;
		}
		return _makeAction(player, key);
	}

	static SMSAction getAction(PlayerItemHeldEvent event) {
		Player player = event.getPlayer();
		
		int delta = event.getNewSlot() - event.getPreviousSlot();
		String key;
		if (delta == -1 || delta == 8) {
			key = "sms.actions.wheelup.";
		} else if (delta == 1 || delta == -8) {
			key = "sms.actions.wheeldown.";
		} else {
			return null;
		}
		return _makeAction(player, key);
	}

	private static SMSAction _makeAction(Player player, String key) {
		if (player.isSneaking())
			key = key + "sneak";
		else 
			key = key + "normal";
		
		String s = SMSConfig.getConfiguration().getString(key, "none");
		return SMSAction.valueOf(s.toUpperCase());
	}
}
