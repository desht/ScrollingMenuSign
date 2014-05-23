package me.desht.scrollingmenusign.commands;

import me.desht.dhutils.ItemNames;
import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.PopupItem;
import me.desht.scrollingmenusign.SMSValidate;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.views.SMSView;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class MakePopupCommand extends SMSAbstractCommand {
    public MakePopupCommand() {
        super("sms mkpopup", 1, 2);
        setPermissionNode("sms.commands.mkpopup");
        setUsage("/<command> mkpopup <view-name> [-force]");
        setOptions("force");
    }

    @Override
    public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
        notFromConsole(sender);
        Player player = (Player) sender;
        ItemStack stack = player.getItemInHand();
        SMSValidate.isFalse(stack.hasItemMeta() && !hasOption("force"),
                "This item already has custom metadata.  If you really want to override it, run this command again with the -force option");

        SMSView view = ((ScrollingMenuSign) plugin).getViewManager().getView(args[0]);
        PopupItem item = PopupItem.create(stack, view);
        player.setItemInHand(item.toItemStack(stack.getAmount()));

        String title = view.getNativeMenu().getTitle();
        MiscUtil.statusMessage(player, "Your " + ItemNames.lookup(stack) + " is now a popup item for " + title);

        return true;
    }
}
