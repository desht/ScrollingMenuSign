package me.desht.scrollingmenusign.parser;

import me.desht.dhutils.*;
import me.desht.dhutils.cost.Cost;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSValidate;
import me.desht.scrollingmenusign.SMSVariables;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.commandlets.BaseCommandlet;
import me.desht.scrollingmenusign.commandlets.CommandletManager;
import me.desht.scrollingmenusign.commandlets.CooldownCommandlet;
import me.desht.scrollingmenusign.enums.ReturnStatus;
import me.desht.scrollingmenusign.views.CommandTrigger;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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
	private boolean console;
	private String lastError;
	private StringBuilder rawCommand;
	private String[] quotedArgs;
	private boolean commandlet;
	private StopCondition commandStopCondition;
	private StopCondition macroStopCondition;

	public interface SubstitutionHandler {
		public String sub(Player player, CommandTrigger trigger);
	}

	private static final Map<String,SubstitutionHandler> subs = new HashMap<String, SubstitutionHandler>();
	static {
		setupDefaultSubHandlers();
	}

	private static final Pattern predefSubPat = Pattern.compile("<([A-Z]+)>");

	@Override
	public String toString() {
		return String.format("ParsedCommand [%s], el=%s re=%s af=%s ap=%s st=%s wh=%s ch=%s ma=%s co=%s",
				command, elevated, restricted, affordable, applicable, status, whisper, chat, macro, console);
	}

	ParsedCommand(CommandSender sender, CommandTrigger trigger, Scanner scanner) throws SMSException {
		args = new ArrayList<String>();
		costs = new ArrayList<Cost>();
		elevated = restricted = chat = whisper = macro = console = commandlet = false;
		affordable = applicable = true;
		command = null;
		status = ReturnStatus.UNKNOWN;
		lastError = "no error";
		rawCommand = new StringBuilder();
		commandStopCondition = StopCondition.NONE;
		macroStopCondition = StopCondition.NONE;

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
					try {
						token = token.substring(1);
						Pattern oldDelimiter = scanner.delimiter();
						scanner.useDelimiter(quote);
						token = token + scanner.next();
						scanner.useDelimiter(oldDelimiter);
						scanner.next(); // swallow the closing quote
					} catch (NoSuchElementException e) {
						LogUtils.warning("Detected mismatched quote in command [" + rawCommand + quote + token + "]");
						throw new SMSException("Mismatched quote detected in command.");
					}
				}
				rawCommand.append(quote).append(token).append(quote).append(" ");
				if (command == null)
					command = token;
				else
					args.add(quote + token + quote);
				continue;
			}

			if (sender instanceof Player) {
				token = predefSubs((Player) sender, token, trigger);
			}

			if (cmdlets.hasCommandlet(token) && command == null) {
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
				commandStopCondition = StopCondition.ON_SUCCESS;
				macroStopCondition = StopCondition.ON_SUCCESS;
				break;
			} else if (token.equals("$$")) {
				// command terminator - run command and finish
				commandStopCondition = StopCondition.ON_SUCCESS;
				break;
			} else if (token.startsWith("$") && command == null && sender instanceof Player) {
				// apply a cost or costs
				applyCosts((Player) sender, token);
			} else if (token.equals("&&")) {
				// command separator - start another command IF this command is runnable
				commandStopCondition = StopCondition.ON_FAIL;
				break;
			} else {
				// just a plain string
				if (command == null)
					command = token;
				else
					args.add(token);
			}

			rawCommand.append(token).append(" ");
		}

		List<String> strings = MiscUtil.splitQuotedString(rawCommand.toString());
		quotedArgs = strings.toArray(new String[strings.size()]);

		if (!(sender instanceof Player) && command != null && command.startsWith("/")) {
			console = true;
		}

		Debugger.getInstance().debug(this.toString());
	}

	private String predefSubs(Player player, String token, CommandTrigger trigger) {
		Matcher m = predefSubPat.matcher(token);
		StringBuffer sb = new StringBuffer(token.length());
		while (m.find()) {
			String key = m.group(1);
			if (subs.containsKey(key)) {
				String repl = subs.get(key).sub(player, trigger);
				m.appendReplacement(sb, Matcher.quoteReplacement(repl));
			} else {
				String menuName = trigger == null ? "???" : trigger.getActiveMenu(player).getName();
				LogUtils.warning("unknown replacement <" + key + "> in command [" + command + "], menu " + menuName);
				sb.append("<").append(key).append(">");
			}
		}
		m.appendTail(sb);
		return sb.toString();
	}

	private void applyCosts(Player player, String token) {
		for (String c : token.substring(1).split(";")) {
			if (!c.isEmpty()) {
				try {
					Cost cost = Cost.parse(c);
					costs.add(cost);
					if (!cost.isAffordable(player)) {
						affordable = false;
					}
					if (!cost.isApplicable(player)) {
						applicable = false;
					}
				} catch (IllegalArgumentException e) {
					throw new SMSException(e.getMessage() + ": bad cost");
				}
			}
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
	 * @return The command name
	 */
	public String getCommand() {
		return command;
	}

	/**
	 * Get the argument list for the command (excluding the command), split on whitespace.
	 *
	 * @return The command's arguments
	 */
	public List<String> getArgs() {
		return args;
	}

	/**
	 * Get the argument list for the command (including the command), split on quoted substrings
	 * and/or whitespace.
	 *
	 * @return the argument list
	 */
	public String[] getQuotedArgs() {
		return quotedArgs;
	}

	/**
	 * Get the elevation status, i.e. whether the command should be (has been) run with permissions checks
	 * bypassed.
	 *
	 * @return true if elevated, false otherwise
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
	 * Set the restriction status of this command.
	 *
	 * @param restricted whether or not this command should be considered restricted
	 */
	public void setRestricted(boolean restricted) {
		this.restricted = restricted;
	}

	/**
	 * Get the affordable status, i.e. whether the command costs can be (have been) met by the player.
	 *
	 * @return true if the command is affordable, false otherwise
	 */
	public boolean isAffordable() {
		return affordable;
	}

	/**
	 * Get the applicable status, i.e. whether the command costs actually make sense.  E.g. repairing an
	 * item which doesn't have durability would not be applicable.
	 *
	 * @return true if the costs are applicable, false otherwise
	 */
	public boolean isApplicable() {
		return applicable;
	}

	/**
	 * Check if this command is a special "commandlet" registered with SMS.  {@link BaseCommandlet}
	 *
	 * @return true if this is a commandlet, false otherwise
	 */
	public boolean isCommandlet() {
		return commandlet;
	}

	/**
	 * Get the details of the costs for this command.
	 *
	 * @return a List of Cost objects
	 */
	public List<Cost> getCosts() {
		return costs;
	}

	/**
	 * Get the return status from actually running the command.
	 *
	 * @return the return status
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
	 * @return true if a macro is used, false otherwise
	 */
	public boolean isMacro() {
		return macro;
	}

	/**
	 * Check if the command sequence should be stopped, i.e. $$ or $$$
	 * was encountered following a command that actually ran (and was
	 * not ignored due to a restriction or cost check), or && was
	 * encountered following a command that did not run.
	 *
	 * @return true if the command was stopped, false otherwise
	 */
	public boolean isCommandStopped() {
		switch (commandStopCondition) {
		case NONE:
		default:
			return false;
		case ON_FAIL:
			return restricted || !affordable;
		case ON_SUCCESS:
			return !restricted && affordable;
		}
	}

	/**
	 * Check if any enclosing macro should be stopped, i.e. $$ or $$$
	 * was encountered following a command that actually ran (and was
	 * not ignored due to a restriction or cost check), or && was
	 * encountered following a command that did not run.
	 *
	 * @return true if a macro was stopped, false otherwise
	 */
	public boolean isMacroStopped() {
		switch (macroStopCondition) {
		case NONE:
		default:
			return false;
		case ON_FAIL:
			return restricted || !affordable;
		case ON_SUCCESS:
			return !restricted && affordable;
		}
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
	 * @return The error text
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
	 * @param index Index of the argument to get
	 * @return The argument
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

		if (check.isEmpty()) {
			return false;
		}

		String[] parts = check.split(":", 2);
		if (parts.length == 1) {
			// legacy check: just see if the player name matches
			return player.getName().equalsIgnoreCase(parts[0]);
		}

		String checkType = parts[0];
		String checkTerm = parts[1];

		switch (checkType.charAt(0)) {
		case 'g':
			return ScrollingMenuSign.permission != null && ScrollingMenuSign.permission.playerInGroup(player, checkTerm);
		case 'p':
			return looksLikeUUID(checkTerm) ? player.getUniqueId().equals(UUID.fromString(checkTerm)) : player.getName().equalsIgnoreCase(checkTerm);
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

	private static boolean looksLikeUUID(String s) {
		return s.length() == 36 && s.charAt(8) == '-';
	}

	private boolean isHoldingObject(Player player, String checkTerm) {
		if (checkTerm.matches("^[0-9]+$")) {
			LogUtils.warning("Checking for held items by ID is deprecated and will stop working in a future release.");
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

		Debugger.getInstance().debug(2, "doComparison: player=[" + player.getDisplayName() + "] var=[" + varSpec + "] val=[" + value + "] op=[" + op + "] test=[" + testValue + "]");
		Debugger.getInstance().debug(2, "doComparison: case-sensitive=" + !caseInsensitive + " regex=" + useRegex + " force-numeric=" + forceNumeric);

		try {
			if (op.equals("=")) {
				if (useRegex) {
					Pattern p = Pattern.compile(testValue, caseInsensitive ? Pattern.CASE_INSENSITIVE : 0);
					return p.matcher(value).matches();
				} else if (forceNumeric) {
					return Double.parseDouble(value) == Double.parseDouble(testValue);
				} else if (caseInsensitive) {
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

	public static void addSubstitutionHandler(String sub, SubstitutionHandler handler) {
		SMSValidate.isFalse(subs.containsKey(sub), "A handler is already registered for " + sub);
		SMSValidate.isTrue(StringUtils.isAlpha(sub), "Substitution string must be all alphabetic");
		subs.put(sub, handler);
	}

	private static void setupDefaultSubHandlers() {
		subs.put("X", new SubstitutionHandler() {
			@Override
			public String sub(Player player, CommandTrigger trigger) {
				return Integer.toString(player.getLocation().getBlockX());
			}
		});
		subs.put("Y", new SubstitutionHandler() {
			@Override
			public String sub(Player player, CommandTrigger trigger) {
				return Integer.toString(player.getLocation().getBlockY());
			}
		});
		subs.put("Z", new SubstitutionHandler() {
			@Override
			public String sub(Player player, CommandTrigger trigger) {
				return Integer.toString(player.getLocation().getBlockZ());
			}
		});
		subs.put("NAME", new SubstitutionHandler() {
			@Override
			public String sub(Player player, CommandTrigger trigger) {
				return player.getName();
			}
		});
		subs.put("DNAME", new SubstitutionHandler() {
			@Override
			public String sub(Player player, CommandTrigger trigger) {
				return player.getDisplayName();
			}
		});
		subs.put("UUID", new SubstitutionHandler() {
			@Override
			public String sub(Player player, CommandTrigger trigger) {
				return player.getUniqueId().toString();
			}
		});
		subs.put("N", subs.get("NAME"));
		subs.put("WORLD", new SubstitutionHandler() {
			@Override
			public String sub(Player player, CommandTrigger trigger) {
				return player.getWorld().getName();
			}
		});
		subs.put("I", new SubstitutionHandler() {
			@Override
			public String sub(Player player, CommandTrigger trigger) {
				LogUtils.warning("Command substitution <I> is deprecated and will stop working in a future release.");
				return player.getItemInHand() == null ? "0" : Integer.toString(player.getItemInHand().getTypeId());
			}
		});
		subs.put("INAME", new SubstitutionHandler() {
			@Override
			public String sub(Player player, CommandTrigger trigger) {
				return player.getItemInHand() == null ? "Air" : ItemNames.lookup(player.getItemInHand());
			}
		});
		subs.put("MONEY", new SubstitutionHandler() {
			@Override
			public String sub(Player player, CommandTrigger trigger) {
				if (ScrollingMenuSign.economy != null) {
					return formatMoney(ScrollingMenuSign.economy.getBalance(player.getName()));
				} else {
					return "0.00";
				}
			}
		});
		subs.put("VIEW", new SubstitutionHandler() {
			@Override
			public String sub(Player player, CommandTrigger trigger) {
				return trigger == null ? "" : trigger.getName();
			}
		});
		subs.put("EXP", new SubstitutionHandler() {
			@Override
			public String sub(Player player, CommandTrigger trigger) {
				return Integer.toString(new ExperienceManager(player).getCurrentExp());
			}
		});
		subs.put("COOLDOWN", new SubstitutionHandler() {
			@Override
			public String sub(Player player, CommandTrigger trigger) {
				CooldownCommandlet cc = (CooldownCommandlet) ScrollingMenuSign.getInstance().getCommandletManager().getCommandlet("COOLDOWN");
				long millis = cc.getLastCooldownTimeRemaining();
				int seconds = (int) (millis / 1000) % 60 ;
				if (millis < 60000) return String.format("%ds", seconds);
				int minutes = (int) ((millis / (1000*60)) % 60);
				if (millis < 3600000) return String.format("%dm %ds", minutes, seconds);
				int hours   = (int) ((millis / (1000*60*60)) % 24);
				return String.format("%dh %dm %ds", hours, minutes, seconds);
			}
		});
	}

	private static String formatMoney(double amount) {
		try {
			return ScrollingMenuSign.economy.format(amount);
		} catch (Exception e) {
			LogUtils.warning("Caught exception from " + ScrollingMenuSign.economy.getName() + " while trying to format quantity " + amount + ":");
			e.printStackTrace();
			LogUtils.warning("ScrollingMenuSign will continue but you should verify your economy plugin configuration.");
		}
		return new DecimalFormat("#0.00").format(amount) + " ";
	}

	public enum StopCondition {NONE, ON_SUCCESS, ON_FAIL}

}
