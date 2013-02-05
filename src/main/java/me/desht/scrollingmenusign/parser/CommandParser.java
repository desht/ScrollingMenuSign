package me.desht.scrollingmenusign.parser;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
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

import me.desht.dhutils.Cost;
import me.desht.dhutils.ExperienceManager;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PermissionUtils;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMacro;
import me.desht.scrollingmenusign.SMSVariables;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.commandlets.CommandletManager;
import me.desht.scrollingmenusign.enums.ReturnStatus;
import me.desht.scrollingmenusign.expector.ExpectCommandSubstitution;
import me.desht.scrollingmenusign.spout.SpoutUtils;
import me.desht.scrollingmenusign.views.SMSView;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.google.common.base.Joiner;

public class CommandParser {

	private static final Pattern promptPat = Pattern.compile("<\\$:(.+?)>");
	private static final Pattern passwordPat = Pattern.compile("<\\$p:(.+?)>");
	private static final Pattern preDefPat = Pattern.compile("<([A-Z]+)>");
	private static final Pattern userVarSubPat = Pattern.compile("<\\$([A-Za-z0-9_\\.]+)(=.*?)?>");
	private static final Pattern viewVarSubPat = Pattern.compile("<\\$v:([A-Za-z0-9_\\.]+)=(.*?)>");
	
	private enum RunMode { CHECK_PERMS, EXECUTE };

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
	
	/**
	 * Parse and run a command string via the SMS command engine
	 * 
	 * @param sender		Player who is running the command
	 * @param command		The command to be run
	 * @return	The parsed command object, which gives access to details on how the command ran
	 * @throws SMSException
	 */
	public ParsedCommand executeCommand(CommandSender sender, String command, SMSView view) {
		ParsedCommand cmd = handleCommandString(sender, view, command, RunMode.EXECUTE);
		return cmd;
	}

	public ParsedCommand executeCommand(CommandSender sender, String command) {
		ParsedCommand cmd = handleCommandString(sender, null, command, RunMode.EXECUTE);
		return cmd;
	}

	/**
	 * Check that the given player has permission to create a menu entry with the given command.
	 * 
	 * @param player	Player who is creating the menu item
	 * @param command	The command to be run
	 * @return	true if the player is allowed to create this item, false otherwise
	 * @throws SMSException
	 */
	public boolean verifyCreationPerms(Player player, String command) throws SMSException {
		ParsedCommand cmd = handleCommandString(player, null, command, RunMode.CHECK_PERMS);
		return cmd == null || cmd.getStatus() == ReturnStatus.CMD_OK;
	}


