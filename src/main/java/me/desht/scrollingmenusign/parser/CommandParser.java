package me.desht.scrollingmenusign.parser;

import com.google.common.base.Joiner;
import me.desht.dhutils.Debugger;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PermissionUtils;
import me.desht.dhutils.cost.Cost;
import me.desht.dhutils.cost.ItemCost;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMacro;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.commandlets.CommandletManager;
import me.desht.scrollingmenusign.enums.ReturnStatus;
import me.desht.scrollingmenusign.expector.ExpectCommandSubstitution;
import me.desht.scrollingmenusign.spout.SpoutUtils;
import me.desht.scrollingmenusign.variables.VariablesManager;
import me.desht.scrollingmenusign.views.CommandTrigger;
import me.desht.scrollingmenusign.views.SMSView;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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

public class CommandParser {

	private static final Pattern promptPat = Pattern.compile("<\\$:(.+?)>");
	private static final Pattern passwordPat = Pattern.compile("<\\$p:(.+?)>");
	private static final Pattern userVarSubPat = Pattern.compile("<\\$([A-Za-z0-9_\\.]+)(=.*?)?>");
	private static final Pattern viewVarSubPat = Pattern.compile("<\\$v:([A-Za-z0-9_\\.]+)=(.*?)>");

	private enum RunMode {CHECK_PERMS, EXECUTE}

	private static Logger cmdLogger = null;

	private final Set<String> macroHistory;

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

	/**
	 * Parse and run a command string via the SMS command engine
	 *
	 * @param sender  Player who is running the command
	 * @param command The command to be run
	 * @param trigger The command trigger
	 * @throws SMSException if there was any problem running the command
	 * @return The parsed command object, which gives access to details on how the command ran
	 */
	public ParsedCommand executeCommand(CommandSender sender, String command, CommandTrigger trigger) {
		return handleCommandString(sender, trigger, command, RunMode.EXECUTE);
	}

	/**
	 * Parse and run a command string via the SMS command engine
	 *
	 * @param sender  Player who is running the command
	 * @param command The command to be run
	 * @return The parsed command object, which gives access to details on how the command ran
	 * @throws SMSException if there was any problem running the command
	 */
	public ParsedCommand executeCommand(CommandSender sender, String command) {
		return handleCommandString(sender, null, command, RunMode.EXECUTE);
	}

	/**
	 * Check that the given player has permission to create a menu entry with the given command.
	 *
	 * @param player  Player who is creating the menu item
	 * @param command The command to be run
	 * @throws SMSException
	 * @return true if the player is allowed to create this item, false otherwise
	 */
	public boolean verifyCreationPerms(Player player, String command) throws SMSException {
		ParsedCommand cmd = handleCommandString(player, null, command, RunMode.CHECK_PERMS);
		return cmd == null || cmd.getStatus() == ReturnStatus.CMD_OK;
	}

	/**
	 * Substitute any user-defined variables (/sms var) in the command
	 *
	 * @param player  The player running the command
	 * @param command The command string
	 * @param missing (returned) a set of variable names with no definitions
	 * @return The substituted command string
	 */
	private String userVarSubs(Player player, String command, Set<String> missing) {
		Matcher m = userVarSubPat.matcher(command);
		StringBuffer sb = new StringBuffer(command.length());
        VariablesManager vm = ScrollingMenuSign.getInstance().getVariablesManager();
		while (m.find()) {
			String repl = vm.get(player, m.group(1));
			if (repl == null && m.groupCount() > 1 && m.group(2) != null) {
				repl = m.group(2).substring(1);
			}
			if (repl == null) {
				missing.add(m.group(1));
			} else {
				m.appendReplacement(sb, Matcher.quoteReplacement(repl));
			}
		}
		m.appendTail(sb);
		return sb.toString();
	}


	/**
	 * Substitute any view-specific variable in the command
	 *
	 * @param view    the view
	 * @param command the command string
	 * @return the substituted command string
	 */
	private String viewVarSubs(SMSView view, String command) {
		Matcher m = viewVarSubPat.matcher(command);
		StringBuffer sb = new StringBuffer(command.length());
		while (m.find()) {
			String repl = view != null && view.checkVariable(m.group(1)) ? view.getVariable(m.group(1)) : m.group(2);
			m.appendReplacement(sb, Matcher.quoteReplacement(repl));
		}
		m.appendTail(sb);
		return sb.toString();
	}

