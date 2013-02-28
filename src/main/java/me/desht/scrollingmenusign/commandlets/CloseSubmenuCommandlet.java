package me.desht.scrollingmenusign.commandlets;

import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.views.SMSView;

import org.apache.commons.lang.Validate;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CloseSubmenuCommandlet extends BaseCommandlet {

	public CloseSubmenuCommandlet() {
		super("BACK");
	}
	
	@Override
	public void execute(ScrollingMenuSign plugin, CommandSender sender, SMSView view, String cmd, String[] args) {
		Validate.isTrue(sender instanceof Player, "Not from the console!");
		String playerName = ((Player)sender).getName();
		view.popMenu(playerName);
	}

}
