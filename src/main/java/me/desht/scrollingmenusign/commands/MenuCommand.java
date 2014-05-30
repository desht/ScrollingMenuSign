package me.desht.scrollingmenusign.commands;

import me.desht.dhutils.ItemNames;
import me.desht.dhutils.MessagePager;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PermissionUtils;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSMenuItem;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.util.SMSUtil;
import me.desht.scrollingmenusign.views.ActiveItem;
import me.desht.scrollingmenusign.views.SMSView;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class MenuCommand extends SMSAbstractCommand {

    public MenuCommand() {
        super("sms menu", 0, 3);
        setPermissionNode("scrollingmenusign.commands.menu");
        setUsage(new String[]{
                "/sms menu <menu-name> [<attribute>] [<value>]",
        });
        setQuotedArgs(true);
    }

    @Override
    public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
        SMSMenu menu;
        SMSView view = null;
        if (args.length > 0 && !args[0].equals(".")) {
            menu = ((ScrollingMenuSign) plugin).getMenuManager().getMenu(args[0]);
        } else {
            notFromConsole(sender);
            Player player = (Player) sender;
            if (ActiveItem.isActiveItem(player.getItemInHand())) {
                menu = new ActiveItem(player.getItemInHand()).getActiveMenu();
            } else {
                view = ScrollingMenuSign.getInstance().getViewManager().getTargetedView(player, true);
                menu = view.getActiveMenu(player);
            }
        }

        String attr;
        switch (args.length) {
            case 0:
            case 1:
                showMenuDetails(plugin, sender, menu, view);
                break;
            case 2:
                attr = args[1];
                MiscUtil.statusMessage(sender, String.format("&e%s.%s&- = &e%s&-", menu.getName(), attr, menu.getAttributes().get(attr).toString()));
                break;
            case 3:
                attr = args[1];
                String newVal = args[2];
                menu.ensureAllowedToModify(sender);
                if (attr.equals(SMSMenu.OWNER) && !PermissionUtils.isAllowedTo(sender, "scrollingmenusign.edit.any")) {
                    throw new SMSException("You may not change the owner of a menu.");
                }
                menu.getAttributes().set(attr, newVal);
                MiscUtil.statusMessage(sender, String.format("&e%s.%s&- = &e%s&-", menu.getName(), attr, menu.getAttributes().get(attr).toString()));
                break;
        }
        return true;
    }

    private void showMenuDetails(Plugin plugin, CommandSender sender, SMSMenu menu, SMSView view) {
        MessagePager pager = MessagePager.getPager(sender).clear().setParseColours(true);
        pager.add("&fMenu: &6" + menu.getName());
        pager.add("&fOwner ID: &6" + menu.getOwnerId());
        if (view != null) {
            pager.add(String.format("(View: &6%s&f)", view.getName()));
        }
        for (String k : menu.getAttributes().listAttributeKeys(true)) {
            pager.add(String.format(MessagePager.BULLET + "&e%s&f = &e%s", k, menu.getAttributes().get(k).toString()));
        }
        if (!menu.formatUses(sender).isEmpty()) {
            pager.add("Uses: &e" + menu.formatUses(sender));
        }

        String defIconName = plugin.getConfig().getString("sms.inv_view.default_icon", "STONE");
        ItemStack defIcon = SMSUtil.parseMaterialSpec(defIconName);

        List<SMSMenuItem> items = menu.getItems();
        String s = items.size() == 1 ? "" : "s";
        int n = 1;
        pager.add("&f" + items.size() + " menu item" + s + ":");
        for (SMSMenuItem item : items) {
            String message = item.getMessage();
            String command = item.getCommand().replace(" && ", " &&&& ");
            String altCommand = item.getAltCommand().replace(" && ", " &&&& ");
            String uses = item.formatUses(sender);
            String perm = item.getPermissionNode();
            pager.add(String.format("&e%2d) &f%s &7[%s]", n++, item.getLabel(), command));
            if (!altCommand.isEmpty()) {
                pager.add("    &9Alt Command: &e" + altCommand);
            }
            if (!message.isEmpty()) {
                pager.add("    &9Feedback: &e" + message);
            }
            if (!uses.isEmpty()) {
                pager.add("    &9Uses: &e" + uses);
            }
            if (!perm.isEmpty()) {
                pager.add("    &9Permission node: &e" + perm);
            }
            if (item.hasIcon()) {
                ItemStack icon = item.getIcon();
                if (!icon.equals(defIcon)) {
                    String amt = icon.getAmount() > 1 ? icon.getAmount() + " x " : "";
                    pager.add("    &9Icon: &e" + amt + ItemNames.lookup(icon) + " &7(" + icon.getData().toString() + ")");
                }
            }
            String[] lore = item.getLore();
            for (int i = 0; i < lore.length; i++) {
                pager.add((i == 0 ? "    &9Lore: &e" : "          &e") + lore[i]);
            }
        }

        pager.showPage();
    }

    @Override
    public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
        SMSMenu menu;
        switch (args.length) {
            case 1:
                return getMenuCompletions(plugin, sender, args[0]);
            case 2:
                menu = getMenu(sender, args[0]);
                return filterPrefix(sender, menu.getAttributes().listAttributeKeys(false), args[1]);
            case 3:
                menu = getMenu(sender, args[0]);
                Object o = menu.getAttributes().get(args[1]);
                String desc = menu.getAttributes().getDescription(args[1]);
                if (!desc.isEmpty())
                    desc = ChatColor.GRAY.toString() + ChatColor.ITALIC + " [" + desc + "]";
                return getConfigValueCompletions(sender, args[1], o, desc, args[2]);
            default:
                return noCompletions(sender);
        }
    }

}
