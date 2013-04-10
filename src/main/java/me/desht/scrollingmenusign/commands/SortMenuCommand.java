package me.desht.scrollingmenusign.commands;

import java.util.List;

import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.scrollingmenusign.views.SMSView;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class SortMenuCommand extends SMSAbstractCommand {

	public SortMenuCommand() {
		super("sms sort", 0, 2);
		setPermissionNode("scrollingmenusign.commands.sort");
		setUsage("/sms sort [<menu-name>] [-auto]");
		setOptions("auto");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		SMSMenu menu = null;
		if (args.length > 0) {
			menu = SMSMenu.getMenu(args[0]);
		} else {
			notFromConsole(sender);
			Player player = (Player)sender;
			SMSView view = SMSView.getTargetedView(player, true);
			menu = view.getActiveMenu(player.getName());
		}

		boolean autoSort = getBooleanOption("auto");
		String s = autoSort ? "enabled" : "disabled";
		menu.setAutosort(autoSort);
		menu.sortItems();
		menu.notifyObservers(SMSMenuAction.REPAINT);
		MiscUtil.statusMessage(sender, "Menu &e" + menu.getName() + "&- has been sorted (autosort " + s + ")");

		return true;
	}

	@Override
	public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
		String prefix = args.length > 0 ? args[0] : "";
		return getMenuCompletions(plugin, sender, prefix);
	}
}
