package me.desht.scrollingmenusign.listeners;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.enums.SMSUserAction;
import me.desht.scrollingmenusign.spout.SpoutUtils;
import me.desht.scrollingmenusign.views.SMSMapView;
import me.desht.scrollingmenusign.views.SMSSpoutView;
import me.desht.scrollingmenusign.views.SMSView;
import me.desht.util.Debugger;
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
			// first see if any existing spout view has a mapping for the pressed keys
			if (SMSSpoutView.handleKeypress(player, pressed)) {
				return;
			}

			SMSView view = findViewForPlayer(player);
			if (view != null) {			
				SMSUserAction action = getAction(pressed);
				Debugger.getDebugger().debug("spout keypress event: keys pressed = " + pressed
						+ ", view = " + view.getName() + ", menu = " + view.getMenu().getName()
						+ ", action = " + action);
				action.execute(player, view);
			}
		} catch (SMSException e) {
			MiscUtil.errorMessage(player, e.getMessage());
		} catch (IllegalStateException e) {
			// can be ignored
		}
	}

	private SMSView findViewForPlayer(SpoutPlayer player) {
		SMSView view = null;
		
		// is there an open spout view?
		if (SMSSpoutView.hasActiveGUI(player)) {
			view = SMSSpoutView.getGUI(player).getView();
		}
		// check for a map view...
		if (view == null) {
			if (player.getItemInHand().getTypeId() == 358) {
				view = SMSMapView.getViewForId(player.getItemInHand().getDurability());	
			}
		}
		// check if user is looking at a sign view...
		if (view == null) {
			Block block = player.getTargetBlock(null, 3);
			view = SMSView.getViewForLocation(block.getLocation());
		}
		
		return view;
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
