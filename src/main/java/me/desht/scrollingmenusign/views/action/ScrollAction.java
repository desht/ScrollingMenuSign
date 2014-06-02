package me.desht.scrollingmenusign.views.action;

import org.bukkit.command.CommandSender;

public class ScrollAction extends ViewUpdateAction {
    private final ScrollDirection direction;

    public ScrollAction(CommandSender sender, ScrollDirection dir) {
        super(sender);
        this.direction = dir;
    }

    public ScrollDirection getDirection() {
        return direction;
    }

    public enum ScrollDirection { UP, DOWN, UNKNOWN }
}