	/**
	 * Handle one command string, which may contain multiple commands (chained with && or $$)
	 *
	 * @param sender  the command sender
	 * @param trigger the command trigger
	 * @param command the command string
	 * @param mode    the run mode
	 * @return a ParsedCommand object
	 * @throws SMSException
	 */
	private ParsedCommand handleCommandString(CommandSender sender, CommandTrigger trigger, String command, RunMode mode) throws SMSException {
		if (sender instanceof Player) {
			Player player = (Player) sender;

			// see if any interactive substitution is needed
			if (mode == RunMode.EXECUTE) {
				Matcher m = promptPat.matcher(command);
				if (m.find() && m.groupCount() > 0) {
					ScrollingMenuSign.getInstance().responseHandler.expect(player, new ExpectCommandSubstitution(command, trigger));
					return new ParsedCommand(ReturnStatus.SUBSTITUTION_NEEDED, m.group(1));
				} else {
					m = passwordPat.matcher(command);
					if (m.find() && m.groupCount() > 0 && ScrollingMenuSign.getInstance().isSpoutEnabled()) {
						SpoutUtils.setupPasswordPrompt(player, command, trigger);
						return new ParsedCommand(ReturnStatus.SUBSTITUTION_NEEDED, m.group(1));
					}
				}
			}

			// make any user-defined substitutions
			Set<String> missing = new HashSet<String>();
			command = userVarSubs(player, command, missing);
			if (!missing.isEmpty() && mode == RunMode.EXECUTE) {
				return new ParsedCommand(ReturnStatus.BAD_VARIABLE, "Command has uninitialised variables: " + missing.toString());
			}
		}

		// make any view-specific substitutions
		if (trigger instanceof SMSView) {
			command = viewVarSubs((SMSView) trigger, command);
		} else {
			command = viewVarSubs(null, command);
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

			cmd = new ParsedCommand(sender, trigger, scanner);

			switch (mode) {
				case EXECUTE:
					execute(sender, trigger, cmd);
					logCommandUsage(sender, cmd);
					break;
				case CHECK_PERMS:
					cmd.setStatus(ReturnStatus.CMD_OK);
					if ((cmd.isElevated() || cmd.isConsole()) && !PermissionUtils.isAllowedTo(sender, "scrollingmenusign.create.elevated")) {
						cmd.setStatus(ReturnStatus.NO_PERMS);
						return cmd;
					} else if (!cmd.getCosts().isEmpty() && !PermissionUtils.isAllowedTo(sender, "scrollingmenusign.create.cost")) {
						cmd.setStatus(ReturnStatus.NO_PERMS);
						return cmd;
					}
					break;
				default:
					throw new IllegalArgumentException("unexpected run mode for parseCommandString()");
			}
		}

		Debugger.getInstance().debug("final command: " + cmd);
		return cmd;
	}

	private void logCommandUsage(CommandSender sender, ParsedCommand cmd) {
		if (ScrollingMenuSign.getInstance().getConfig().getBoolean("sms.log_commands")) {
			cmdLogger.log(Level.INFO, sender.getName() + " ran [" + cmd.getRawCommand() + "], outcome = " + cmd.getStatus() + " (" + cmd.getLastError() + ")");
		}
	}

