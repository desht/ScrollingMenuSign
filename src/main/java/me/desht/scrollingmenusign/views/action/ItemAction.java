package me.desht.scrollingmenusign.views.action;

import me.desht.scrollingmenusign.SMSMenuItem;
import org.bukkit.command.CommandSender;

public abstract class ItemAction extends ViewUpdateAction {
    private final SMSMenuItem modifiedItem;

    public ItemAction(CommandSender sender, SMSMenuItem modifiedItem) {
        super(sender);
        this.modifiedItem = modifiedItem;
    }

    public SMSMenuItem getModifiedItem() {
        return modifiedItem;
    }
}
