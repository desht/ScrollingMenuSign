package me.desht.scrollingmenusign.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Level;

import me.desht.scrollingmenusign.SMSConfig;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMacro;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.enums.ReturnStatus;
import me.desht.util.Debugger;
import me.desht.util.MiscUtil;
import me.desht.util.PermissionsUtils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachment;

public class CommandParser {
	private Set<String> macroHistory;
	
	public CommandParser() {
		this.macroHistory = new HashSet<String>();
	}
	
	private enum RunMode { CHECK_PERMS, EXECUTE };

	boolean runSimpleCommandString(Player player, String command) {
		player.chat(command);
		return true;
	}

	/**
	 * Parse and run a command string via the SMS command engine
	 * 
	 * @param player	Player who is running the command
	 * @param command	Command to be run
	 * @return			A return status indicating the outcome of the command
	 * @throws SMSException	
	 */
	public ReturnStatus runCommandString(Player player, String command) throws SMSException {
		ParsedCommand cmd = handleCommandString(player, command, RunMode.EXECUTE);
		return cmd.getStatus();
	}

	public boolean verifyCreationPerms(Player player, String command) throws SMSException {
		ParsedCommand cmd = handleCommandString(player, command, RunMode.CHECK_PERMS);
		return cmd.getStatus() == ReturnStatus.CMD_OK;
	}
	
	ParsedCommand handleCommandString(Player player, String command, RunMode mode) throws SMSException {

		// do some preprocessing ...
		command = command.replace("<X>", "" + player.getLocation().getBlockX());
		command = command.replace("<Y>", "" + player.getLocation().getBlockY());
		command = command.replace("<Z>", "" + player.getLocation().getBlockZ());
		command = command.replace("<NAME>", player.getName());
		command = command.replace("<N>", player.getName());
		command = command.replace("<WORLD>", player.getWorld().getName());
		ItemStack stack =  player.getItemInHand();
		command = command.replace("<I>", stack != null ? "" + stack.getTypeId() : "0");

		Scanner scanner = new Scanner(command);

		ParsedCommand cmd = null;
		while (scanner.hasNext()) {
			cmd = new ParsedCommand(player, scanner);

			if (mode == RunMode.EXECUTE) {
				if (cmd.isRestricted() || !cmd.isAffordable()) {
					// bypassing any potential cmd.isCommandStopped() or cmd.isMacroStopped()
					continue;
				}
				execute(player, cmd);
			} else if (mode == RunMode.CHECK_PERMS) {
				cmd.setStatus(ReturnStatus.CMD_OK);
				if (cmd.isElevated() && !PermissionsUtils.isAllowedTo(player, "scrollingmenusign.create.elevated")) {
					cmd.setStatus(ReturnStatus.NO_PERMS);
					return cmd;
				} else if (!cmd.getCosts().isEmpty() && !PermissionsUtils.isAllowedTo(player, "scrollingmenusign.create.cost")) {
					cmd.setStatus(ReturnStatus.NO_PERMS);
					return cmd;
				}
			} else {
				// should never get here
				throw new IllegalArgumentException("unexpected run mode for parseCommandString()");
			}
			if (cmd.isCommandStopped() || cmd.isMacroStopped()) {
				break;
			}
		}

		return cmd;
	}

