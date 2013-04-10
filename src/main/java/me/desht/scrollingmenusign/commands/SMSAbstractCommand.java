package me.desht.scrollingmenusign.commands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.commands.AbstractCommand;
import me.desht.scrollingmenusign.SMSHandler;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.views.SMSView;

public abstract class SMSAbstractCommand extends AbstractCommand {

	public SMSAbstractCommand(String label, int minArgs, int maxArgs) {
		super(label, minArgs, maxArgs);
	}

	public SMSAbstractCommand(String label, int minArgs) {
		super(label, minArgs);
	}

	public SMSAbstractCommand(String label) {
		super(label);
	}

	protected List<String> getMenuCompletions(Plugin plugin, CommandSender sender, String prefix) {
		List<String> res = new ArrayList<String>();
		SMSHandler handler = ((ScrollingMenuSign)plugin).getHandler();
		List<SMSMenu> menus = handler.listMenus(true);
		for (SMSMenu menu : menus) {
			if (prefix.isEmpty() || menu.getName().startsWith(prefix))
				res.add(menu.getName());
		}
		return res.isEmpty() ? noCompletions(sender) : res;
	}

	protected List<String> getViewCompletions(CommandSender sender, String prefix) {
		List<String> res = new ArrayList<String>();
		List<SMSView> views = SMSView.listViews();
		for (SMSView view : views) {
			if (prefix.isEmpty() || view.getName().startsWith(prefix))
				res.add(view.getName());
		}
		return res.isEmpty() ? noCompletions(sender) : MiscUtil.asSortedList(res);
	}

	protected List<String> filterPrefix(CommandSender sender, Collection<String> c, String prefix) {
		List<String> res = new ArrayList<String>();
		for (String s : c) {
			if (prefix == null || prefix.isEmpty() || s.toLowerCase().startsWith(prefix.toLowerCase())) {
				res.add(s);
			}
		}
		return res.isEmpty() ? noCompletions(sender) : MiscUtil.asSortedList(res);
	}
}
