package me.desht.scrollingmenusign.listeners;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import me.desht.scrollingmenusign.SMSConfig;
import me.desht.scrollingmenusign.SMSException;
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

public class SMSSpoutKeyListener extends InputListener {
	private static final Map<String, Set<Keyboard>> pressedKeys = new HashMap<String, Set<Keyboard>>();
	
	public SMSSpoutKeyListener() {
	}
	
	@Override
	public void onKeyPressedEvent(KeyPressedEvent event) {
		if (event.getScreenType() != ScreenType.GAME_SCREEN)
			return;

		Player player = event.getPlayer();
		
		Set<Keyboard> pressed = getPressedKeys(player);
		pressed.add(event.getKey());
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
		if (event.getScreenType() != ScreenType.GAME_SCREEN)
			return;
		
		getPressedKeys(event.getPlayer()).remove(event.getKey());
	}
	
	private Set<Keyboard> getPressedKeys(Player player) {
		if (!pressedKeys.containsKey(player.getName())) {
			pressedKeys.put(player.getName(), new HashSet<Keyboard>());
		}
		return pressedKeys.get(player.getName());
	}

	private static SMSUserAction getAction(Set<Keyboard> pressed) {
		if (tryKeyboardMatch("sms.actions.spout.up", "key_up", pressed)) {
			return SMSUserAction.SCROLLUP;
		} else if (tryKeyboardMatch("sms.actions.spout.down", "key_down", pressed)) {
			return SMSUserAction.SCROLLDOWN;
		} else if (tryKeyboardMatch("sms.actions.spout.execute", "key_return", pressed)) {
			return SMSUserAction.EXECUTE;
		}

		return SMSUserAction.NONE;
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
}
