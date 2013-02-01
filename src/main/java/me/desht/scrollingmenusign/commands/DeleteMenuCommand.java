package me.desht.scrollingmenusign.commands;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSHandler;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.views.SMSView;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.commands.AbstractCommand;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class DeleteMenuCommand extends AbstractCommand {
	
	public DeleteMenuCommand() {
		super("sms del", 0, 1);
		setPermissionNode("scrollingmenusign.commands.delete");
		setUsage("/sms delete <menu>");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) throws SMSException {
		SMSHandler handler = ((ScrollingMenuSign)plugin).getHandler();
		
		SMSMenu menu = null;
		if (args.length > 0) {
			menu = handler.getMenu(args[0]);
		} else {
			notFromConsole(sender);
			Player player = (Player)sender;
			SMSView view = SMSView.getTargetedView(player, true);
			menu = view.getActiveMenu(player.getName());
		}
		handler.deleteMenu(menu.getName());
		MiscUtil.statusMessage(sender, "Deleted menu &e" + menu.getName());

		return true;
	}

}
