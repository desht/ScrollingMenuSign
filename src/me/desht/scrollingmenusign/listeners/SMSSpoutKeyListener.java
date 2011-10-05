package me.desht.scrollingmenusign.listeners;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SpoutUtils;
import me.desht.scrollingmenusign.enums.SMSUserAction;
import me.desht.scrollingmenusign.views.SMSMapView;
import me.desht.scrollingmenusign.views.SMSView;
import me.desht.util.MiscUtil;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.getspout.spoutapi.event.input.InputListener;
import org.getspout.spoutapi.event.input.KeyPressedEvent;
import org.getspout.spoutapi.event.input.KeyReleasedEvent;
import org.getspout.spoutapi.gui.ScreenType;
import org.getspout.spoutapi.keyboard.Keyboard;
import org.getspout.spoutapi.player.SpoutPlayer;

public class SMSSpoutKeyListener extends InputListener {
	private static final Map<String, Set<Keyboard>> pressedKeys = new HashMap<String, Set<Keyboard>>();
	
	public SMSSpoutKeyListener() {
		SpoutUtils.loadKeyDefinitions();
	}

	@Override
	public void onKeyPressedEvent(KeyPressedEvent event) {

		SpoutPlayer player = event.getPlayer();

		Set<Keyboard> pressed = getPressedKeys(player);
		if (event.getKey() == Keyboard.KEY_ESCAPE) {
			// special case - Escape always resets the pressed key set
			pressed.clear();
		} else {
			pressed.add(event.getKey());
		}
		
		// don't actually do any action unless we're on the game screen
		if (event.getScreenType() != ScreenType.GAME_SCREEN)
			return;
		
		try {
			Block block = player.getTargetBlock(null, 3);

			SMSView view = SMSView.getViewForLocation(block.getLocation());
			if (view == null) {
				if (player.getItemInHand().getTypeId() == 358) {
					view = SMSMapView.getViewForId(player.getItemInHand().getDurability());	
				}
			}
			if (view != null) {
				SMSUserAction action = getAction(pressed);
				action.execute(player, view);
			}
		} catch (SMSException e) {
			MiscUtil.errorMessage(player, e.getMessage());
		} catch (IllegalStateException e) {
			// can be ignored
		}
	}

	@Override
	public void onKeyReleasedEvent(KeyReleasedEvent event) {
		getPressedKeys(event.getPlayer()).remove(event.getKey());
	}

	private Set<Keyboard> getPressedKeys(Player player) {
		if (!pressedKeys.containsKey(player.getName())) {
			pressedKeys.put(player.getName(), new HashSet<Keyboard>());
		}
		return pressedKeys.get(player.getName());
	}

	private static SMSUserAction getAction(Set<Keyboard> pressed) {
		if (SpoutUtils.tryKeyboardMatch("sms.actions.spout.up", pressed)) {
			return SMSUserAction.SCROLLUP;
		} else if (SpoutUtils.tryKeyboardMatch("sms.actions.spout.down", pressed)) {
			return SMSUserAction.SCROLLDOWN;
		} else if (SpoutUtils.tryKeyboardMatch("sms.actions.spout.execute", pressed)) {
			return SMSUserAction.EXECUTE;
		}

		return SMSUserAction.NONE;
	}

}
