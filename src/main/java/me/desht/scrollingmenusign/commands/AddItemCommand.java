package me.desht.scrollingmenusign.commands;

import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.*;
import me.desht.scrollingmenusign.parser.CommandParser;
import me.desht.scrollingmenusign.util.SMSUtil;
import me.desht.scrollingmenusign.views.action.AddItemAction;
import org.apache.commons.lang.StringEscapeUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class AddItemCommand extends SMSAbstractCommand {

    public AddItemCommand() {
        super("sms add", 2);
        setPermissionNode("scrollingmenusign.commands.add");
        setUsage(new String[]{
                "/sms add <menu-name> <label> [<command>] [<options...>]",
                "Options (-at takes integer, all others take string):",
                "  -at         Position to add the new item at",
                "  -altcommand The alternative command to run",
                "  -feedback   The new feedback message to display",
                "  -icon       The new material used for the item's icon",
                "  -perm       The permission node to see/use this item",
                "  -lore       The lore for the item (line delimiter '\\\\')",
        });
        setQuotedArgs(true);
        setOptions("at:i", "altcommand:s", "feedback:s", "icon:s", "lore:s", "perm:s");
    }

    @Override
    public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
        String menuName = args[0];

        SMSMenu menu = getMenu(sender, menuName);

        if (args.length < 3 && menu.getDefaultCommand().isEmpty()) {
            throw new SMSException(getUsage()[0]);
        }

        menu.ensureAllowedToModify(sender);

        int pos = hasOption("at") ? getIntOption("at") : -1;
        String label = SMSUtil.unEscape(args[1]);
        String cmd = args.length >= 3 ? StringEscapeUtils.unescapeHtml(args[2]) : "";
        String altCmd = hasOption("altcommand") ? StringEscapeUtils.unescapeHtml(getStringOption("altcommand")) : "";
        String msg = hasOption("feedback") ? SMSUtil.unEscape(getStringOption("feedback")) : "";
        String iconMat = getStringOption("icon");
        String[] lore = hasOption("lore") ? SMSUtil.unEscape(getStringOption("lore")).split("\\\\\\\\") : new String[0];
        String perm = hasOption("perm") ? getStringOption("perm") : "";

        SMSValidate.isFalse(sender instanceof Player && !new CommandParser().verifyCreationPerms((Player) sender, cmd),
                "You do not have permission to add that kind of command.");

        SMSMenuItem newItem = new SMSMenuItem.Builder(menu, label)
                .withCommand(cmd)
                .withMessage(msg)
                .withIcon(iconMat)
                .withAltCommand(altCmd)
                .withLore(lore)
                .withPermissionNode(perm)
                .build();

        if (pos < 0) {
            menu.addItem(newItem);
            MiscUtil.statusMessage(sender, "Menu item &f" + label + "&- added to &e" + menu.getName());
        } else {
            menu.insertItem(pos, newItem);
            int actualPos = menu.indexOfItem(label);
            MiscUtil.statusMessage(sender, "Menu item &f" + label + "&- inserted in &e" + menu.getName() + "&- at position " + actualPos);
        }

        menu.notifyObservers(new AddItemAction(sender, newItem));

        return true;
    }

    @Override
    public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
        if (args.length == 1) {
            return getMenuCompletions(plugin, sender, args[0]);
        } else {
            showUsage(sender);
            return noCompletions(sender);
        }
    }
}
