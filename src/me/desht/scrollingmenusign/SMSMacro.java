package me.desht.scrollingmenusign;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.yaml.snakeyaml.Yaml;

public class SMSMacro {
	private static final String commandFile = "commands.yml";
//	private ScrollingMenuSign plugin = null;
	private static Map<String,List<String>> cmdSet;
	
//	SMSMacro(ScrollingMenuSign plugin) {
//		this.plugin = plugin;
//	}

	@SuppressWarnings("unchecked")
	static void loadCommands() {
		File f = new File(SMSConfig.getPluginFolder(), commandFile);
		if (!f.exists()) { // create empty file if doesn't already exist
            try {
                f.createNewFile();
            } catch (IOException e) {
                SMSUtils.log(Level.SEVERE, e.getMessage());
            }
        }
		Yaml yaml = new Yaml();
		
		try {
			cmdSet = (Map<String,List<String>>) yaml.load(new FileInputStream(f));
		} catch (FileNotFoundException e) {
			SMSUtils.log(Level.SEVERE, "commands file '" + f + "' was not found.");
		} catch (Exception e) {
			SMSUtils.log(Level.SEVERE, "caught exception loading " + f + ": " + e.getMessage());
			backupCommandsFile(f);
		}
		if (cmdSet == null) cmdSet = new HashMap<String,List<String>>();
		SMSUtils.log(Level.INFO, "read " + cmdSet.size() + " macros from file.");
	}

	static void saveCommands() {
		Yaml yaml = new Yaml();
		File f = new File(SMSConfig.getPluginFolder(), commandFile);
		if (cmdSet != null)	SMSUtils.log(Level.INFO, "Saving " + cmdSet.size() + " macros to file...");
		try {
			yaml.dump(cmdSet, new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8")));
		} catch (IOException e) {
			SMSUtils.log(Level.SEVERE, e.getMessage());
		}
		
	}

	static void addCommand(String commandSet, String cmd) {
		List<String> c = getCommands(commandSet);
		c.add(cmd);
	}
	
	static void insertCommand(String commandSet, String cmd, int index) {
		List<String> c = getCommands(commandSet);
		c.add(index, cmd);
	}
	
	static Set<String> getCommands() {
		return cmdSet.keySet();
	}
	
	static Boolean hasCommand(String cmd) {
		return cmdSet.containsKey(cmd);
	}
	
	static List<String> getCommands(String commandSet) {
		List<String> c = cmdSet.get(commandSet);
		if (c == null) {
			c = new ArrayList<String>();
			cmdSet.put(commandSet, c);
		}
		return c;
	}
	
	static void removeCommand(String commandSet) {
		cmdSet.remove(commandSet);
	}
	
	static void removeCommand(String commandSet, int index) {
		cmdSet.get(commandSet).remove(index);
	}
	
	
	static void executeCommand(String command, Player player) {
		executeCommand(command, player, new HashSet<String>());
	}
	
	private static void executeCommandSet(String commandSet, Player player, Set<String> history) {
		List<String>c = cmdSet.get(commandSet);
		for (String cmd : c) {
			executeCommand(cmd, player, history);
		}
	}

	private static void executeCommand(String command, Player player, Set<String> history) {
		if (command.length() == 0) return;
		
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
				} else if (cmdSet.containsKey(cmd)) {
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
			} else if (hasCommand(macro)) {
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
        	File backup = SMSPersistence.getBackupFileName(original.getParentFile(), commandFile);

            SMSUtils.log(Level.INFO, "An error occurred while loading the commands file, so a backup copy of "
                + original + " is being created. The backup can be found at " + backup.getPath());
            SMSPersistence.copy(original, backup);
        } catch (IOException e) {
            SMSUtils.log(Level.SEVERE, "Error while trying to write backup file: " + e);
        }
	}
}
