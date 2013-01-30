package me.desht.scrollingmenusign.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSVariables;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.commandlets.CommandletManager;
import me.desht.scrollingmenusign.enums.ReturnStatus;
import me.desht.dhutils.Cost;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.PermissionUtils;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ParsedCommand {
	private String command;
	private List<String> args;
	private boolean elevated;
	private boolean restricted;
	private boolean affordable;
	private boolean applicable;
	private List<Cost> costs;
	private ReturnStatus status;
	private boolean whisper;
	private boolean chat;
	private boolean macro;
	private boolean commandStopped, macroStopped;
	private boolean console;
	private String lastError;
	private StringBuilder rawCommand;
	private String[] quotedArgs;
	private boolean commandlet;

	ParsedCommand (CommandSender sender, Scanner scanner) throws SMSException {
		args = new ArrayList<String>();
		costs = new ArrayList<Cost>();
		elevated = restricted = chat = whisper = macro = console = commandlet = false;
		commandStopped = macroStopped = false;
		affordable = applicable = true;
		command = null;
		status = ReturnStatus.UNKNOWN;
		lastError = "no error";
		rawCommand = new StringBuilder();

		CommandletManager cmdlets = ScrollingMenuSign.getInstance().getCommandletManager();
		
		while (scanner.hasNext()) {
			String token = scanner.next();

			if (token.startsWith("\"") || token.startsWith("'")) {
				// quoted string (single or double) - swallow all following tokens until a matching
				// quotation mark is detected
				String quote = token.substring(0, 1);
				if (token.endsWith(quote)) {
					token = token.substring(1, token.length() - 1);
				} else {
					token = token.substring(1);
					Pattern oldDelimiter = scanner.delimiter();
					scanner.useDelimiter(quote);
					token = token + scanner.next();
					scanner.useDelimiter(oldDelimiter);
					scanner.next(); // swallow the closing quote
				}
				rawCommand.append("\"").append(token).append("\" ");
				if (command == null)
					command = token;
				else
					args.add("\"" + token + "\"");
				continue;
			}
			
			rawCommand.append(token).append(" ");
			
			if (cmdlets.hasCommandlet(token)) {
				// commandlet
				command = token;
				commandlet = true;
			} else if (token.startsWith("%")) {
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
				// verify a restriction NOT matched
				if (restrictionCheck(sender, token.substring(2))) {
					restricted = true;
				}
			} else if (token.startsWith("@") && command == null) {
				// verify a restriction matched
				if (!restrictionCheck(sender, token.substring(1))) {
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
							throw new SMSException(e.getMessage() + ": bad cost");
						}
					}
				}

				if (!Cost.playerCanAfford(sender, getCosts())) {
					affordable = false;
				}
				if (!Cost.isApplicable(sender, getCosts())) {
					applicable = false;
				}
			} else if (token.equals("&&")) {
				// command separator - start another command IF this command is runnable
				commandStopped = restricted || !affordable;
				break;
			} else {
				// just a plain string
				if (command == null)
					command = token;
				else
					args.add(token);
			}
		}

		quotedArgs = MiscUtil.splitQuotedString(rawCommand.toString()).toArray(new String[0]);

		if (!(sender instanceof Player) && command != null && command.startsWith("/")) {
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
	 * Get the argument list for the command (excluding the command), split on whitespace.
	 * 
	 * @return	The command's arguments
	 */
	public List<String> getArgs() {
		return args;
	}

	/**
	 * Get the argument list for the command (including the command), split on quoted substrings
	 * and/or whitespace.
	 * 
	 * @return
	 */
	public String[] getQuotedArgs() {
		return quotedArgs;
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
	 * Get the applicable status, i.e. whether the command costs actually make sense.  E.g. repairing an
	 * item which doesn't have durability would not be applicable.
	 * 
	 * @return
	 */
	public boolean isApplicable() {
		return applicable;
	}

	/**
	 * Check if this command is a special "commandlet" registered with SMS.
	 * 
	 * @return
	 */
	public boolean isCommandlet() {
		return commandlet;
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

	/**
	 * Check if this command is a chat string, i.e. it started with '\'
	 * 
	 * @return true if the command was a chat string, false otherwise
	 */
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

	private boolean restrictionCheck(CommandSender sender, String check) {
		if (!(sender instanceof Player)) {
			// no restrictions apply when being run from the console
			return true;
		}
		Player player = (Player) sender;

		String[] parts = check.split(":", 2);
		if (parts.length == 1) {
			// legacy check: just see if the player name matches
			return player.getName().equalsIgnoreCase(parts[0]);
		}

		String checkType = parts[0];
		String checkTerm = parts[1];

		switch (checkType.charAt(0)) {
		case 'g':
			return ScrollingMenuSign.permission == null ? false : ScrollingMenuSign.permission.playerInGroup(player, checkTerm);
		case 'p':
			return player.getName().equalsIgnoreCase(checkTerm);
		case 'w':
			return player.getWorld().getName().equalsIgnoreCase(checkTerm);
		case 'n':
			return PermissionUtils.isAllowedTo(player, checkTerm);
		case 'i':
			return isHoldingObject(player, checkTerm);
		case 'v':
			return variableTest(player, checkType, checkTerm);
		default:
			LogUtils.warning("Unknown check type: " + check);
			return false;
		}
	}

	private boolean isHoldingObject(Player player, String checkTerm) {
		if (checkTerm.matches("[0-9]+")) {
			return player.getItemInHand().getTypeId() == Integer.parseInt(checkTerm);
		} else {
			Material mat = Material.matchMaterial(checkTerm);
			if (mat == null) {
				LogUtils.warning("Invalid material specification: " + checkTerm);
				return false;
			} else {
				return player.getItemInHand().getType() == mat;
			}
		}
	}

	private static final Pattern exprPattern = Pattern.compile("^([a-zA-Z0-9\\._]+)(=|<|>|<=|>=)?(.+)?");

	private boolean variableTest(Player player, String checkType, String checkTerm) {
		Matcher m = exprPattern.matcher(checkTerm);
		if (m.matches()) {
			if (m.group(1) == null) {
				return false;
			} else if (m.group(2) == null) {
				return SMSVariables.isSet(player, m.group(1));
			} else {
				return doComparison(player, checkType, m.group(1), m.group(2), m.group(3) == null ? "" : m.group(3));
			}
		}

		return false;
	}

	private boolean doComparison(Player player, String checkType, String varSpec, String op, String testValue) {
		String value = SMSVariables.get(player, varSpec);
		if (value == null) return false;

		boolean caseInsensitive = checkType.indexOf('i') > 0;
		boolean useRegex = checkType.indexOf('r') > 0;
		boolean forceNumeric = checkType.indexOf('n') > 0;

		LogUtils.fine("doComparison: player=[" + player.getName() + "] var=[" + varSpec + "] val=[" + value + "] op=[" + op + "] test=[" + testValue + "]");
		LogUtils.fine("doComparison: case-sensitive=" + !caseInsensitive + " regex=" + useRegex + " force-numeric=" + forceNumeric);

		try {
			if (op.equals("=")) {
				if (useRegex) {
					Pattern p = Pattern.compile(testValue, caseInsensitive ? Pattern.CASE_INSENSITIVE : 0);
					return p.matcher(value).matches();
				} else if (forceNumeric) {
					return Double.parseDouble(value) == Double.parseDouble(testValue);
				} else 	if (caseInsensitive) {
					return value.equalsIgnoreCase(testValue);
				} else {
					return value.equals(testValue);
				}
			} else if (op.equals(">")) {
				return Double.parseDouble(value) > Double.parseDouble(testValue);
			} else if (op.equals("<")) {
				return Double.parseDouble(value) < Double.parseDouble(testValue);
			} else if (op.equals(">=")) {
				return Double.parseDouble(value) >= Double.parseDouble(testValue);
			} else if (op.equals("<=")) {
				return Double.parseDouble(value) <= Double.parseDouble(testValue);
			} else {
				LogUtils.warning("unexpected comparison op: " + op);
			}
		} catch (NumberFormatException e) {
			LogUtils.warning(e.getMessage() + ": invalid numeric value");
		} catch (PatternSyntaxException e) {
			LogUtils.warning("invalid regexp syntax: " + testValue + " " + e.getMessage());
		}

		return false;
	}

}
