package me.desht.scrollingmenusign.commandlets;

import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.views.CommandTrigger;

import org.apache.commons.lang.Validate;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CloseSubmenuCommandlet extends BaseCommandlet {

	public CloseSubmenuCommandlet() {
		super("BACK");
	}

	@Override
	public boolean execute(ScrollingMenuSign plugin, CommandSender sender, CommandTrigger trigger, String cmd, String[] args) {
		Validate.isTrue(sender instanceof Player, "Not from the console!");
        trigger.popMenu(sender.getName());
		return true;
	}

}
