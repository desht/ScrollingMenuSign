package me.desht.scrollingmenusign.commands;

import java.util.ArrayList;
import java.util.List;

import me.desht.dhutils.commands.AbstractCommand;
import me.desht.scrollingmenusign.SMSHandler;
import me.desht.scrollingmenusign.SMSMacro;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSMenuItem;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.views.SMSView;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

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
		if (sender instanceof Player) {
			if (SMSView.getTargetedView((Player) sender) != null) {
				// player has a view targeted - add "." as the first item
				// "." is a convenience for "currently targeted menu"
				res.add(".");
			}
		}		SMSHandler handler = ((ScrollingMenuSign)plugin).getHandler();
		List<SMSMenu> menus = handler.listMenus(true);
		for (SMSMenu menu : menus) {
			if (prefix.isEmpty() || menu.getName().startsWith(prefix))
				res.add(menu.getName());
		}
		return getResult(res, sender, false);
	}

	protected List<String> getViewCompletions(CommandSender sender, String prefix) {
		List<String> res = new ArrayList<String>();

		if (sender instanceof Player) {
			if (SMSView.getTargetedView((Player) sender) != null) {
				// player has a view targeted - add "." as the first item
				// "." is a convenience for "currently targeted view"
				res.add(".");
			}
		}

		List<SMSView> views = SMSView.listViews();
		for (SMSView view : views) {
			if (prefix.isEmpty() || view.getName().startsWith(prefix))
				res.add(view.getName());
		}
		return getResult(res, sender, true);
	}

	protected List<String> getMenuItemCompletions(CommandSender sender, SMSMenu menu, String prefix) {
		List<String> res = new ArrayList<String>();
		for (SMSMenuItem item : menu.getItems()) {
			String label = item.getLabelStripped();
			if (prefix.isEmpty() || label.startsWith(prefix)) {
				// This is a bit of a hack - whitespace in a completion breaks further
				// completion, so we'll use a pretend whitespace character.
				// This does need to be processed later, in any commands which refer to it -
				// handled in SMSMenu
				res.add(label.replace(" ", SMSMenu.FAKE_SPACE));
			}
		}
		return getResult(res, sender, false);
	}

	protected List<String> getMacroCompletions(CommandSender sender, String prefix) {
		List<String> res = new ArrayList<String>();
		for (SMSMacro macro : SMSMacro.listMacros()) {
			String label = macro.getName();
			if (prefix.isEmpty() || label.startsWith(prefix)) {
				res.add(label);
			}
		}
		return getResult(res, sender, true);
	}

}
