package me.desht.scrollingmenusign;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import me.desht.scrollingmenusign.CommandParser.ReturnStatus;
import me.desht.util.MiscUtil;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.util.config.Configuration;
import org.yaml.snakeyaml.reader.ReaderException;

public class SMSMacro {
	private static Configuration cmdSet;

	static void loadCommands() {
		File f = SMSConfig.getCommandFile();
		if (cmdSet == null)
			cmdSet = new Configuration(f);

		try {
			cmdSet.load();
			MiscUtil.log(Level.INFO, "read " + cmdSet.getKeys().size() + " macros from file.");
		} catch (ReaderException e) {
			MiscUtil.log(Level.SEVERE, "caught exception loading " + f + ": " + e.getMessage());
			backupCommandsFile(f);
		}		
	}

	static void saveCommands() {
		File f = SMSConfig.getCommandFile();
		if (cmdSet == null)
			cmdSet = new Configuration(f);

		cmdSet.save();
	}

	static void autosave() {
		if (SMSConfig.getConfiguration().getBoolean("autosave", true))
			saveCommands();
	}

	/**
	 * Add a command to a macro.  If the macro does not exist,
	 * a new empty macro will be automatically created.
	 * 
	 * @param macro	The macro to add the command to
	 * @param cmd	The command to add
	 */
	public static void addCommand(String macro, String cmd) {
		List<String> c = getCommands(macro);
		c.add(cmd);
		cmdSet.setProperty(macro, c);
		autosave();
	}

	/**
	 * Add a command to a macro, at a given position.  If the macro does not exist,
	 * a new empty macro will be automatically created.
	 * 
	 * @param macro The macro to add the command to
	 * @param cmd	The command to add
	 * @param index	The index at which to add the command (0 is start of the macro)
	 */
	public static void insertCommand(String macro, String cmd, int index) {
		List<String> c = getCommands(macro);
		c.add(index, cmd);
		cmdSet.setProperty(macro, c);
		autosave();
	}

	/**
	 * Get a list of all known macros.
	 * 
	 * @return	A list of strings, each of which is a macro name
	 */
	public static List<String> getMacros() {
		return cmdSet.getKeys();
	}

	/**
	 * Check to see if the given macro exists.
	 * 
	 * @param macro	The macro to check for
	 * @return	True if the macro exists, false otherwise
	 */
	public static Boolean hasMacro(String macro) {
		return cmdSet.getStringList(macro, null) != null;
	}

	/**
	 * Get a list of the commands in the given macro.  If the macro does not exist,
	 * a new empty macro will be automatically created.
	 * 
	 * @param macro	The macro to check
	 * @return	A list of strings, each of which is a command in the macro
	 */
	public static List<String> getCommands(String macro) {
		List<String> c = cmdSet.getStringList(macro, null);
		if (c == null) {
			c = new ArrayList<String>();
			cmdSet.setProperty(macro, c);
			autosave();
		}
		return c;
	}

	/**
	 * Remove a macro.
	 * 
	 * @param macro	The macro to remove
	 */
	public static void removeMacro(String macro) {
		cmdSet.removeProperty(macro);
		autosave();
	}

	/**
	 * Remove a command from a macro.
	 * 
	 * @param macro	The macro to modify
	 * @param index	The index of the command to remove (0 is the first command)
	 */
	public static void removeCommand(String macro, int index) {
		List<String> l = getCommands(macro);
		l.remove(index);
		cmdSet.setProperty(macro, l);
		autosave();
	}

	static void executeCommand(String command, Player player) throws SMSException {
		executeCommand(command, player, new HashSet<String>());
	}

	private static ReturnStatus executeCommandSet(String macro, Player player, Set<String> history) throws SMSException {
		List<String>c = cmdSet.getStringList(macro, null);
		ReturnStatus rs = ReturnStatus.CMD_OK;
		for (String cmd : c) {
			rs = executeCommand(cmd, player, history);
			if (rs == ReturnStatus.MACRO_STOPPED)
				break;
		}
		return rs;
	}

	private static ReturnStatus executeCommand(String command, Player player, Set<String> history) throws SMSException {
		if (command == null || command.isEmpty())
			return ReturnStatus.CMD_IGNORED;

		if (command.startsWith("cs:")) {
			// explicit call to CommandSigns
			SMSCommandSigns.runCommandString(player, command.substring(3));
			return ReturnStatus.CMD_OK;
		} else if (command.startsWith("%")) {
			// a macro expansion
			String macro = command.substring(1);
			if (history.contains(macro)) {
				MiscUtil.log(Level.WARNING, "executeCommandSet [" + macro + "]: recursion detected");
				MiscUtil.errorMessage(player, "Recursive loop detected in macro " + macro + "!");
				return ReturnStatus.MACRO_STOPPED;
			} else if (hasMacro(macro)) {
				history.add(macro);
				return executeCommandSet(macro, player, history);
			} else {
				MiscUtil.errorMessage(player, "No such macro '" + macro + "'.");
				return ReturnStatus.CMD_IGNORED;
			}
		} else if (SMSCommandSigns.isActive() &&
				SMSConfig.getConfiguration().getBoolean("sms.always_use_commandsigns", true)) {
			// implicit call to CommandSigns
			SMSCommandSigns.runCommandString(player, command);
			return ReturnStatus.CMD_OK;
		} else {
			// call to built-in command parser
			return CommandParser.runCommandString(player, command);
		}
	}

	static void sendFeedback(Player player, String message) {
		sendFeedback(player, message, new HashSet<String>());
	}

	private static void sendFeedback(Player player, String message, Set<String> history) {
		if (message == null || message.length() == 0)
			return;
		if (message.length() > 1 && message.startsWith("%")) {
			// macro expansion
			String macro = message.substring(1);
			if (history.contains(macro)) {
				MiscUtil.log(Level.WARNING, "sendFeedback [" + macro + "]: recursion detected");
				MiscUtil.errorMessage(player, "Recursive loop detected in macro " + macro + "!");
				return;
			} else if (hasMacro(macro)) {
				history.add(macro);
				sendFeedback(player, getCommands(macro), history);
			} else {
				MiscUtil.errorMessage(player, "No such macro '" + macro + "'.");
			}
		} else {
			player.sendMessage(ChatColor.YELLOW + MiscUtil.parseColourSpec(null, message));
		}	
	}

	private static void sendFeedback(Player player, List<String> messages, Set<String> history) {
		for (String m : messages) {
			sendFeedback(player, m, history);
		}
	}

	private static void backupCommandsFile(File original) {
		try {
			File backup = SMSPersistence.getBackupFileName(original.getParentFile(), original.getName());

			MiscUtil.log(Level.INFO, "An error occurred while loading the commands file, so a backup copy of "
					+ original + " is being created. The backup can be found at " + backup.getPath());
			SMSPersistence.copy(original, backup);
		} catch (IOException e) {
			MiscUtil.log(Level.SEVERE, "Error while trying to write backup file: " + e);
		}
	}
}
