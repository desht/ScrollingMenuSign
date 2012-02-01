package me.desht.scrollingmenusign.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.enums.ReturnStatus;
import me.desht.scrollingmenusign.util.MiscUtil;

import org.bukkit.entity.Player;

public class ParsedCommand {
	private String command;
	private List<String> args;
	private boolean elevated;
	private boolean restricted;
	private boolean affordable;
	private List<Cost> costs;
	private ReturnStatus status;
	private boolean whisper;
	private boolean chat;
	private boolean macro;
	private boolean commandStopped, macroStopped;
	private boolean console;
	private String lastError;
	private StringBuilder rawCommand;

	ParsedCommand (Player player, Scanner scanner) throws SMSException {
		args = new ArrayList<String>();
		costs = new ArrayList<Cost>();
		elevated = restricted = chat = whisper = macro = console = false;
		commandStopped = macroStopped = false;
		affordable = true;
		command = null;
		status = ReturnStatus.UNKNOWN;
		lastError = "no error";
		rawCommand = new StringBuilder();

		while (scanner.hasNext()) {
			String token = scanner.next();
			rawCommand.append(token).append(" ");

			if (token.startsWith("%")) {
				// macro
				command = token.substring(1);
				macro = true;
			} else if ((token.startsWith("/@") || token.startsWith("/*")) && command == null) {
				// elevated command
				command = "/" + token.substring(2);
				elevated = true;
			} else if (token.startsWith("/#") && command == null) {
				// console command
				command = "/" + token.substring(2);
				console = true;
			} else if (token.startsWith("/") && command == null) {
				// regular command
				command = token;
			} else if (token.startsWith("\\\\") && command == null) {
				// a whisper string
				command = token.substring(2);
				whisper = true;
			} else if (token.startsWith("\\") && command == null) {
				// a chat string
				command = token.substring(1);
				chat = true;
			} else if (token.startsWith("@!") && command == null) {
				// verify NOT player or group name
				if (restrictionCheck(player, token.substring(2))) {
					restricted = true;
				}
			} else if (token.startsWith("@") && command == null) {
				// verify player or group name
				if (!restrictionCheck(player, token.substring(1))) {
					restricted = true;
				}
			} else if (token.equals("$$$") && !restricted && affordable) {
				// command terminator, and stop any macro too
				macroStopped = commandStopped = !restricted && affordable;
				break;
			} else if (token.equals("$$")) {
				// command terminator - run command and finish
				commandStopped = !restricted && affordable;
				break;
			} else if (token.startsWith("$") && command == null) {
				// apply a cost or costs
				for (String c : token.substring(1).split(";")) {
					if (!c.isEmpty()) {
						try {
							costs.add(new Cost(c));
						} catch (IllegalArgumentException e) {
							throw new SMSException(e.getMessage());
						}
					}
				}

				if (!Cost.playerCanAfford(player, getCosts())) {
					affordable = false;
				}	
			} else if (token.equals("&&")) {
				// command separator - start another command
				break;
			} else {
				// just a plain string
				if (command == null)
					command = token;
				else
					args.add(token);
			}
		}
		
		if (player == null && command != null && command.startsWith("/")) {
			console = true;
		}
	}

	public ParsedCommand(ReturnStatus rs, String message) {
		status = rs;
		lastError = message;
	}
	
	/**
	 * Get the name of the command, i.e. the first word of the command string with any special
	 * leading characters removed.
	 * 
	 * @return	The command name
	 */
	public String getCommand() {
		return command;
	}

	/**
	 * Get the argument list for the command, split on whitespace.
	 * 
	 * @return	The command's arguments
	 */
	public List<String> getArgs() {
		return args;
	}

	/**
	 * Get the elevation status, i.e. whether the command should be (has been) run with permissions checks
	 * bypassed.
	 * 
	 * @return	true if elevated, false otherwise
	 */
	public boolean isElevated() {
		return elevated;
	}

	/**
	 * Get the restriction status, i.e. whether the command will be (has been) ignored due to a restriction
	 * check not being met.
	 * 
	 * @return true if the command was not run due to a restriction check, false otherwise
	 */
	public boolean isRestricted() {
		return restricted;
	}

	/**
	 * Get the affordable status, i.e. whether the command costs can be (have been) met by the player.
	 * 
	 * @return	true if the command is affordable, false otherwise
	 */
	public boolean isAffordable() {
		return affordable;
	}

	/**
	 * Get the details of the costs for this command.
	 * 
	 * @return	a List of Cost objects
	 */
	public List<Cost> getCosts() {
		return costs;
	}

	/**
	 * Get the return status from actually running the command.
	 * 
	 * @return	the return status
	 */
	public ReturnStatus getStatus() {
		return status;
	}

	void setStatus(ReturnStatus status) {
		this.status = status;
	}

	/**
	 * Check if this command was to whisper a message to the player, i.e. it started with '\\'
	 * 
	 * @return true if the command was a whisper, false otherwise
	 */
	public boolean isWhisper() {
		return whisper;
	}

	public boolean isChat() {
		return chat;
	}

	/**
	 * Check if this command calls a macro.
	 * 
	 * @return	true if a macro is used, false otherwise
	 */
	public boolean isMacro() {
		return macro;
	}
	
	/**
	 * Check if the command sequence was stopped, i.e. $$ or $$$ was encountered following a command that actually ran (and
	 * was not ignored due to a restriction or cost check)
	 *  
	 * @return	true if the command was stopped, false otherwise
	 */
	public boolean isCommandStopped() {
		return commandStopped;
	}

	/**
	 * Check if a macro was stopped, i.e. $$$ was encountered following a command that actually ran (and
	 * was not ignored due to a restriction or cost check)
	 * 
	 * @return	true if a macro was stopped, false otherwise
	 */
	public boolean isMacroStopped() {
		return macroStopped;
	}

	/**
	 * Check if the command was run as a console command, i.e. started with '#'
	 * 
	 * @return true if a console command, false otherwise
	 */
	public boolean isConsole() {
		return console;
	}

	/**
	 * Get the last error message that was generated from running the command.
	 * 
	 * @return	The error text
	 */
	public String getLastError() {
		return lastError;
	}

	void setLastError(String lastError) {
		this.lastError = lastError;
	}

	public String getRawCommand() {
		return rawCommand.toString().trim();
	}

	/**
	 * Get the argument at the given index.
	 * 
	 * @param index		Index of the argument to get
	 * @return			The argument
	 */
	public String arg(int index) {
		return args.get(index);
	}
	
	private boolean restrictionCheck(Player player, String check) {
		if (player == null) {
			// no restrictions apply to being run from the console
			return true;
		}
		
		if (check.startsWith("g:")) {
			if (ScrollingMenuSign.permission != null) {
				return ScrollingMenuSign.permission.playerInGroup(player, check.substring(2));
			} else {
				return false;
			}
		} else if (check.startsWith("p:")) {
			return player.getName().equalsIgnoreCase(check.substring(2));
		} else if (check.startsWith("w:")) {
			return player.getWorld().getName().equalsIgnoreCase(check.substring(2));
		} else if (check.startsWith("n:")) {
			return ScrollingMenuSign.permission.has(player, check.substring(2));
		} else if (check.startsWith("i:")) {
			try {
				return player.getItemInHand().getTypeId() == Integer.parseInt(check.substring(2));
			} catch (NumberFormatException e) {
				MiscUtil.log(Level.WARNING, "bad number format in restriction check: " + check);
				return false;
			}
		} else {
			return player.getName().equalsIgnoreCase(check);
		}
	}
}
