package me.desht.scrollingmenusign.enums;

import me.desht.dhutils.PermissionUtils;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSMenuItem;
import me.desht.scrollingmenusign.ScrollingMenuSign;
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

		String s = ScrollingMenuSign.getInstance().getConfig().getString(key.toString(), "none");
		return SMSUserAction.valueOf(s.toUpperCase());
	}

	public void execute(Player player, SMSView view) throws SMSException {
		if (this == NONE)
			return;
		if (!(view instanceof SMSScrollableView)) {
			// this method only makes sense for scrollable views
			return;
		}

		if (player != null) {
			view.ensureAllowedToUse(player);
			PermissionUtils.requirePerms(player, getPermissionNode());
		}

		SMSScrollableView sview = (SMSScrollableView) view;
		String playerName = player == null ? "CONSOLE" : player.getName();
		SMSMenu menu = sview.getActiveMenu(playerName);
		switch (this) {
		case EXECUTE:
//			SMSMenuItem item = menu.getItemAt(sview.getScrollPos(playerName));
			SMSMenuItem item = sview.getActiveMenuItemAt(playerName, sview.getScrollPos(playerName));
			if (item != null) {
				item.executeCommand(player, view);
				item.feedbackMessage(player);
				view.onExecuted(player);
			}
			break;
		case SCROLLDOWN:
			sview.scrollDown(playerName);
			sview.update(menu, SMSMenuAction.SCROLLED);
			sview.onScrolled(player, SCROLLDOWN);
			break;
		case SCROLLUP:
			sview.scrollUp(playerName);
			sview.update(menu, SMSMenuAction.SCROLLED);
			sview.onScrolled(player, SCROLLUP);
			break;
		default:
			break;
		}
	}

	public String getPermissionNode() {
		switch (this) {
		case EXECUTE: return "scrollingmenusign.execute";
		case SCROLLDOWN: case SCROLLUP: return "scrollingmenusign.scroll";
		default: return null;
		}
	}

	public String getShortDesc() {
		switch (this) {
		case EXECUTE: return "X";
		case SCROLLUP: return "U";
		case SCROLLDOWN: return "D";
		default: return "N";
		}
	}
}
