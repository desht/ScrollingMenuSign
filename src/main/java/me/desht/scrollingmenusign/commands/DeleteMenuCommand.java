package me.desht.scrollingmenusign.commands;

import java.util.List;

import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSHandler;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.views.SMSView;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class DeleteMenuCommand extends SMSAbstractCommand {

	public DeleteMenuCommand() {
		super("sms delete", 0, 1);
		setPermissionNode("scrollingmenusign.commands.delete");
		setUsage("/sms delete <menu>");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) throws SMSException {
		SMSHandler handler = ((ScrollingMenuSign)plugin).getHandler();
		SMSMenu menu = null;

		if (args.length > 0) {
			menu = getMenu(sender, args[0]);
		} else {
			notFromConsole(sender);
			Player player = (Player)sender;
			SMSView view = SMSView.getTargetedView(player, true);
			menu = view.getActiveMenu(player.getName());
		}
		menu.ensureAllowedToModify(sender);
		handler.deleteMenu(menu.getName());
		MiscUtil.statusMessage(sender, "Deleted menu &e" + menu.getName() + "&-.  Use &f/sms undelete " + menu.getName() + "&- to restore it.");

		return true;
	}

	@Override
	public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
		if (args.length == 1) {
			return getMenuCompletions(plugin, sender, args[0]);
		} else {
			showUsage(sender);
			return noCompletions(sender);
		}
	}
}
