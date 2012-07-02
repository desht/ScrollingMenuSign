package me.desht.scrollingmenusign.parser;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMacro;
import me.desht.scrollingmenusign.SMSVariables;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.enums.ReturnStatus;
import me.desht.scrollingmenusign.expector.ExpectCommandSubstitution;
import me.desht.scrollingmenusign.spout.SpoutUtils;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PermissionUtils;
import me.desht.dhutils.LogUtils;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.google.common.base.Joiner;

public class CommandParser {
	private static Logger cmdLogger = null;

	private Set<String> macroHistory;

	public CommandParser() {
		if (cmdLogger == null) {
			cmdLogger = Logger.getLogger(CommandParser.class.getName());
			setLogFile(ScrollingMenuSign.getInstance().getConfig().getString("sms.command_log_file"));
		}
		this.macroHistory = new HashSet<String>();
	}

	public static void setLogFile(String logFileName) {
		for (Handler h : cmdLogger.getHandlers()) {
			h.close();
			cmdLogger.removeHandler(h);
		}
		if (logFileName != null && !logFileName.isEmpty()) {
			try {
				File logFile = new File(ScrollingMenuSign.getInstance().getDataFolder(), logFileName);
				FileHandler fh = new FileHandler(logFile.getPath());
				CommandLogFormatter formatter = new CommandLogFormatter();
				fh.setFormatter(formatter);
				cmdLogger.addHandler(fh);
				cmdLogger.setUseParentHandlers(false);
			} catch (SecurityException e) {
				LogUtils.warning("Can't log to " + logFileName + ": " + e.getMessage());
				e.printStackTrace();
			} catch (IOException e) {
				LogUtils.warning("Can't log to " + logFileName + ": " + e.getMessage());
				e.printStackTrace();
			}
		} else {
			// no explicit log file - just use the parent handler, which should log to the Bukkit console
			cmdLogger.setUseParentHandlers(true);
		}
	}

