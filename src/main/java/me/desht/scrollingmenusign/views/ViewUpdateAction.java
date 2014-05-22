package me.desht.scrollingmenusign.views;

import me.desht.scrollingmenusign.enums.SMSMenuAction;
import org.bukkit.entity.Player;

public class ViewUpdateAction {
    private final SMSMenuAction action;
    private final Player player;

    public ViewUpdateAction(SMSMenuAction action, Player player) {
        this.action = action;
        this.player = player;
    }

    public ViewUpdateAction(SMSMenuAction action) {
        this.action = action;
        this.player = null;
    }

    public SMSMenuAction getAction() {
        return action;
    }

    public Player getPlayer() {
        return player;
    }

    public static ViewUpdateAction getAction(Object o) {
        if (o instanceof SMSMenuAction) {
            return new ViewUpdateAction((SMSMenuAction) o, null);
        } else if (o instanceof ViewUpdateAction) {
            return (ViewUpdateAction) o;
        } else {
            throw new IllegalArgumentException("Expecting a ViewUpdateAction or SMSMenuAction object");
        }
    }
}
