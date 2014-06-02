package me.desht.scrollingmenusign.commands;

import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSMenuItem;
import me.desht.scrollingmenusign.SMSValidate;
import me.desht.scrollingmenusign.parser.CommandParser;
import me.desht.scrollingmenusign.util.SMSUtil;
import me.desht.scrollingmenusign.views.action.UpdateItemAction;
import org.apache.commons.lang.StringEscapeUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.List;

public class EditMenuCommand extends SMSAbstractCommand {

    public EditMenuCommand() {
        super("sms edit", 3);
        setPermissionNode("scrollingmenusign.commands.edit");
        setUsage(new String[]{
                "/sms edit <menu-name> @<pos> <options...>",
                "/sms edit <menu-name> <label> <options...>",
                "Options:",
                "  -label <str>         The new item label",
                "  -command <str>       The new command to run",
                "  -altcommand <str>    The new alternative command to run",
                "  -feedback <str>      The new feedback message to display",
                "  -icon <str>          The new material used for the item's icon",
                "  -perm <str>          The permission node to see/use this item",
                "  -lore <str>          The new lore for the item (use '+text' to append)",
                "  -move <pos>          The new position in the menu for the item",
        });
        setQuotedArgs(true);
        setOptions("label:s", "command:s", "altcommand:s", "feedback:s", "icon:s", "move:i", "lore:s", "perm:s");
    }

    @Override
    public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
        SMSMenu menu = getMenu(sender, args[0]);
        menu.ensureAllowedToModify(sender);

        int pos;
        if (args[1].startsWith("@")) {
            try {
                pos = Integer.parseInt(args[1].substring(1));
            } catch (NumberFormatException e) {
                throw new SMSException(e.getMessage() + " bad numeric index");
            }
        } else {
            pos = menu.indexOfItem(args[1]);
        }

        SMSMenuItem currentItem = menu.getItemAt(pos, true);
        String label = hasOption("label") ? SMSUtil.unEscape(getStringOption("label")) : currentItem.getLabel();
        String command = hasOption("command") ? StringEscapeUtils.unescapeHtml(getStringOption("command")) : currentItem.getCommand();
        String altCommand = hasOption("altcommand") ? StringEscapeUtils.unescapeHtml(getStringOption("altcommand")) : currentItem.getAltCommand();
        String message = hasOption("feedback") ? SMSUtil.unEscape(getStringOption("feedback")) : currentItem.getMessage();
        ItemStack icon = hasOption("icon") ? SMSUtil.parseMaterialSpec(getStringOption("icon")) : currentItem.getIcon();
        String perm = hasOption("perm") ? getStringOption("perm") : currentItem.getPermissionNode();
        String[] lore = buildNewLore(currentItem);

        if (!command.isEmpty() && sender instanceof Player && !new CommandParser().verifyCreationPerms((Player) sender, command)) {
            throw new SMSException("You do not have permission to add that kind of command.");
        }

        SMSMenuItem newItem = new SMSMenuItem.Builder(menu, label)
                .withCommand(command)
                .withAltCommand(altCommand)
                .withMessage(message)
                .withIcon(icon)
                .withLore(lore)
                .withPermissionNode(perm)
                .withUseLimits(currentItem.getUseLimits())
                .build();

        if (hasOption("move")) {
            int newPos = getIntOption("move");
            SMSValidate.isTrue(newPos >= 1 && newPos <= menu.getItemCount(), "Invalid position for -move: " + newPos);
            menu.removeItem(pos);
            menu.insertItem(newPos, newItem);
            MiscUtil.statusMessage(sender, "Menu item &f" + label + "&- edited in &e" + menu.getName() + "&-, new position &e" + newPos);
        } else {
            menu.replaceItem(pos, newItem);
            MiscUtil.statusMessage(sender, "Menu item &f" + label + "&- edited in &e" + menu.getName() + "&-, position &e" + pos);
        }

        menu.notifyObservers(new UpdateItemAction(sender, currentItem, newItem));

        return true;
    }

    private String[] buildNewLore(SMSMenuItem item) {
        List<String> lore = item.getLoreAsList();
        if (hasOption("lore")) {
            String l = SMSUtil.unEscape(getStringOption("lore"));
            String l1;
            if (l.startsWith("+") && l.length() > 1) {
                l1 = l.substring(1);
            } else {
                lore.clear();
                l1 = l;
            }
            if (!l1.isEmpty()) {
                Collections.addAll(lore, l1.split("\\\\\\\\"));
            }
        }
        return lore.toArray(new String[lore.size()]);
    }

    @Override
    public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
        switch (args.length) {
            case 1:
                return getMenuCompletions(plugin, sender, args[0]);
            case 2:
                SMSMenu menu = getMenu(sender, args[0]);
                return getMenuItemCompletions(sender, menu, args[1]);
            default:
                showUsage(sender);
                return noCompletions(sender);
        }
    }
}
