package me.desht.scrollingmenusign.commands;

import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.commands.AbstractCommand;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.scrollingmenusign.views.SMSView;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class SortMenuCommand extends AbstractCommand {

	public SortMenuCommand() {
		super("sms so", 0, 2);
		setPermissionNode("scrollingmenusign.commands.sort");
		setUsage("sms sort [<menu-name>] [<auto>]");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		
		SMSMenu menu = null;
		if (args.length > 0) {
			menu = SMSMenu.getMenu(args[0]);
		} else {
			notFromConsole(sender);
			SMSView view = SMSView.getTargetedView((Player)sender, true);
			menu = view.getMenu();
//			menu = SMSMenu.getMenu(SMSMenu.getTargetedMenuSign((Player)sender, true));
		}
		
		if (args.length >=2 && args[1].startsWith("a")) {	// autosort
			menu.setAutosort(true);
			menu.sortItems();
			MiscUtil.statusMessage(sender, "Menu &e" + menu.getName() + "&- has been sorted (autosort enabled)");
		} else {
			menu.setAutosort(false);
			menu.sortItems();
			MiscUtil.statusMessage(sender, "Menu &e" + menu.getName() + "&- has been sorted (autosort disabled)");
		}
		menu.notifyObservers(SMSMenuAction.REPAINT);
		
		return true;
	}
}
