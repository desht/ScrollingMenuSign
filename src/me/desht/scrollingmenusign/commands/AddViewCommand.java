package me.desht.scrollingmenusign.commands;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.util.MiscUtil;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class AddViewCommand extends AbstractCommand {

	public AddViewCommand() {
		super("sms sy", 1, 1);
		setPermissionNode("scrollingmenusign.commands.sync");
		setUsage("/sms sync <menu-name>");
	}

	@Override
	public boolean execute(ScrollingMenuSign plugin, Player player, String[] args) throws SMSException {
		notFromConsole(player);
	
		Block b = player.getTargetBlock(null, 3);
		SMSMenu menu = plugin.getHandler().getMenu(args[0]);
		menu.addSign(b.getLocation(), true);

		MiscUtil.statusMessage(player, String.format("Sign @ &f%s&- was added to menu &e%s&-.",
		                                             MiscUtil.formatLocation(b.getLocation()), menu.getName()));

		return true;
	}

}
