package me.desht.scrollingmenusign.commandlets;

import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.views.SMSView;

import org.apache.commons.lang.Validate;
import org.bukkit.command.CommandSender;

public class SubmenuCommandlet extends BaseCommandlet {

	@Override
	public void execute(CommandSender sender, SMSView view, String cmd, String[] args) {
		Validate.isTrue(args.length >= 2, "Usage: " + cmd + " <menu-name>");
		SMSMenu menu = SMSMenu.getMenu(args[1]);
		
		view.pushMenu(menu);
	}

}
