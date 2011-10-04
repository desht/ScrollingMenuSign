package me.desht.scrollingmenusign.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.Map.Entry;
import java.util.logging.Level;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.enums.ReturnStatus;
import me.desht.util.MiscUtil;
import me.desht.util.PermissionsUtils;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ParsedCommand {
	private String command;
	private List<String> args;
	private boolean elevated;
	private boolean restricted;
	private boolean affordable;
	private List<Cost> costs;
	private ReturnStatus status;
	private boolean fakeuser;
	private boolean whisper;
	private boolean macro;
	private boolean commandStopped, macroStopped;

	ParsedCommand (Player player, Scanner scanner) throws SMSException {
		args = new ArrayList<String>();
		costs = new ArrayList<Cost>();
		elevated = restricted = whisper = macro = false;
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
			} else if (token.startsWith("/@") && command == null) {
				// elevated command
				command = "/" + token.substring(2);
				elevated = true;
			} else if (token.startsWith("/*") && command == null) {
				// fakeuser command
				command = "/" + token.substring(2);
				fakeuser = true;
			} else if (token.startsWith("/") && command == null) {
				// regular command
				command = token;
				elevated = false;
			} else if (token.startsWith("\\\\") && command == null) {
				// a whisper string
				command = token.substring(2);
				whisper = true;
			} else if (token.startsWith("\\") && command == null) {
				// a chat string
				command = token.substring(1);
				elevated = false;
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
			} else if (token.equals("$$$")) {
				// command terminator, and stop any macro too
				macroStopped = true;
				return;
			} else if (token.equals("$$")) {
				// command terminator - run command and finish
				commandStopped = true;
				return;
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

				if (!playerCanAfford(player, getCosts())) {
					affordable = false;
				}	
			} else if (token.equals("&&")) {
				// command separator - start another command
				return;
			} else {
				// just a plain string
				if (command == null)
					command = token;
				else
					args.add(token);
			}
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
	
	public boolean isFakeuser() {
		return fakeuser;
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

	public String arg(int index) {
		return args.get(index);
	}
	
	private boolean restrictionCheck(Player player, String check) {
		if (check.startsWith("g:")) {
			return checkGroup(player, check.substring(2));
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

	/**
	 * Check if the player can afford to pay the costs.
	 * 
	 * @param player
	 * @param costs
	 * @return
	 */
	private boolean playerCanAfford(Player player, List<Cost> costs) {
		for (Cost c : costs) {
			if (c.getQuantity() <= 0)
				continue;

			switch (c.getType()) {
			case MONEY:
				if (ScrollingMenuSign.getEconomy() == null) {
					return true;
				}
				if (!ScrollingMenuSign.getEconomy().getAccount(player.getName()).hasEnough(c.getQuantity())) {
					return false;
				}
				break;
			case ITEM:
				HashMap<Integer, ? extends ItemStack> matchingInvSlots = player.getInventory().all(Material.getMaterial(c.getId()));
				int remainingCheck = (int) c.getQuantity();
				for (Entry<Integer, ? extends ItemStack> entry : matchingInvSlots.entrySet()) {
					if(c.getData() == null || (entry.getValue().getData() != null && entry.getValue().getData().getData() == c.getData())) {
						remainingCheck -= entry.getValue().getAmount();
						if(remainingCheck <= 0)
							break;
					}
				}
				if (remainingCheck > 0) {
					return false;
				}
				break;
			case EXPERIENCE:
				if (player.getTotalExperience() < c.getQuantity())
					return false;
				break;
			case FOOD:
				if (player.getFoodLevel() <= c.getQuantity())
					return false;
				break;
			case HEALTH:
				if (player.getHealth() <= c.getQuantity())
					return false;
				break;
			}
		}
		return true;
	}
	
	private boolean checkGroup(Player player, String groupName) {
		return PermissionsUtils.isInGroup(player, groupName);
	}

}
