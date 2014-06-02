package me.desht.scrollingmenusign.views.action;

import me.desht.scrollingmenusign.SMSMenuItem;
import org.bukkit.command.CommandSender;

public class UpdateItemAction extends ItemAction {
    private final SMSMenuItem newItem;

    public UpdateItemAction(CommandSender sender, SMSMenuItem modifiedItem, SMSMenuItem newItem) {
        super(sender, modifiedItem);
        this.newItem = newItem;
    }

    public SMSMenuItem getNewItem() {
        return newItem;
    }
}
