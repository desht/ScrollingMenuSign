package me.desht.scrollingmenusign.enums;

import me.desht.scrollingmenusign.SMSConfig;

import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;

public enum SMSUserAction {
	NONE, SCROLLDOWN, SCROLLUP, EXECUTE;
	
	public static SMSUserAction getAction(PlayerInteractEvent event) {
		Action a = event.getAction();
		Player player = event.getPlayer();
		
		StringBuilder key;
		if (a == Action.RIGHT_CLICK_BLOCK ) {
			key = new StringBuilder("sms.actions.rightclick.");
		} else if (a == Action.LEFT_CLICK_BLOCK) {
			key = new StringBuilder("sms.actions.leftclick.");
		} else {
			return null;
		}
		return _makeAction(player, key);
	}

	public static SMSUserAction getAction(PlayerItemHeldEvent event) {
		Player player = event.getPlayer();
		
		int delta = event.getNewSlot() - event.getPreviousSlot();
		StringBuilder key;
		if (delta == -1 || delta == 8) {
			key = new StringBuilder("sms.actions.wheelup.");
		} else if (delta == 1 || delta == -8) {
			key = new StringBuilder("sms.actions.wheeldown.");
		} else {
			return null;
		}
		return _makeAction(player, key);
	}

	private static SMSUserAction _makeAction(Player player, StringBuilder key) {
		if (player.isSneaking())
			key.append("sneak");
		else 
			key.append("normal");
		
		String s = SMSConfig.getConfiguration().getString(key.toString(), "none");
		return SMSUserAction.valueOf(s.toUpperCase());
	}
}
