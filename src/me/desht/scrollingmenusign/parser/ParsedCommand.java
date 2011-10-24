package me.desht.scrollingmenusign.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.enums.ReturnStatus;
import me.desht.util.MiscUtil;
import me.desht.util.PermissionsUtils;

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
	private boolean macro;
	private boolean commandStopped, macroStopped;
	private boolean console;

	ParsedCommand (Player player, Scanner scanner) throws SMSException {
		args = new ArrayList<String>();
		costs = new ArrayList<Cost>();
		elevated = restricted = whisper = macro = console = false;
		commandStopped = macroStopped = false;
		affordable = true;
		command = null;
		status = ReturnStatus.CMD_OK;

		while (scanner.hasNext()) {
			String token = scanner.next();

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
				command = token.substring(2);
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
				macroStopped = true;
				break;
			} else if (token.equals("$$") && !restricted && affordable ) {
				// command terminator - run command and finish
				commandStopped = true;
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
			command = command.substring(1);
		}
	}

	public String getCommand() {
		return command;
	}

	public List<String> getArgs() {
		return args;
	}

	public boolean isElevated() {
		return elevated;
	}

	public boolean isRestricted() {
		return restricted;
	}

	public boolean isAffordable() {
		return affordable;
	}

	public List<Cost> getCosts() {
		return costs;
	}

	public ReturnStatus getStatus() {
		return status;
	}

	public void setStatus(ReturnStatus status) {
		this.status = status;
	}

	public boolean isWhisper() {
		return whisper;
	}

	public boolean isMacro() {
		return macro;
	}
	
	public boolean isCommandStopped() {
		return commandStopped;
	}

	public boolean isMacroStopped() {
		return macroStopped;
	}

	public boolean isConsole() {
		return console;
	}

	public String arg(int index) {
		return args.get(index);
	}
	
	private boolean restrictionCheck(Player player, String check) {
		if (player == null) {
			// no restrictions apply to being run from the console
			return true;
		}
		
		if (check.startsWith("g:")) {
//			return PermissionsUtils.isInGroup(player, check.substring(2));
			return PermissionsUtils.isAllowedTo(player, "scrollingmenusign.groups." + check.substring(2));
		} else if (check.startsWith("p:")) {
			return player.getName().equalsIgnoreCase(check.substring(2));
		} else if (check.startsWith("w:")) {
			return player.getWorld().getName().equalsIgnoreCase(check.substring(2));
		} else if (check.startsWith("n:")) {
			return player.hasPermission(check.substring(2));
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
