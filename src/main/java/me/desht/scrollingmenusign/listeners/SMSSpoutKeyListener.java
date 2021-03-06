package me.desht.scrollingmenusign.listeners;

import me.desht.dhutils.Debugger;
import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.enums.SMSUserAction;
import me.desht.scrollingmenusign.spout.SMSGenericPopup;
import me.desht.scrollingmenusign.spout.SMSSpoutKeyMap;
import me.desht.scrollingmenusign.spout.SpoutUtils;
import me.desht.scrollingmenusign.spout.SpoutViewPopup;
import me.desht.scrollingmenusign.views.SMSSpoutView;
import me.desht.scrollingmenusign.views.SMSView;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.getspout.spoutapi.event.input.KeyPressedEvent;
import org.getspout.spoutapi.event.input.KeyReleasedEvent;
import org.getspout.spoutapi.gui.PopupScreen;
import org.getspout.spoutapi.gui.ScreenType;
import org.getspout.spoutapi.keyboard.Keyboard;
import org.getspout.spoutapi.player.SpoutPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SMSSpoutKeyListener extends SMSListenerBase {
    private final Map<UUID, SMSSpoutKeyMap> pressedKeys = new HashMap<UUID, SMSSpoutKeyMap>();

    public SMSSpoutKeyListener(ScrollingMenuSign plugin) {
        super(plugin);

        SpoutUtils.loadKeyDefinitions();
    }

    @EventHandler
    public void onKeyPressedEvent(KeyPressedEvent event) {
        SpoutPlayer sp = event.getPlayer();

        SMSSpoutKeyMap pressed = getPressedKeys(sp);
        if (event.getKey() == Keyboard.KEY_ESCAPE) {
            // special case - Escape always resets the pressed key set
            pressed.clear();
        } else {
            pressed.add(event.getKey());
        }

        // only interested in keypresses on the main screen or one of our custom popups
        if (event.getScreenType() != ScreenType.GAME_SCREEN && event.getScreenType() != ScreenType.CUSTOM_SCREEN)
            return;
        // and if there's a custom screen up belonging to another plugin, we stop here too
        PopupScreen s = sp.getMainScreen().getActivePopup();
        if (s != null && !(s instanceof SMSGenericPopup))
            return;

        try {
            // see if any existing spout view has a mapping for the pressed keys
            if (SMSSpoutView.handleKeypress(sp, pressed)) {
                return;
            }

            // otherwise, check for use of the scroll/execute keys on a targeted view
            SMSView view = findViewForPlayer(sp);
            if (view != null) {
                SMSUserAction action = getAction(pressed);
                Debugger.getInstance().debug("spout keypress event: keys pressed = " + pressed
                        + ", view = " + view.getName() + ", menu = " + view.getActiveMenu(sp).getName()
                        + ", action = " + action);
                action.execute(sp, view);
            }
        } catch (SMSException e) {
            MiscUtil.errorMessage(sp, e.getMessage());
        } catch (IllegalStateException e) {
            // can be ignored
        }
    }

    @EventHandler
    public void onKeyReleasedEvent(KeyReleasedEvent event) {
        getPressedKeys(event.getPlayer()).remove(event.getKey());
    }

    private SMSView findViewForPlayer(SpoutPlayer player) {
        SMSView view = null;

        // is there an open spout view for this player?
        PopupScreen popup = player.getMainScreen().getActivePopup();
        if (popup != null && popup instanceof SpoutViewPopup) {
            view = ((SpoutViewPopup) popup).getView();
        }

        // check if user is targeting any other kind of view
        if (view == null) {
            view = ScrollingMenuSign.getInstance().getViewManager().getTargetedView(player);
        }

        return view;
    }


    private SMSSpoutKeyMap getPressedKeys(Player player) {
        if (!pressedKeys.containsKey(player.getUniqueId())) {
            pressedKeys.put(player.getUniqueId(), new SMSSpoutKeyMap());
        }
        return pressedKeys.get(player.getUniqueId());
    }

    private SMSUserAction getAction(SMSSpoutKeyMap pressed) {
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