	/**
	 * Substitute any user-defined variables (/sms var) in the command
	 * 
	 * @param player	The player running the command
	 * @param command	The command string
	 * @param missing	(returned) a set of variable names with no definitions
	 * @return	The substituted command string
	 */
	private String userVarSubs(Player player, String command, Set<String> missing) {
		Matcher m = userVarSubPat.matcher(command);
		StringBuffer sb = new StringBuffer(command.length());
		while (m.find()) {
			String repl = SMSVariables.get(player, m.group(1));
			if (repl == null)
				repl = m.group(2).substring(1);
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
	 * @param view
	 * @param command
	 * @return
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
	 * Carry out all the predefined substitutions
	 * 
	 * @param player
	 * @param view
	 * @param command
	 * @return
	 */
	private String preDefinedSubs(Player player, SMSView view, String command) {
		Matcher m = preDefPat.matcher(command);
		StringBuffer sb = new StringBuffer(command.length());
		while (m.find()) {
			String key = m.group(1);
			String repl;
			if (key.equals("X")) {
				repl = Integer.toString(player.getLocation().getBlockX());
			} else if (key.equals("Y")) {
				repl = Integer.toString(player.getLocation().getBlockY());
			} else if (key.equals("Z")) {
				repl = Integer.toString(player.getLocation().getBlockZ());
			} else if (key.equals("NAME") || key.equals("N")) {
				repl = player.getName();
			} else if (key.equals("WORLD")) {
				repl = player.getWorld().getName();
			} else if (key.equals("I")) {
				repl = player.getItemInHand() == null ? "0" : Integer.toString(player.getItemInHand().getTypeId());
			} else if (key.equals("INAME")) {
				repl = player.getItemInHand() == null ? "nothing" : player.getItemInHand().getType().toString();
			} else if (key.equals("MONEY") && ScrollingMenuSign.economy != null) {
				repl = formatStakeStr(ScrollingMenuSign.economy.getBalance(player.getName()));
			} else if (key.equals("VIEW")) {
				repl = view == null ? "" : view.getName();
			} else if (key.equals("EXP")) {
				repl = Integer.toString(new ExperienceManager(player).getCurrentExp());
			} else {
				String menuName = view == null ? "???" : view.getActiveMenu(player.getName()).getName();
				LogUtils.warning("unknown replacement <" + key + "> in command [" + command + "], menu " + menuName);
				repl = "<" + key + ">";
			}
			m.appendReplacement(sb, repl);
		}
		m.appendTail(sb);
		return sb.toString();
	}

	/**
	 * Handle one command string, which may contain multiple commands (chained with && or $$)
	 * 
	 * @param sender
	 * @param view
	 * @param command
	 * @param mode
	 * @return
	 * @throws SMSException
	 */
	private ParsedCommand handleCommandString(CommandSender sender, SMSView view, String command, RunMode mode) throws SMSException {
		if (sender instanceof Player) {
			Player player = (Player) sender;

			// see if an interactive substitution is needed
			if (mode == RunMode.EXECUTE) {
				Matcher m = promptPat.matcher(command);
				if (m.find() && m.groupCount() > 0) {
					ScrollingMenuSign.getInstance().responseHandler.expect(player.getName(), new ExpectCommandSubstitution(command, view));
					return new ParsedCommand(ReturnStatus.SUBSTITUTION_NEEDED, m.group(1));
				} else {
					m = passwordPat.matcher(command);
					if (m.find() && m.groupCount() > 0 && ScrollingMenuSign.getInstance().isSpoutEnabled()) {
						SpoutUtils.setupPasswordPrompt(player, command, view);
						return new ParsedCommand(ReturnStatus.SUBSTITUTION_NEEDED, m.group(1));
					}
				}
			}

			// make any predefined substitutions
			command = preDefinedSubs(player, view, command);

			// make any user-defined substitutions
			Set<String> missing = new HashSet<String>();
			command = userVarSubs(player, command, missing);
			if (!missing.isEmpty() && mode == RunMode.EXECUTE) {
				return new ParsedCommand(ReturnStatus.BAD_VARIABLE, "Command has uninitialised variables: " + missing.toString());
			}
		}

		// make any view-specific substitutions
		command = viewVarSubs(view, command);

		Scanner scanner = new Scanner(command);

		ParsedCommand cmd = null;
		while (scanner.hasNext()) {
			if (cmd != null && cmd.isCommandStopped()) {
				// Not the first command in the sequence, and the outcome of the previous command
				// means we need to stop here, i.e. the last command ended with "&&" but didn't run, or
				// ended with "$$" but ran OK.
				break;
			}

			cmd = new ParsedCommand(sender, scanner);

			switch (mode) {
			case EXECUTE:
				execute(sender, view, cmd);
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

		return cmd;
	}

	private void logCommandUsage(CommandSender sender, ParsedCommand cmd)	 {
		logCommandUsage(sender, cmd, null);
	}

	private void logCommandUsage(CommandSender sender, ParsedCommand cmd, String message) {
		if (ScrollingMenuSign.getInstance().getConfig().getBoolean("sms.log_commands")) {
			String outcome = message == null ? cmd.getLastError() : message;
			cmdLogger.log(Level.INFO, sender.getName() + " ran [" + cmd.getRawCommand() + "], outcome = " + cmd.getStatus() + " (" + outcome + ")");
		}
	}

	private void execute(CommandSender sender, SMSView view, ParsedCommand cmd) throws SMSException {
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

		if (sender instanceof Player) {
			Cost.chargePlayer(sender, cmd.getCosts());
		}

		if (cmd.getCommand() == null || cmd.getCommand().isEmpty()) {
			// this allows for "commands" which only apply a cost and don't have an actual command
			cmd.setStatus(ReturnStatus.CMD_OK);
			return;
		}

		String command = cmd.getCommand() + " " + Joiner.on(' ').join(cmd.getArgs());

		if (cmd.isMacro()) {
			// run a macro
			runMacro(sender, view, cmd);
		} else if (cmd.isCommandlet()) {
			runCommandlet(sender, view, cmd);
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
			LogUtils.fine("Execute (console): " + command);
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
				executeElevated((Player)sender, cmd, command);
			} else {
				executeLowLevelCommand(sender, cmd, command);
			}
		} else {
			// just an ordinary command (possibly chat), no special privilege elevation
			LogUtils.fine("Execute (normal): " + command);
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
					LogUtils.fine("Added temporary permission node '" + node + "' to " + player.getName());
				}
			}
			if (ScrollingMenuSign.getInstance().getConfig().getBoolean("sms.elevation.grant_op", false) && !player.isOp()) {
				tempOp = true;
				player.setOp(true);
				LogUtils.fine("Granted temporary op to " + player.getName());
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
				LogUtils.fine("Removed temporary op from " + player.getName());
			}
		}
	}

	private void runMacro(CommandSender sender, SMSView view, ParsedCommand cmd) throws SMSException {
		String macroName = cmd.getCommand();
		if (macroHistory.contains(macroName)) {
			LogUtils.warning("Recursion detected and stopped in macro " + macroName);
			cmd.setStatus(ReturnStatus.WOULD_RECURSE);
			cmd.setLastError("Recursion detected and stopped in macro " + macroName);
			return;
		} else if (SMSMacro.hasMacro(macroName)) {
			macroHistory.add(macroName);
			ParsedCommand subCommand = null;
			String allArgs = Joiner.on(" ").join(cmd.getArgs());
			for (String c : SMSMacro.getCommands(macroName)) {
				for (int i = 0; i < cmd.getQuotedArgs().length; i++) {
					c = c.replace("<" + i + ">", cmd.getQuotedArgs()[i]);
				}
				c = c.replace("<*>", allArgs);
				subCommand = handleCommandString(sender, view, c, RunMode.EXECUTE);
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
			return;
		} else {
			cmd.setStatus(ReturnStatus.BAD_MACRO);
			cmd.setLastError("Unknown macro " + macroName + ".");
			return;
		}
	}

	private void runCommandlet(CommandSender sender, SMSView view, ParsedCommand cmd) {
		CommandletManager cmdlets = ScrollingMenuSign.getInstance().getCommandletManager();
		
		cmdlets.getCommandlet(cmd.getCommand()).execute(cmdlets.getPlugin(), sender, view, cmd.getCommand(), cmd.getQuotedArgs());
	}

	private void executeLowLevelCommand(CommandSender sender, ParsedCommand cmd, String command) {
		cmd.setStatus(ReturnStatus.CMD_OK);
		if (command.startsWith("/") && !cmd.isChat()) {
			if (!Bukkit.getServer().dispatchCommand(sender, command.substring(1))) {
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

	private static String formatStakeStr(double stake) {
		try {
			return ScrollingMenuSign.economy.format(stake);
		} catch (Exception e) {
			LogUtils.warning("Caught exception from " + ScrollingMenuSign.economy.getName() + " while trying to format quantity " + stake + ":");
			e.printStackTrace();
			LogUtils.warning("ScrollingMenuSign will continue but you should verify your economy plugin configuration.");
		}
		return new DecimalFormat("#0.00").format(stake) + " ";
	}
}