	boolean restrictionCheck(Player player, String check) {
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

	private boolean checkGroup(Player player, String groupName) {
		return PermissionsUtils.isInGroup(player, groupName);
	}

	private void execute(Player player, ParsedCommand cmd) {
		if (cmd.isRestricted() || !cmd.isAffordable()) 
			return;

		chargePlayer(player, cmd.getCosts());

		if (cmd.getCommand() == null || cmd.getCommand().isEmpty())
			return;

		StringBuilder sb = new StringBuilder().append(cmd.getCommand()).append(" ");
		for (String a : cmd.getArgs()) {
			sb.append(a).append(" ");
		}
		String command = sb.toString().trim();

		if (cmd.isMacro()) {
			// run a macro
			String macroName = cmd.getCommand();
			if (macroHistory.contains(macroName)) {
				cmd.setStatus(ReturnStatus.WOULD_RECURSE);
				return;
			} else if (SMSMacro.hasMacro(macroName)) {
				macroHistory.add(macroName);
				for (String c : SMSMacro.getCommands(macroName)) {
					for (int i = 0; i < cmd.getArgs().size(); i++) {
						c = c.replace("<" + (i + 1) + ">", cmd.arg(i));
					}
					try {
						ParsedCommand cmd2 = handleCommandString(player, c, RunMode.EXECUTE);
						if (cmd2.isMacroStopped())
							break;
					} catch (SMSException e) {
						MiscUtil.log(Level.WARNING, "Caught exception parsing " + c + ": " + e.getMessage());
					}
				}
				return;
			} else {
				cmd.setStatus(ReturnStatus.BAD_MACRO);
				return;
			}
		} else if (cmd.isWhisper()) {
			// private message to the player
			MiscUtil.alertMessage(player, command);
		} else if (cmd.isFakeuser() || cmd.isElevated()) {
			// this is a /@ command, to be run as the real player, but with temporary permissions
			// (this now also handles the /* fake-player style, which is no longer directly supported)

			if (!PermissionsUtils.isAllowedTo(player, "scrollingmenusign.execute.elevated")) {
				cmd.setStatus(ReturnStatus.NO_PERMS);
				return;
			}

			Debugger.getDebugger().debug("execute (elevated): " + sb.toString());

			List<PermissionAttachment> attachments = new ArrayList<PermissionAttachment>();
			boolean tempOp = false;
			try {
				ScrollingMenuSign plugin = ScrollingMenuSign.getInstance();
				for (String node : SMSConfig.getConfiguration().getStringList("sms.elevation.nodes", null)) {
					if (!node.isEmpty() && !player.hasPermission(node)) {
						System.out.println("add node: " + node);
						attachments.add(player.addAttachment(plugin, node, true));
					}
				}
				if (SMSConfig.getConfiguration().getBoolean("sms.elevation.grant_op", false) && !player.isOp()) {
					tempOp = true;
					player.setOp(true);
				}
				if (command.startsWith("/")) {
					if (!Bukkit.getServer().dispatchCommand(player, command.substring(1))) {
						cmd.setStatus(ReturnStatus.CMD_FAILED);
					}
				} else {
					player.chat(command);
				}
			} finally {
				// revoke all temporary permissions granted to the user
				for (PermissionAttachment att : attachments) {
					for (Entry<String,Boolean> e : att.getPermissions().entrySet()) {
						System.out.println("remove attachment: " + e.getKey() + " = " + e.getValue());
					}
					player.removeAttachment(att);
				}
				if (tempOp) {
					player.setOp(false);
				}
			}
		} else {
			// just an ordinary command, no special privilege elevation
			Debugger.getDebugger().debug("execute (normal): " + sb.toString());
			if (command.startsWith("/")) {
				if (!Bukkit.getServer().dispatchCommand(player, command.substring(1))) {
					cmd.setStatus(ReturnStatus.CMD_FAILED);
				}
			} else {
				player.chat(command);
			}
		}
	}

	@SuppressWarnings("deprecation")
	private void chargePlayer(Player player, List<Cost> list) {
		for (Cost c : list) {
			if (c.getQuantity() == 0.0)
				continue;
			switch (c.getType()) {
			case MONEY:
				if (ScrollingMenuSign.getEconomy() != null) {
					ScrollingMenuSign.getEconomy().getAccount(player.getName()).subtract(c.getQuantity());
				}
				break;
			case ITEM:
				if (c.getQuantity() > 0) 
					chargeItems(player, c);
				else
					grantItems(player, c);
				player.updateInventory();
				break;
			case EXPERIENCE:
				player.setTotalExperience(getNewQuantity(player.getTotalExperience(), c.getQuantity(), 0, Integer.MAX_VALUE));
				break;
			case FOOD:
				player.setFoodLevel(getNewQuantity(player.getFoodLevel(), c.getQuantity(), 1, 20));
				break;
			case HEALTH:
				player.setHealth(getNewQuantity(player.getHealth(), c.getQuantity(), 1, 20));
				break;
			}
		}
	}

	private int getNewQuantity(int original, double adjust, int min, int max) {
		int newQuantity = original - (int) adjust;
		if (newQuantity < min) {
			newQuantity = min;
		} else if (newQuantity > max) {
			newQuantity = max;	
		}
		return newQuantity;
	}

	/**
	 * Give items to a player.
	 * 
	 * @param player
	 * @param c
	 */
	private void grantItems(Player player, Cost c) {
		int quantity = (int) -c.getQuantity();

		ItemStack stack = null;
		while (quantity > 64) {
			if (c.getData() == null)
				stack = new ItemStack(c.getId(), 64);
			else
				stack = new ItemStack(c.getId(), 64, (short)0, c.getData().byteValue());
			player.getInventory().addItem(stack);
			quantity -= 64;
		}

		if (c.getData() == null)
			stack = new ItemStack(c.getId(), quantity);
		else
			stack = new ItemStack(c.getId(), quantity, (short)0, c.getData().byteValue());
		player.getInventory().addItem(stack);
	}

	/**
	 * Take items from a player's inventory.  Doesn't check to see if there is enough -
	 * use playerCanAfford() for that.
	 * 
	 * @param player
	 * @param c
	 */
	private void chargeItems(Player player, Cost c) {
		HashMap<Integer, ? extends ItemStack> matchingInvSlots = player.getInventory().all(Material.getMaterial(c.getId()));

		int remainingCheck = (int) c.getQuantity();
		for (Entry<Integer, ? extends ItemStack> entry : matchingInvSlots.entrySet()) {
			if (c.getData() == null || (entry.getValue().getData() != null && entry.getValue().getData().getData() == c.getData())) {
				remainingCheck -= entry.getValue().getAmount();
				if (remainingCheck < 0) {
					entry.getValue().setAmount(-remainingCheck);
					break;
				} else if (remainingCheck == 0) {	
					player.getInventory().removeItem(entry.getValue());
					break;
				} else {
					player.getInventory().removeItem(entry.getValue());
				}
			}
		}
	}

	

}
