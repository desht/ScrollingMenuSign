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

import org.bukkit.entity.Player;
import org.yaml.snakeyaml.Yaml;

public class SMSMacro {
	private static final String commandFile = "commands.yml";
	private ScrollingMenuSign plugin = null;
	Map<String,List<String>> cmdSet;
	
	SMSMacro(ScrollingMenuSign plugin) {
		this.plugin = plugin;
	}

	@SuppressWarnings("unchecked")
	void loadCommands() {
		File f = new File(plugin.getDataFolder(), commandFile);
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

	void saveCommands() {
		Yaml yaml = new Yaml();
		File f = new File(plugin.getDataFolder(), commandFile);
		if (cmdSet != null)	SMSUtils.log(Level.INFO, "Saving " + cmdSet.size() + " macros to file...");
		try {
			yaml.dump(cmdSet, new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8")));
		} catch (IOException e) {
			SMSUtils.log(Level.SEVERE, e.getMessage());
		}
		
	}

	void addCommand(String commandSet, String cmd) {
		List<String> c = getCommands(commandSet);
		c.add(cmd);
	}
	
	void insertCommand(String commandSet, String cmd, int index) {
		List<String> c = getCommands(commandSet);
		c.add(index, cmd);
	}
	
	Set<String> getCommands() {
		return cmdSet.keySet();
	}
	
	Boolean hasCommand(String cmd) {
		return cmdSet.containsKey(cmd);
	}
	
	List<String> getCommands(String commandSet) {
		List<String> c = cmdSet.get(commandSet);
		if (c == null) {
			c = new ArrayList<String>();
			cmdSet.put(commandSet, c);
		}
		return c;
	}
	
	void removeCommand(String commandSet) {
		cmdSet.remove(commandSet);
	}
	
	void removeCommand(String commandSet, int index) {
		cmdSet.get(commandSet).remove(index);
	}
	
	
	void executeCommand(String command, Player player) {
		executeCommand(command, player, new HashSet<String>());
	}
	
	private void executeCommandSet(String commandSet, Player player, Set<String> history) {
		List<String>c = cmdSet.get(commandSet);
		for (String cmd : c) {
			executeCommand(cmd, player, history);
		}
	}

	private void executeCommand(String command, Player player, Set<String> history) {
		if (command.length() == 0) return;
		
		Pattern pattern = Pattern.compile("^(%|cs:)(.+)");
		Matcher matcher = pattern.matcher(command);
		if (matcher.find()) {
			String cmd = matcher.group(2);
			if (matcher.group(1).equalsIgnoreCase("cs:") && plugin.csHandler != null) {
				plugin.csHandler.runCommandString(cmd, player);
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
		} else if (plugin.csHandler != null &&
				plugin.getConfiguration().getBoolean("sms.always_use_commandsigns", true)) {
			plugin.csHandler.runCommandString(command, player);
		} else {
			player.chat(command);
		}
	}

	private void backupCommandsFile(File original) {
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
