package me.desht.scrollingmenusign.commands;

import java.util.List;

import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.enums.SMSMenuAction;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class MenuTitleCommand extends SMSAbstractCommand {

	public MenuTitleCommand() {
		super("sms title", 2);
		setPermissionNode("scrollingmenusign.commands.title");
		setUsage("/sms title <menu-name> <new-title>");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		SMSMenu menu = SMSMenu.getMenu(args[0]);
		String title = combine(args, 1);
		menu.setTitle(MiscUtil.parseColourSpec(sender, title));
		menu.notifyObservers(SMSMenuAction.REPAINT);

		MiscUtil.statusMessage(sender, "Title for menu &e" + menu.getName() + "&- has been set to &f" + title + "&-.");

		return true;
	}

	@Override
	public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
		if (args.length == 1) {
			return getMenuCompletions(plugin, sender, args[0]);
		} else {
			return noCompletions(sender);
		}
	}
}
