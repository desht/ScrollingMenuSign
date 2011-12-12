package me.desht.scrollingmenusign.parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.desht.scrollingmenusign.SMSConfig;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMacro;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.enums.ReturnStatus;
import me.desht.scrollingmenusign.expector.ExpectCommandSubstitution;
import me.desht.scrollingmenusign.util.Debugger;
import me.desht.scrollingmenusign.util.MiscUtil;
import me.desht.scrollingmenusign.util.PermissionsUtils;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CommandParser {
	private Set<String> macroHistory;

	public CommandParser() {
		this.macroHistory = new HashSet<String>();
	}

	private enum RunMode { CHECK_PERMS, EXECUTE };

	/**
	 * Parse and run a command string via the SMS command engine
	 * 
	 * @param player	Player who is running the command
	 * @param command	Command to be run
	 * @return			A return status indicating the outcome of the command
	 * @throws SMSException
	 * @deprecated use runCommand()	
	 */
	@Deprecated
	public ReturnStatus runCommandString(Player player, String command) throws SMSException { 
		ParsedCommand cmd = runCommand(player, command);
		return cmd.getStatus();
	}
	
	/**
	 * High-level wrapper to run a command.  Return status is reported to the calling player.
	 * 
	 * @param player	Player who is running the command
	 * @param command	The command to be run
	 * @throws SMSException
	 */
	public static void runCommandWrapper(Player player, String command) throws SMSException {
		ParsedCommand pCmd = new CommandParser().runCommand(player, command);
		// pCmd could be null if this was an empty command
		if (pCmd != null) {
			switch(pCmd.getStatus()) {
			case CMD_OK:
				break;
			case SUBSTITUTION_NEEDED:
				MiscUtil.alertMessage(player, pCmd.getLastError());
				break;
			default:
				MiscUtil.errorMessage(player, pCmd.getLastError());
				break;
			}
		}
	}

	/**
	 * Parse and run a command string via the SMS command engine
	 * 
	 * @param player		Player who is running the command
	 * @param command		The command to be run
	 * @return	The parsed command object, which gives access to details on how the command ran
	 * @throws SMSException
	 */
	public ParsedCommand runCommand(Player player, String command) throws SMSException {
		ParsedCommand cmd = handleCommandString(player, command, RunMode.EXECUTE);

		return cmd;
	}

	public boolean verifyCreationPerms(Player player, String command) throws SMSException {
		ParsedCommand cmd = handleCommandString(player, command, RunMode.CHECK_PERMS);
		return cmd.getStatus() == ReturnStatus.CMD_OK;
	}

	ParsedCommand handleCommandString(Player player, String command, RunMode mode) throws SMSException {
		if (player != null) {
			// see if an interactive subsitution is needed
			Pattern p = Pattern.compile("<\\$:(.+?)>");
			Matcher m = p.matcher(command);
			if (m.find() && m.groupCount() > 0) {
				ScrollingMenuSign.getInstance().expecter.expectingResponse(player, new ExpectCommandSubstitution(command));
				return new ParsedCommand(ReturnStatus.SUBSTITUTION_NEEDED, m.group(1));
			}
			
			// pre-defined substitutions ...
			ItemStack stack =  player.getItemInHand();
			command = command.replace("<X>", "" + player.getLocation().getBlockX());
			command = command.replace("<Y>", "" + player.getLocation().getBlockY());
			command = command.replace("<Z>", "" + player.getLocation().getBlockZ());
			command = command.replace("<NAME>", player.getName());
			command = command.replace("<N>", player.getName());
			command = command.replace("<WORLD>", player.getWorld().getName());
			command = command.replace("<I>", stack != null ? "" + stack.getTypeId() : "0");
			command = command.replace("<INAME>", stack != null ? stack.getType().toString() : "???");

			// user-defined substitutions...
			ConfigurationSection cs = SMSConfig.getConfig().getConfigurationSection("uservar." + player.getName());
			if (cs != null) {
				for (String key : cs.getKeys(false)) {
					command = command.replace("<$" + key + ">", cs.getString(key));	
				}
			}
			p = Pattern.compile("<\\$.+?>");
			m = p.matcher(command);
			List<String> missing = new ArrayList<String>();
			while (m.find()) {
				missing.add(m.group());
			}
			if (!missing.isEmpty() && mode == RunMode.EXECUTE) {
				return new ParsedCommand(ReturnStatus.BAD_VARIABLE, "Command has uninitialised variables: " + missing.toString());
			}
		}

		Scanner scanner = new Scanner(command);

		ParsedCommand cmd = null;
		while (scanner.hasNext()) {
			if (cmd != null && cmd.getStatus() != ReturnStatus.CMD_OK) {
				// not the first command in the sequence - must have been a && separator
				// if the previous command was not successful, we quit here
				break;
			}

			cmd = new ParsedCommand(player, scanner);

			switch (mode) {
			case EXECUTE:
				execute(player, cmd);
				break;
			case CHECK_PERMS:
				cmd.setStatus(ReturnStatus.CMD_OK);
				if ((cmd.isElevated() || cmd.isConsole()) && !PermissionsUtils.isAllowedTo(player, "scrollingmenusign.create.elevated")) {
					cmd.setStatus(ReturnStatus.NO_PERMS);
					return cmd;
				} else if (!cmd.getCosts().isEmpty() && !PermissionsUtils.isAllowedTo(player, "scrollingmenusign.create.cost")) {
					cmd.setStatus(ReturnStatus.NO_PERMS);
					return cmd;
				}
				break;
			default:
				throw new IllegalArgumentException("unexpected run mode for parseCommandString()");
			}

			if (cmd.getStatus() == ReturnStatus.CMD_OK && cmd.isCommandStopped()) {
				// if the last command was stopped (by $$ or $$$), we quit here, but only the command was successful
				break;
			}
		}

		return cmd;
	}

	private void execute(Player player, ParsedCommand cmd) throws SMSException {
		if (cmd.isRestricted()) {
			// restriction checks can stop a command from running, but it's not
			// an error condition
			cmd.setStatus(ReturnStatus.CMD_OK);
			return;
		}
		if (!cmd.isAffordable()) {
			// failure to meet costs is an error condition that we report to the player
			cmd.setLastError("You can't afford to run this command.");
			cmd.setStatus(ReturnStatus.CANT_AFFORD);
			return;
		}

		Cost.chargePlayer(player, cmd.getCosts());

		if (cmd.getCommand() == null || cmd.getCommand().isEmpty()) {
			// this allows for "commands" which only apply a cost and don't have an actual command
			cmd.setStatus(ReturnStatus.CMD_OK);
			return;
		}
		
		StringBuilder sb = new StringBuilder(cmd.getCommand()).append(" ");
		for (String arg : cmd.getArgs()) {
			sb.append(arg).append(" ");
		}
		String command = sb.toString().trim();
		
		if (cmd.isMacro()) {
			// run a macro
			runMacro(player, cmd);
		} else if (cmd.isWhisper()) {
			// private message to the player
			MiscUtil.alertMessage(player, command);
			cmd.setStatus(ReturnStatus.CMD_OK);
		} else if (cmd.isConsole()) {
			// run this as a console command
			// only works for commands that may be run via the console, but should always work
			if (!PermissionsUtils.isAllowedTo(player, "scrollingmenusign.execute.elevated")) {
				cmd.setStatus(ReturnStatus.NO_PERMS);
				cmd.setLastError("You don't have permission to run this command.");
				return;
			}
			Debugger.getDebugger().debug("execute (console): " + command);
			executeLowLevelCommand(Bukkit.getServer().getConsoleSender(), cmd, command);
		} else if (cmd.isElevated()) {
			// this is a /@ command, to be run as the real player, but with temporary permissions
			// (this now also handles the /* fake-player style, which is no longer directly supported)

			if (!PermissionsUtils.isAllowedTo(player, "scrollingmenusign.execute.elevated") || ScrollingMenuSign.permission == null) {
				cmd.setStatus(ReturnStatus.NO_PERMS);
				cmd.setLastError("You don't have permission to run this command.");
				return;
			}

			Debugger.getDebugger().debug("execute (elevated): " + command);

			boolean tempOp = false;
			@SuppressWarnings("unchecked")
			List<String> nodes = (List<String>) SMSConfig.getConfig().getList("sms.elevation.nodes");
			try {
				for (String node : nodes) {
					if (!node.isEmpty()) {
						ScrollingMenuSign.permission.playerAddTransient(player, node);
					}
				}
				if (SMSConfig.getConfig().getBoolean("sms.elevation.grant_op", false) && !player.isOp()) {
					tempOp = true;
					player.setOp(true);
				}
				executeLowLevelCommand(player, cmd, command);
			} finally {
				// revoke all temporary permissions granted to the user
				for (String node : nodes) {
					if (!node.isEmpty()) {
						ScrollingMenuSign.permission.playerRemoveTransient(player, node);
					}
				}
				if (tempOp) {
					player.setOp(false);
				}
			}
		} else {
			// just an ordinary command, no special privilege elevation
			Debugger.getDebugger().debug("execute (normal): " + command);
			executeLowLevelCommand(player, cmd, command);
		}
	}

	private void runMacro(Player player, ParsedCommand cmd) throws SMSException {
		String macroName = cmd.getCommand();
		if (macroHistory.contains(macroName)) {
			MiscUtil.log(Level.WARNING, "Recursion detected and stopped in macro " + macroName);
			cmd.setStatus(ReturnStatus.WOULD_RECURSE);
			cmd.setLastError("Recursion detected and stopped in macro " + macroName);
			return;
		} else if (SMSMacro.hasMacro(macroName)) {
			macroHistory.add(macroName);
			ParsedCommand cmd2 = null;
			for (String c : SMSMacro.getCommands(macroName)) {
				for (int i = 0; i < cmd.getArgs().size(); i++) {
					c = c.replace("<" + (i + 1) + ">", cmd.arg(i));
				}
				cmd2 = handleCommandString(player, c, RunMode.EXECUTE);
				if (cmd2.isMacroStopped())
					break;
			}
			// return status of a macro is the return status of the last command that was run
			if (cmd2 == null) {
				cmd.setStatus(ReturnStatus.BAD_MACRO);
				cmd.setLastError("Empty macro?");					
			} else {
				cmd.setStatus(cmd2.getStatus());
				cmd.setLastError(cmd2.getLastError());
				if (!cmd2.isAffordable()) {
					cmd.setStatus(ReturnStatus.CANT_AFFORD);
				}
			}
			return;
		} else {
			cmd.setStatus(ReturnStatus.BAD_MACRO);
			cmd.setLastError("Unknown macro " + macroName + ".");
			return;
		}
	}

	private void executeLowLevelCommand(CommandSender sender, ParsedCommand cmd, String command) {
		cmd.setStatus(ReturnStatus.CMD_OK);
		if (command.startsWith("/")) {
			if (!Bukkit.getServer().dispatchCommand(sender, command.substring(1))) {
				cmd.setStatus(ReturnStatus.CMD_FAILED);
				cmd.setLastError("Execution of command '" + cmd.getCommand() + "' failed (unknown command?)");
			}
		} else if (sender instanceof Player) {
			((Player)sender).chat(MiscUtil.parseColourSpec(command));
		} else {
			MiscUtil.log(Level.INFO, "Chat: " + command);
		}
	}
}
