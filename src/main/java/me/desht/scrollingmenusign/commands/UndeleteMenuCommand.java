package me.desht.scrollingmenusign.commands;

import java.util.List;

import me.desht.dhutils.MessagePager;
import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.MenuManager;
import me.desht.scrollingmenusign.SMSMenu;

import me.desht.scrollingmenusign.ScrollingMenuSign;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class UndeleteMenuCommand extends SMSAbstractCommand {

    public UndeleteMenuCommand() {
        super("sms undelete", 1, 1);
        setPermissionNode("scrollingmenusign.commands.undelete");
        setUsage(new String[]{
                "/sms undelete <menu-name>",
                "/sms undelete -l",
        });
        setOptions("l");
    }

    @Override
    public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
        MenuManager menuManager = ((ScrollingMenuSign) plugin).getMenuManager();

        if (getBooleanOption("l")) {
            MessagePager pager = MessagePager.getPager(sender).clear().setParseColours(true);
            List<String> list = menuManager.listDeletedMenus();
            String s = list.size() == 1 ? "" : "s";
            pager.add(list.size() + " deleted menu" + s + ":");
            for (String name : MiscUtil.asSortedList(menuManager.listDeletedMenus())) {
                pager.add(MessagePager.BULLET + " " + name);
            }
            pager.showPage();
        } else {
            SMSMenu menu = menuManager.getDeletedMenu(args[0]);
            menu.ensureAllowedToModify(sender);
            menu = menuManager.restoreDeletedMenu(args[0]);
            MiscUtil.statusMessage(sender, "Restored deleted menu &e" + menu.getName() + "&-.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
        if (args.length == 1) {
            return getResult(((ScrollingMenuSign) plugin).getMenuManager().listDeletedMenus(), sender, true);
        } else {
            showUsage(sender);
            return noCompletions(sender);
        }
    }
}
