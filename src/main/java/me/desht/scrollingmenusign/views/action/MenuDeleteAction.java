package me.desht.scrollingmenusign.views.action;

import org.bukkit.command.CommandSender;

public class MenuDeleteAction extends ViewUpdateAction {
    private final boolean permanent;

    public MenuDeleteAction(CommandSender sender, boolean permanent) {
        super(sender);
        this.permanent = permanent;
    }

    public boolean isPermanent() {
        return permanent;
    }
}
