package me.desht.scrollingmenusign.commandlets;

import me.desht.scrollingmenusign.SMSValidate;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.views.CommandTrigger;
import me.desht.scrollingmenusign.views.PoppableView;
import me.desht.scrollingmenusign.views.SMSView;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PopupCommandlet extends BaseCommandlet {

    public PopupCommandlet() {
        super("POPUP");
    }

    @Override
    public boolean execute(ScrollingMenuSign plugin, CommandSender sender, CommandTrigger trigger, String cmd, String[] args) {
        SMSValidate.isTrue(args.length >= 2, "Usage: " + cmd + " <view-name>");
        SMSValidate.isTrue(sender instanceof Player, "Not from the console!");
        SMSView targetView = plugin.getViewManager().getView(args[1]);
        SMSValidate.isTrue(targetView instanceof PoppableView, "View " + args[1] + " is not a poppable view");

        ((PoppableView) targetView).toggleGUI((Player) sender);
        return true;
    }

}
