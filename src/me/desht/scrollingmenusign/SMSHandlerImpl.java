package me.desht.scrollingmenusign;

import java.util.List;


import me.desht.scrollingmenusign.enums.MenuRemovalAction;
import me.desht.util.MiscUtil;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class SMSHandlerImpl implements SMSHandler {
	private ScrollingMenuSign plugin;
	
	SMSHandlerImpl(ScrollingMenuSign plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public SMSMenu createMenu(String name, String title, String owner) {
		SMSMenu menu;
		try {
			menu = new SMSMenu(plugin, name, MiscUtil.parseColourSpec(title), owner, null);
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
			menu = new SMSMenu(plugin, otherMenu, name, owner, null);
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
	public void deleteMenu(String name, MenuRemovalAction action) throws SMSException {
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
}