	private enum RunMode { CHECK_PERMS, EXECUTE };

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
			case RESTRICTED:
			case UNKNOWN:
				break;
			case SUBSTITUTION_NEEDED:
				if (!ScrollingMenuSign.getInstance().isSpoutEnabled() || !SpoutUtils.showTextEntryPopup(player, pCmd.getLastError())) {
					MiscUtil.alertMessage(player, pCmd.getLastError() + " &6(Left or right-click anywhere to cancel)");
				}
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
		return cmd == null || cmd.getStatus() == ReturnStatus.CMD_OK;
	}
	
	private static final Pattern promptPat = Pattern.compile("<\\$:(.+?)>");
	private static final Pattern varSubPat = Pattern.compile("<\\$.+?>");
	
	ParsedCommand handleCommandString(Player player, String command, RunMode mode) throws SMSException {
		if (player != null) {
			// see if an interactive substitution is needed
			Matcher m = promptPat.matcher(command);
			if (m.find() && m.groupCount() > 0 && mode == RunMode.EXECUTE) {
				ScrollingMenuSign.getInstance().responseHandler.expect(player.getName(), new ExpectCommandSubstitution(command));

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
			SMSVariables vars = SMSVariables.getVariables(player.getName());
			if (!vars.getVariables().isEmpty()) {
				for (String var : vars.getVariables()) {
					command = command.replace("<$" + var + ">", vars.get(var));	
				}
			}
			m = varSubPat.matcher(command);
			Set<String> missing = new HashSet<String>();
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
			if (cmd != null && cmd.isCommandStopped()) {
				// Not the first command in the sequence, and the outcome of the previous command
				// means we need to stop here, i.e. the last command ended with "&&" but didn't run, or
				// ended with "$$" but ran OK.
				break;
			}

			cmd = new ParsedCommand(player, scanner);

			switch (mode) {
			case EXECUTE:
				execute(player, cmd);
				logCommandUsage(player, cmd);
				break;
			case CHECK_PERMS:
				cmd.setStatus(ReturnStatus.CMD_OK);
				if ((cmd.isElevated() || cmd.isConsole()) && !PermissionUtils.isAllowedTo(player, "scrollingmenusign.create.elevated")) {
					cmd.setStatus(ReturnStatus.NO_PERMS);
					return cmd;
				} else if (!cmd.getCosts().isEmpty() && !PermissionUtils.isAllowedTo(player, "scrollingmenusign.create.cost")) {
					cmd.setStatus(ReturnStatus.NO_PERMS);
					return cmd;
				}
				break;
			default:
				throw new IllegalArgumentException("unexpected run mode for parseCommandString()");
			}
		}

		return cmd;
	}

	private void execute(Player player, ParsedCommand cmd) throws SMSException {
		if (cmd.isRestricted()) {
			// restriction checks can stop a command from running, but it's not
			// an error condition
			cmd.setLastError("Restriction checks prevented command from running");
			cmd.setStatus(ReturnStatus.RESTRICTED);
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
		String playerName = player == null ? "CONSOLE" : player.getName();
		
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
			if (!PermissionUtils.isAllowedTo(player, "scrollingmenusign.execute.elevated")) {
				cmd.setStatus(ReturnStatus.NO_PERMS);
				cmd.setLastError("You don't have permission to run this command.");
				return;
			}
			LogUtils.fine("Execute (console): " + command);
			executeLowLevelCommand(Bukkit.getServer().getConsoleSender(), cmd, command);
		} else if (cmd.isElevated()) {
			// this is a /@ command, to be run as the real player, but with temporary permissions
			// (this now also handles the /* fake-player style, which is no longer directly supported)
			if (!PermissionUtils.isAllowedTo(player, "scrollingmenusign.execute.elevated") || ScrollingMenuSign.permission == null) {
				cmd.setStatus(ReturnStatus.NO_PERMS);
				cmd.setLastError(ScrollingMenuSign.permission == null ? 
						"Permission elevation is not supported." : 
						"You don't have permission to run this command.");
				return;
			}

			boolean tempOp = false;
			List<String> nodes = ScrollingMenuSign.getInstance().getConfig().getStringList("sms.elevation.nodes");
			try {
				for (String node : nodes) {
					if (!node.isEmpty()) {
						ScrollingMenuSign.permission.playerAddTransient(player, node);
						LogUtils.fine("Added temporary permission node '" + node + "' to " + playerName);
					}
				}
				if (ScrollingMenuSign.getInstance().getConfig().getBoolean("sms.elevation.grant_op", false) && !player.isOp()) {
					tempOp = true;
					player.setOp(true);
					LogUtils.fine("Granted temporary op to " + playerName);
				}
				LogUtils.fine("Execute (elevated): " + command);
				executeLowLevelCommand(player, cmd, command);
			} finally {
				// revoke all temporary permissions granted to the user
				for (String node : nodes) {
					if (!node.isEmpty()) {
						ScrollingMenuSign.permission.playerRemoveTransient(player, node);
						LogUtils.fine("Removed temporary permission node '" + node + "' from " + player.getName());
					}
				}
				if (tempOp) {
					player.setOp(false);
					LogUtils.fine("Removed temporary op from " + playerName);
				}
			}
		} else {
			// just an ordinary command (possibly chat), no special privilege elevation
			LogUtils.fine("Execute (normal): " + command);
			executeLowLevelCommand(player, cmd, command);
		}
	}

	private void logCommandUsage(Player player, ParsedCommand cmd)	 {
		logCommandUsage(player, cmd, null);
	}

	private void logCommandUsage(Player player, ParsedCommand cmd, String message) {
		if (ScrollingMenuSign.getInstance().getConfig().getBoolean("sms.log_commands")) {
			String playerName = player == null ? "CONSOLE" : player.getName();
			String outcome = message == null ? cmd.getLastError() : message;
			cmdLogger.log(Level.INFO, playerName + " ran [" + cmd.getRawCommand() + "], outcome = " + cmd.getStatus() + " (" + outcome + ")");
		}
	}

	private void runMacro(Player player, ParsedCommand cmd) throws SMSException {
		String macroName = cmd.getCommand();
		if (macroHistory.contains(macroName)) {
			LogUtils.warning("Recursion detected and stopped in macro " + macroName);
			cmd.setStatus(ReturnStatus.WOULD_RECURSE);
			cmd.setLastError("Recursion detected and stopped in macro " + macroName);
			return;
		} else if (SMSMacro.hasMacro(macroName)) {
			macroHistory.add(macroName);
			ParsedCommand cmd2 = null;
			for (String c : SMSMacro.getCommands(macroName)) {
				for (int i = 0; i < cmd.getQuotedArgs().length; i++) {
					c = c.replace("<" + i + ">", cmd.getQuotedArgs()[i]);
				}
				c = c.replace("<*>", Joiner.on(" ").join(cmd.getArgs()));
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
		if (command.startsWith("/") && !cmd.isChat()) {
			if (!Bukkit.getServer().dispatchCommand(sender, command.substring(1))) {
				
				//				cmd.setStatus(ReturnStatus.CMD_FAILED);
				//				cmd.setLastError("Execution of command '" + cmd.getCommand() + "' failed (unknown command?)");
				
				// It's possible the command is OK, but some plugins insist on implementing commands by hooking
				// chat events, and dispatchCommand() does not work for those.  So we'll try running the command
				// via player.chat().  Sadly, player.chat() doesn't tell us if the command was found or not.
				cmd.setStatus(ReturnStatus.UNKNOWN);
				((Player)sender).chat(command);
			}
		} else if (sender instanceof Player) {
			((Player)sender).chat(MiscUtil.parseColourSpec(command));
		} else {
			LogUtils.info("Chat: " + command);
		}
	}
}
