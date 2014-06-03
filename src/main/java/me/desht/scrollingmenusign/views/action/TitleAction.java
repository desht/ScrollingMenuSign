package me.desht.scrollingmenusign.views.action;

import org.bukkit.command.CommandSender;

public class TitleAction extends ViewUpdateAction {
    private final String oldTitle;
    private final String newTitle;

    public TitleAction(CommandSender sender, String oldTitle, String newTitle) {
        super(sender);
        this.oldTitle = oldTitle;
        this.newTitle = newTitle;
    }

    public String getOldTitle() {
        return oldTitle;
    }

    public String getNewTitle() {
        return newTitle;
    }
}
