package me.desht.scrollingmenusign;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import me.desht.scrollingmenusign.ScrollingMenuSign.MenuRemoveAction;

public interface SMSHandler {
	public SMSMenu createMenu(String name, String title, String owner);
	public SMSMenu createMenu(String name, SMSMenu otherMenu, String owner);
	
	public void deleteMenu(String name) throws SMSNoSuchMenuException;
	public void deleteMenu(String menuName, MenuRemoveAction action) throws SMSNoSuchMenuException;
	
	public SMSMenu getMenu(String name) throws SMSNoSuchMenuException;
	public boolean checkMenu(String name);
	
	public String getMenuNameAt(Location loc);
	public SMSMenu getMenuAt(Location loc) throws SMSNoSuchMenuException;
	
	public String getTargetedMenuSign(Player player, Boolean complain) throws SMSException;
	
	public List<SMSMenu> listMenus();
	public List<SMSMenu> listMenus(boolean isSorted);
}
