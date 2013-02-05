package me.desht.scrollingmenusign.commandlets;

import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.views.SMSView;

import org.apache.commons.lang.Validate;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SubmenuCommandlet extends BaseCommandlet {

	@Override
	public void execute(ScrollingMenuSign plugin, CommandSender sender, SMSView view, String cmd, String[] args) {
		Validate.isTrue(args.length >= 2, "Usage: " + cmd + " <menu-name>");
		Validate.isTrue(sender instanceof Player, "Not from the console!");
		
		SMSMenu menu = SMSMenu.getMenu(args[1]);
		Player player = (Player)sender;
		
		view.pushMenu(player.getName(), menu);
	}

}
