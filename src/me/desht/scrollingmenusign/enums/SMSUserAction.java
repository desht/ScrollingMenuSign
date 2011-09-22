package me.desht.scrollingmenusign.enums;

import me.desht.scrollingmenusign.SMSConfig;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;

public enum SMSUserAction {
	NONE, SCROLLDOWN, SCROLLUP, EXECUTE;
	
	public static SMSUserAction getAction(PlayerInteractEvent event) {
		StringBuilder key;
		switch (event.getAction()) {
		case RIGHT_CLICK_BLOCK:
//		case RIGHT_CLICK_AIR:
			key = new StringBuilder("sms.actions.rightclick.");
			break;
		case LEFT_CLICK_BLOCK:
//		case LEFT_CLICK_AIR:
			key = new StringBuilder("sms.actions.leftclick.");
			break;
		default:
			return null;	
		}
		
		return _makeAction(event.getPlayer(), key);
	}

	public static SMSUserAction getAction(PlayerItemHeldEvent event) {
		int delta = event.getNewSlot() - event.getPreviousSlot();
		StringBuilder key;
		if (delta == -1 || delta == 8) {
			key = new StringBuilder("sms.actions.wheelup.");
		} else if (delta == 1 || delta == -8) {
			key = new StringBuilder("sms.actions.wheeldown.");
		} else {
			return null;
		}
		return _makeAction(event.getPlayer(), key);
	}

	public static SMSUserAction getAction(PlayerAnimationEvent event) {
		switch (event.getAnimationType()) {
		case ARM_SWING:
			StringBuilder key = new StringBuilder("sms.actions.leftclick.");
			return _makeAction(event.getPlayer(), key);
		default:
			return null;	
		}
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
