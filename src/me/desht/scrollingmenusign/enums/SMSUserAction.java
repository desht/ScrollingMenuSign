package me.desht.scrollingmenusign.enums;

import java.util.Set;
import java.util.logging.Level;

import me.desht.scrollingmenusign.SMSConfig;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSMenuItem;
import me.desht.scrollingmenusign.views.SMSMapView;
import me.desht.scrollingmenusign.views.SMSScrollableView;
import me.desht.scrollingmenusign.views.SMSView;
import me.desht.util.MiscUtil;
import me.desht.util.PermissionsUtils;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.getspout.spoutapi.event.input.KeyPressedEvent;
import org.getspout.spoutapi.keyboard.Keyboard;

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

	public static SMSUserAction getAction(KeyPressedEvent event) {
		if (event.getKey() == getSpoutKey("up", "up")) {
			return SCROLLUP;
		} else if (event.getKey() == getSpoutKey("down", "down")) {
			return SCROLLDOWN;
		} else if (event.getKey() == getSpoutKey("execute", "return")) {
			return EXECUTE;
		}
		return null;
	}

	public static SMSUserAction getAction(Set<Keyboard> pressed) {
		if (tryKeyboardMatch("sms.actions.spout.up", "key_up", pressed)) {
			return SCROLLUP;
		} else if (tryKeyboardMatch("sms.actions.spout.down", "key_down", pressed)) {
			return SCROLLDOWN;
		} else if (tryKeyboardMatch("sms.actions.spout.execute", "key_return", pressed)) {
			return EXECUTE;
		}
		
		return null;
	}

	private static boolean tryKeyboardMatch(String key, String def, Set<Keyboard> pressed) {
		String[] wanted = SMSConfig.getConfiguration().getString(key, def).split("\\+");
		int matched = 0;
		for (String w : wanted) {
			try {
				Keyboard kw = Keyboard.valueOf(w.toUpperCase());
				if (pressed.contains(kw)) {
					matched++;
				}
			} catch (IllegalArgumentException e) {
				MiscUtil.log(Level.WARNING, "Unknown Spout key definition " + w + " in sms.actions.spout.up");
			}
		}
		return matched == wanted.length && pressed.size() == wanted.length;
	}

	private static Keyboard getSpoutKey(String input, String def) {
		String key = SMSConfig.getConfiguration().getString("sms.actions.spout." + input, def);
		try {
			return Keyboard.valueOf(key.toUpperCase());
		} catch (IllegalArgumentException e) {
			MiscUtil.log(Level.WARNING, "Unknown Spout key definition for " + key);
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
	
	public void execute(Player player, SMSView view) throws SMSException {

		if (!view.allowedToUse(player))
			throw new SMSException("This " + view.getType() + " belongs to someone else.");

		// this method only makes sense for scrollable views
		
		SMSScrollableView sview;
		if (view instanceof SMSScrollableView) {
			sview = (SMSScrollableView) view;
		} else {
			return;
		}
		SMSMenu menu = sview.getMenu();
		switch (this) {
		case EXECUTE:
			if (sview instanceof SMSMapView)
				PermissionsUtils.requirePerms(player, "scrollingmenusign.maps");
			SMSMenuItem item = menu.getItem(sview.getScrollPos());
			if (item != null) {
				item.execute(player);
				item.feedbackMessage(player);
			}
			break;
		case SCROLLDOWN:
			sview.scrollDown();
			sview.update(menu, SMSMenuAction.REPAINT);
			break;
		case SCROLLUP:
			sview.scrollUp();
			sview.update(menu, SMSMenuAction.REPAINT);
			break;
		}
	}
}
