package me.desht.scrollingmenusign.enums;


import me.desht.scrollingmenusign.SMSConfig;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSMenuItem;
import me.desht.scrollingmenusign.util.PermissionsUtils;
import me.desht.scrollingmenusign.views.SMSScrollableView;
import me.desht.scrollingmenusign.views.SMSView;

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
		case RIGHT_CLICK_AIR:
			key = new StringBuilder("sms.actions.rightclick.");
			break;
		case LEFT_CLICK_BLOCK:
		case LEFT_CLICK_AIR:
			key = new StringBuilder("sms.actions.leftclick.");
			break;
		default:
			return NONE;	
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
			return NONE;
		}
		return _makeAction(event.getPlayer(), key);
	}

	public static SMSUserAction getAction(PlayerAnimationEvent event) {
		switch (event.getAnimationType()) {
		case ARM_SWING:
			StringBuilder key = new StringBuilder("sms.actions.leftclick.");
			return _makeAction(event.getPlayer(), key);
		default:
			return NONE;	
		}
	}

	private static SMSUserAction _makeAction(Player player, StringBuilder key) {
		if (player.isSneaking())
			key.append("sneak");
		else 
			key.append("normal");

		String s = SMSConfig.getConfig().getString(key.toString(), "none");
		return SMSUserAction.valueOf(s.toUpperCase());
	}

	public void execute(Player player, SMSView view) throws SMSException {
		if (this == NONE)
			return;
		
		if (!view.allowedToUse(player))
			throw new SMSException("This " + view.getType() + " belongs to someone else.");

		// this method only makes sense for scrollable views
		if (!(view instanceof SMSScrollableView)) {
			return;
		}
		
		PermissionsUtils.requirePerms(player, "scrollingmenusign.use." + view.getType());
		
		SMSScrollableView sview = (SMSScrollableView) view;
		SMSMenu menu = sview.getMenu();
		switch (this) {
		case EXECUTE:
			PermissionsUtils.requirePerms(player, "scrollingmenusign.execute");
			SMSMenuItem item = menu.getItem(sview.getScrollPos(player.getName()));
			if (item != null) {
				item.execute(player);
				item.feedbackMessage(player);
				view.onExecuted(player);
			}
			break;
		case SCROLLDOWN:
			PermissionsUtils.requirePerms(player, "scrollingmenusign.scroll");
			sview.scrollDown(player.getName());
			sview.update(menu, SMSMenuAction.SCROLLED);
			break;
		case SCROLLUP:
			PermissionsUtils.requirePerms(player, "scrollingmenusign.scroll");
			sview.scrollUp(player.getName());
			sview.update(menu, SMSMenuAction.SCROLLED);
			break;
		}
	}
}
