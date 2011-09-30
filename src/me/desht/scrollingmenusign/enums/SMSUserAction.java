package me.desht.scrollingmenusign.enums;

import me.desht.scrollingmenusign.SMSConfig;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSMenuItem;
import me.desht.scrollingmenusign.views.SMSMapView;
import me.desht.scrollingmenusign.views.SMSScrollableView;
import me.desht.scrollingmenusign.views.SMSView;
import me.desht.util.PermissionsUtils;

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
