package me.desht.scrollingmenusign;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface SMSHandler {
	public SMSMenu createMenu(String name, String title, String owner);
	public SMSMenu createMenu(String name, SMSMenu otherMenu, String owner);
	
	public void deleteMenu(String name) throws SMSException;
	public void deleteMenu(String menuName, MenuRemovalAction action) throws SMSException;
	
	public SMSMenu getMenu(String name) throws SMSException;
	public boolean checkMenu(String name);
	
	public String getMenuNameAt(Location loc);
	public SMSMenu getMenuAt(Location loc) throws SMSException;
	
	public String getTargetedMenuSign(Player player, Boolean complain) throws SMSException;
	
	public List<SMSMenu> listMenus();
	public List<SMSMenu> listMenus(boolean isSorted);
}
