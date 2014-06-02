package me.desht.scrollingmenusign.views.action;

import org.bukkit.command.CommandSender;

public class TitleAction extends ViewUpdateAction {
    private final String newTitle;

    public TitleAction(CommandSender sender, String newTitle) {
        super(sender);
        this.newTitle = newTitle;
    }

    public String getNewTitle() {
        return newTitle;
    }
}
