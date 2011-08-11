package me.desht.scrollingmenusign;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
			SMSUtils.log(Level.INFO, "read " + cmdSet.getKeys().size() + " macros from file.");
		} catch (ReaderException e) {
			SMSUtils.log(Level.SEVERE, "caught exception loading " + f + ": " + e.getMessage());
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

	static void executeCommand(String command, Player player) {
		executeCommand(command, player, new HashSet<String>());
	}

	private static void executeCommandSet(String macro, Player player, Set<String> history) {
		List<String>c = cmdSet.getStringList(macro, null);
		for (String cmd : c) {
			executeCommand(cmd, player, history);
		}
	}

	private static void executeCommand(String command, Player player, Set<String> history) {
		if (command == null || command.isEmpty())
			return;

		Pattern pattern = Pattern.compile("^(%|cs:)(.+)");
		Matcher matcher = pattern.matcher(command);
		if (matcher.find()) {
			String cmd = matcher.group(2);
			if (matcher.group(1).equalsIgnoreCase("cs:") && SMSCommandSigns.isActive()) {
				SMSCommandSigns.runCommandString(player, cmd);
			} else if (matcher.group(1).equalsIgnoreCase("%")) {
				// a macro expansion
				if (history.contains(cmd)) {
					SMSUtils.log(Level.WARNING, "executeCommandSet [" + cmd + "]: recursion detected");
					SMSUtils.errorMessage(player, "Recursive loop detected in macro " + cmd + "!");
					return;
				} else if (hasMacro(cmd)) {
					history.add(cmd);
					executeCommandSet(cmd, player, history);
				} else {
					SMSUtils.errorMessage(player, "No such macro '" + cmd + "'.");
				}
			}
		} else if (SMSCommandSigns.isActive() &&
				SMSConfig.getConfiguration().getBoolean("sms.always_use_commandsigns", true)) {
			SMSCommandSigns.runCommandString(player, command);
		} else {
			player.chat(command);
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
				SMSUtils.log(Level.WARNING, "sendFeedback [" + macro + "]: recursion detected");
				SMSUtils.errorMessage(player, "Recursive loop detected in macro " + macro + "!");
				return;
			} else if (hasMacro(macro)) {
				history.add(macro);
				sendFeedback(player, getCommands(macro), history);
			} else {
				SMSUtils.errorMessage(player, "No such macro '" + macro + "'.");
			}
		} else {
			player.sendMessage(ChatColor.YELLOW + SMSUtils.parseColourSpec(null, message));
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

			SMSUtils.log(Level.INFO, "An error occurred while loading the commands file, so a backup copy of "
					+ original + " is being created. The backup can be found at " + backup.getPath());
			SMSPersistence.copy(original, backup);
		} catch (IOException e) {
			SMSUtils.log(Level.SEVERE, "Error while trying to write backup file: " + e);
		}
	}
}
