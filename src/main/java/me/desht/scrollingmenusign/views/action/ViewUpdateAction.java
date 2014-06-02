package me.desht.scrollingmenusign.views.action;

import me.desht.scrollingmenusign.enums.SMSMenuAction;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public abstract class ViewUpdateAction {
//    private final SMSMenuAction action;
    private final CommandSender sender;

    public ViewUpdateAction(CommandSender sender) {
//        this.action = action;
        this.sender = sender;
    }

    public ViewUpdateAction() {
//        this.action = action;
        this.sender = null;
    }
//
//    public SMSMenuAction getAction() {
//        return action;
//    }

    public CommandSender getSender() {
        return sender;
    }

    public static ViewUpdateAction getAction(Object o) {
        if (o instanceof SMSMenuAction) {
            switch ((SMSMenuAction) o) {
//            return new ViewUpdateAction((SMSMenuAction) o, null);
                case SCROLLED: return new ScrollAction(null, ScrollAction.ScrollDirection.UNKNOWN);
                case REPAINT: return new RepaintAction();
                default: return null;
            }
        } else if (o instanceof ViewUpdateAction) {
            return (ViewUpdateAction) o;
        } else {
            throw new IllegalArgumentException("Expecting a ViewUpdateAction or SMSMenuAction object");
        }
    }
}
