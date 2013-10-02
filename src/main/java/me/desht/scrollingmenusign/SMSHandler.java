package me.desht.scrollingmenusign;

import java.util.List;

import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.scrollingmenusign.parser.ParsedCommand;
import me.desht.scrollingmenusign.views.ViewManager;
import me.desht.scrollingmenusign.views.SMSView;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;

public interface SMSHandler {
	/**
	 * Creates a new menu
	 * @param name	Unique name of the menu
	 * @param title	Title for the menu
	 * @param owner	Owner of the menu
	 * @return		The menu
	 */
	public SMSMenu createMenu(String name, String title, String owner);

	//	/**
	//	 * Creates a new menu as a copy of an existing menu
	//	 * @param name		Unique name of the menu
	//	 * @param otherMenu	The menu to copy
	//	 * @param owner		Owner of the menu
	//	 * @return			The menu
	//	 */
	//	public SMSMenu createMenu(String name, SMSMenu otherMenu, String owner);

	/**
	 * Deletes a menu, leaving its signs as they are
	 *
	 * @param name		Name of menu to delete
	 * @throws SMSException	if the menu does not exist
	 */
	public void deleteMenu(String name) throws SMSException;

	/**
	 * Retrieves the menu object for the given menu name
	 * @param name	Name of menu to retrieve
	 * @return		The menu
	 * @throws SMSException	if the menu does not exist
	 */
	public SMSMenu getMenu(String name) throws SMSException;
	/**
	 * Check if the given menu exists
	 * @param name	Name of menu to check
	 * @return		true if the menu exists, false if it does not
	 */
	public boolean checkMenu(String name);

	/**
	 * Get the name of the menu which owns the sign at <b>loc</b>
	 * @param loc	The location of the sign
	 * @return		The menu name, or null if there is no menu sign at <b>loc</b>
	 */
	public String getMenuNameAt(Location loc);
	/**
	 * Get the menu object for the menu which owns the sign at <b>loc</b>
	 * @param loc	The location of the sign
	 * @return		The menu
	 * @throws SMSException	if there is no menu sign at <b>loc</b>
	 */
	public SMSMenu getMenuAt(Location loc) throws SMSException;

	/**
	 * Get a list of all known menu objects.
	 *
	 * @return	A list of SMSMenu objects.
	 */
	public List<SMSMenu> listMenus();
	/**
	 * Get a list of all known menu objects, optionally sorting it by menu name.
	 *
	 * @param isSorted 	true to sort the menu objects by name
	 * @return	A list of SMSMenu objects.
	 */
	public List<SMSMenu> listMenus(boolean isSorted);

	/**
	 * Run a command using the ScrollingMenuSign command parser/executor.
	 *
	 * @param sender	The command sender who is running the command
	 * @param command	The command string to be run
	 * @return	A ParsedCommand object giving access to detailed information about the outcome of the command
	 * @throws SMSException if the command string contains syntax errors
	 */
	public ParsedCommand executeCommand(CommandSender sender, String command) throws SMSException;

	/**
	 * Get the view manager object, which allows views to be created and/or deleted.
	 * @return the view manager
	 */
	public ViewManager getViewManager();
}
