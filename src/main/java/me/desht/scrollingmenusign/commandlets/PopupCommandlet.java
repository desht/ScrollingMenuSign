package me.desht.scrollingmenusign.commandlets;

import org.apache.commons.lang.Validate;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.views.PoppableView;
import me.desht.scrollingmenusign.views.SMSView;

public class PopupCommandlet extends BaseCommandlet {
	
	@Override
	public void execute(ScrollingMenuSign plugin, CommandSender sender, SMSView view, String cmd, String[] args) {
		Validate.isTrue(args.length >= 2, "Usage: " + cmd + " <view-name>");
		Validate.isTrue(sender instanceof Player, "Not from the console!");
		SMSView targetView = SMSView.getView(args[1]);
		Validate.isTrue(targetView instanceof PoppableView, "View " + args[1] + " is not a poppable view");

		((PoppableView)targetView).toggleGUI((Player)sender);
	}
	
}
