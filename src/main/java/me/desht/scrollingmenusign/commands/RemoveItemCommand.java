package me.desht.scrollingmenusign.commands;

import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSMenuItem;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.util.SMSUtil;
import me.desht.scrollingmenusign.views.action.RemoveItemAction;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class RemoveItemCommand extends SMSAbstractCommand {

    public RemoveItemCommand() {
        super("sms remove", 2, 2);
        setPermissionNode("scrollingmenusign.commands.remove");
        setUsage(new String[]{
                "/sms remove <menu-name> @<pos>",
                "/sms remove <menu-name> <item-label>"
        });
        setQuotedArgs(true);
    }

    @Override
    public boolean execute(Plugin plugin, CommandSender sender, String[] args) {

        String menuName = args[0];
        String itemLabel = SMSUtil.unEscape(args[1]);

        if (itemLabel.matches("@[0-9]+")) {
            // backwards compatibility - numeric indices should be prefixed with a '@'
            // but we'll allow raw numbers to be used
            itemLabel = itemLabel.substring(1);
        }

        try {
            SMSMenu menu = getMenu(sender, menuName);
            menu.ensureAllowedToModify(sender);
            int pos = menu.indexOfItem(itemLabel);
            SMSMenuItem menuItem = menu.getItemAt(pos);
            menu.removeItem(pos);
            menu.notifyObservers(new RemoveItemAction(sender, menuItem));
            MiscUtil.statusMessage(sender, "Menu entry &f#" + itemLabel + "&- removed from &e" + menu.getName());
        } catch (IndexOutOfBoundsException e) {
            throw new SMSException("Item index " + itemLabel + " out of range");
        } catch (IllegalArgumentException e) {
            throw new SMSException(e.getMessage());
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
        switch (args.length) {
            case 1:
                return getMenuCompletions(plugin, sender, args[0]);
            case 2:
                SMSMenu menu = ((ScrollingMenuSign) plugin).getMenuManager().getMenu(args[0]);
                return getMenuItemCompletions(sender, menu, args[1]);
            default:
                showUsage(sender);
                return noCompletions(sender);
        }
    }
}
