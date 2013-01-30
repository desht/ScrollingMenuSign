package me.desht.scrollingmenusign.commandlets;

import me.desht.scrollingmenusign.views.SMSView;

import org.bukkit.command.CommandSender;

public class CloseSubmenuCommandlet extends BaseCommandlet {

	@Override
	public void execute(CommandSender sender, SMSView view, String cmd, String[] args) {
		view.popMenu();
	}

}
