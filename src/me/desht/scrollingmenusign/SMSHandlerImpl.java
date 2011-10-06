package me.desht.scrollingmenusign;

import java.util.List;

import me.desht.scrollingmenusign.enums.ReturnStatus;
import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.scrollingmenusign.parser.CommandParser;
import me.desht.util.MiscUtil;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class SMSHandlerImpl implements SMSHandler {
	
	SMSHandlerImpl() {
	}
	
	@Override
	public SMSMenu createMenu(String name, String title, String owner) {
		SMSMenu menu;
		try {
			menu = new SMSMenu(name, MiscUtil.parseColourSpec(title), owner);
		} catch (SMSException e) {
			// should not get here
			return null;
		}
		SMSMenu.addMenu(name, menu, false);
		return menu;
	}

	@Override
	public SMSMenu createMenu(String name, SMSMenu otherMenu, String owner) {
		SMSMenu menu;
		try {
			menu = new SMSMenu(otherMenu, name, owner);
		} catch (SMSException e) {
			// should not get here
			return null;
		}
		SMSMenu.addMenu(name, menu, false);
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
	public void deleteMenu(String name, SMSMenuAction action) throws SMSException {
		SMSMenu.getMenu(name).deletePermanent(action);
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
	public String getTargetedMenuSign(Player player, Boolean complain) throws SMSException {
		return SMSMenu.getTargetedMenuSign(player, complain);
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
	public ReturnStatus runCommandString(Player player, String command) throws SMSException {
		return new CommandParser().runCommandString(player, command);
	}
}
