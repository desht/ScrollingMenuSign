package me.desht.scrollingmenusign;

import java.util.List;

import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.scrollingmenusign.parser.CommandParser;
import me.desht.scrollingmenusign.parser.ParsedCommand;
import me.desht.scrollingmenusign.views.ViewManager;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class SMSHandlerImpl implements SMSHandler {

	SMSHandlerImpl() {
	}

	@Override
	@Deprecated
	public SMSMenu createMenu(String name, String title, String owner) {
		SMSMenu menu;
		try {
			menu = new SMSMenu(name, MiscUtil.parseColourSpec(title), owner);
		} catch (SMSException e) {
			// should not get here
			return null;
		}
		SMSMenu.registerMenu(name, menu, false);
		return menu;
	}

	@Override
	public SMSMenu createMenu(String name, String title, Player owner) {
		SMSMenu menu;
		try {
			menu = new SMSMenu(name, MiscUtil.parseColourSpec(title), owner);
		} catch (SMSException e) {
			e.printStackTrace();
			// should not get here
			return null;
		}
		SMSMenu.registerMenu(name, menu, false);
		return menu;
	}

	@Override
	public SMSMenu createMenu(String name, String title, Plugin owner) {
		SMSMenu menu;
		try {
			menu = new SMSMenu(name, MiscUtil.parseColourSpec(title), owner);
		} catch (SMSException e) {
			// should not get here
			return null;
		}
		SMSMenu.registerMenu(name, menu, false);
		return menu;
	}

	@Override
	public SMSMenu getMenu(String name) throws SMSException {
		return SMSMenu.getMenu(name);
	}

	@Override
	public boolean checkMenu(String name) {
		return SMSMenu.checkForMenu(name);
	}

	@Override
	public void deleteMenu(String name) throws SMSException {
		SMSMenu.getMenu(name).deletePermanent();
	}

	@Override
	public SMSMenu getMenuAt(Location loc) throws SMSException {
		return SMSMenu.getMenuAt(loc);
	}

	@Override
	public String getMenuNameAt(Location loc) {
		return SMSMenu.getMenuNameAt(loc);
	}

	@Override
	public List<SMSMenu> listMenus() {
		return SMSMenu.listMenus();
	}

	@Override
	public List<SMSMenu> listMenus(boolean isSorted) {
		return SMSMenu.listMenus(isSorted);
	}

	@Override
	public ParsedCommand executeCommand(CommandSender sender, String command) throws SMSException {
		return new CommandParser().executeCommand(sender, command);
	}

	@Override
	public ViewManager getViewManager() {
		return ScrollingMenuSign.getInstance().getViewManager();
	}
}