	private void execute(CommandSender sender, CommandTrigger trigger, ParsedCommand cmd) throws SMSException {
		if (cmd.isRestricted()) {
			// restriction checks can stop a command from running, but it's not an error condition
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
		if (!cmd.isApplicable()) {
			// an inapplicable cost is an error condition that we report to the player
			cmd.setLastError("Doing this would not make sense...");
			cmd.setStatus(ReturnStatus.INAPPLICABLE);
			return;
		}

		// apply any costs associated with this command
		if (sender instanceof Player) {
			for (Cost cost : cmd.getCosts()) {
				cost.apply((Player) sender);
				if (cost instanceof ItemCost && ((ItemCost)cost).isItemsDropped()) {
					MiscUtil.statusMessage(sender, "&6Your inventory is full.  Some items dropped.");
				}
			}
		}

		if (cmd.getCommand() == null || cmd.getCommand().isEmpty()) {
			// this allows for "commands" which only apply a cost and don't have an actual command
			cmd.setStatus(ReturnStatus.CMD_OK);
			return;
		}

		String command = cmd.getCommand() + " " + Joiner.on(' ').join(cmd.getArgs());

		if (cmd.isMacro()) {
			// run a macro
			runMacro(sender, trigger, cmd);
		} else if (cmd.isCommandlet()) {
			runCommandlet(sender, trigger, cmd);
		} else if (cmd.isWhisper()) {
			// private message to the player
			MiscUtil.alertMessage(sender, command);
			cmd.setStatus(ReturnStatus.CMD_OK);
		} else if (cmd.isConsole()) {
			// run this as a console command
			// only works for commands that may be run via the console, but should always work
			if (!PermissionUtils.isAllowedTo(sender, "scrollingmenusign.execute.elevated")) {
				cmd.setStatus(ReturnStatus.NO_PERMS);
				cmd.setLastError("You don't have permission to run this command.");
				return;
			}
			Debugger.getInstance().debug("Execute (console): " + command);
			executeLowLevelCommand(Bukkit.getServer().getConsoleSender(), cmd, command);
		} else if (cmd.isElevated()) {
			// this is a /@ command, to be run as the real player, but with temporary permissions
			// (this now also handles the /* fake-player style, which is no longer directly supported)
			if (!PermissionUtils.isAllowedTo(sender, "scrollingmenusign.execute.elevated") || ScrollingMenuSign.permission == null) {
				cmd.setStatus(ReturnStatus.NO_PERMS);
				cmd.setLastError(ScrollingMenuSign.permission == null ?
						"Permission elevation is not supported." :
						"You don't have permission to run this command.");
				return;
			}
			if (sender instanceof Player) {
				executeElevated((Player) sender, cmd, command);
			} else {
				executeLowLevelCommand(sender, cmd, command);
			}
		} else {
			// just an ordinary command (possibly chat), no special privilege elevation
			Debugger.getInstance().debug("Execute (normal): " + command);
			executeLowLevelCommand(sender, cmd, command);
		}
	}

	private void executeElevated(Player player, ParsedCommand cmd, String command) {
		List<String> nodes = ScrollingMenuSign.getInstance().getConfig().getStringList("sms.elevation.nodes");
		boolean tempOp = false;

		try {
			for (String node : nodes) {
				if (!node.isEmpty()) {
					ScrollingMenuSign.permission.playerAddTransient(player, node);
					Debugger.getInstance().debug("Added temporary permission node '" + node + "' to " + player.getDisplayName());
				}
			}
			if (ScrollingMenuSign.getInstance().getConfig().getBoolean("sms.elevation.grant_op", false) && !player.isOp()) {
				tempOp = true;
				player.setOp(true);
				Debugger.getInstance().debug("Granted temporary op to " + player.getDisplayName());
			}
			Debugger.getInstance().debug("Execute (elevated): " + command);
			executeLowLevelCommand(player, cmd, command);
		} finally {
			// revoke all temporary permissions granted to the user
			for (String node : nodes) {
				if (!node.isEmpty()) {
					ScrollingMenuSign.permission.playerRemoveTransient(player, node);
					Debugger.getInstance().debug("Removed temporary permission node '" + node + "' from " + player.getDisplayName());
				}
			}
			if (tempOp) {
				player.setOp(false);
				Debugger.getInstance().debug("Removed temporary op from " + player.getDisplayName());
			}
		}
	}

	private void runMacro(CommandSender sender, CommandTrigger trigger, ParsedCommand cmd) throws SMSException {
		String macroName = cmd.getCommand();
		if (macroHistory.contains(macroName)) {
			LogUtils.warning("Recursion detected and stopped in macro " + macroName);
			cmd.setStatus(ReturnStatus.WOULD_RECURSE);
			cmd.setLastError("Recursion detected and stopped in macro " + macroName);
		} else if (SMSMacro.hasMacro(macroName)) {
			macroHistory.add(macroName);
			ParsedCommand subCommand = null;
			String allArgs = Joiner.on(" ").join(cmd.getArgs());
			for (String c : SMSMacro.getCommands(macroName)) {
				for (int i = 0; i < cmd.getQuotedArgs().length; i++) {
					c = c.replace("<" + i + ">", cmd.getQuotedArgs()[i]);
				}
				c = c.replace("<*>", allArgs);
				subCommand = handleCommandString(sender, trigger, c, RunMode.EXECUTE);
				if (subCommand.isMacroStopped())
					break;
			}
			// return status of a macro is the return status of the last command that was run
			if (subCommand == null) {
				cmd.setStatus(ReturnStatus.BAD_MACRO);
				cmd.setLastError("Empty macro?");
			} else {
				cmd.setStatus(subCommand.getStatus());
				cmd.setLastError(subCommand.getLastError());
			}
		} else {
			cmd.setStatus(ReturnStatus.BAD_MACRO);
			cmd.setLastError("Unknown macro " + macroName + ".");
		}
	}

	private void runCommandlet(CommandSender sender, CommandTrigger trigger, ParsedCommand cmd) {
		CommandletManager cmdlets = ScrollingMenuSign.getInstance().getCommandletManager();

		boolean res = cmdlets.getCommandlet(cmd.getCommand()).execute(cmdlets.getPlugin(), sender, trigger, cmd.getCommand(), cmd.getQuotedArgs());
		if (!res) {
			// a commandlet returning false indicates the command should be treated as restricted
			cmd.setStatus(ReturnStatus.RESTRICTED);
			cmd.setRestricted(true);
		} else {
			cmd.setStatus(ReturnStatus.CMD_OK);
		}
	}

	private void executeLowLevelCommand(CommandSender sender, ParsedCommand cmd, String command) {
		cmd.setStatus(ReturnStatus.CMD_OK);
		if (command.startsWith("/") && !cmd.isChat()) {
			if (!Bukkit.getServer().dispatchCommand(sender, command.substring(1))) {
				// It's possible the command is OK, but some plugins insist on implementing commands by hooking
				// chat events, and dispatchCommand() does not work for those.  So we'll try running the command
				// via player.chat().  Sadly, player.chat() doesn't tell us if the command was found or not.
				cmd.setStatus(ReturnStatus.UNKNOWN);
				((Player) sender).chat(command);
			}
		} else if (sender instanceof Player) {
			((Player) sender).chat(MiscUtil.parseColourSpec(command));
		} else {
			LogUtils.info("Chat: " + command);
		}
	}
}
