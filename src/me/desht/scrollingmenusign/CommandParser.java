package me.desht.scrollingmenusign;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.Set;

import me.desht.util.Debugger;
import me.desht.util.MiscUtil;
import me.desht.util.PermissionsUtils;
import me.desht.util.FakePlayer;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CommandParser {

	enum ReturnStatus { CMD_OK, CMD_STOPPED, CMD_IGNORED, MACRO_STOPPED, NO_PERMS, CMD_FAILED, CANT_AFFORD, CMD_RESTRICTED };
	private enum RunMode { CHECK_PERMS, EXECUTE };

	static boolean runSimpleCommandString(Player player, String command) {
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
	public static ReturnStatus runCommandString(Player player, String command) throws SMSException {
		return handleCommandString(player, command, RunMode.EXECUTE);
	}

	public static boolean verifyCreationPerms(Player player, String command) throws SMSException {
		return handleCommandString(player, command, RunMode.CHECK_PERMS) == ReturnStatus.CMD_OK;
	}

	static ReturnStatus handleCommandString(Player player, String command, RunMode mode) throws SMSException {

		// some preprocessing ...
		command = command.replace("<X>", "" + player.getLocation().getBlockX());
		command = command.replace("<Y>", "" + player.getLocation().getBlockY());
		command = command.replace("<Z>", "" + player.getLocation().getBlockZ());
		command = command.replace("<NAME>", player.getName());
		command = command.replace("<N>", player.getName());
		command = command.replace("<WORLD>", player.getWorld().getName());
		ItemStack stack =  player.getItemInHand();
		command = command.replace("<I>", stack != null ? "" + stack.getTypeId() : "0");

		Scanner scanner = new Scanner(command);

		ReturnStatus rs = ReturnStatus.CMD_OK;

		while (scanner.hasNext()) {
			ParsedCommand cmd = new ParsedCommand(player, scanner);

			if (mode == RunMode.EXECUTE) {
				rs = execute(player, cmd);
				if (rs == ReturnStatus.CMD_RESTRICTED || rs == ReturnStatus.CANT_AFFORD)
					continue;	// bypassing any potential CMD_STOPPED or MACRO_STOPPED
			} else if (mode == RunMode.CHECK_PERMS) {
				if (cmd.isElevated() && !PermissionsUtils.isAllowedTo(player, "scrollingmenusign.create.elevated"))
					return ReturnStatus.NO_PERMS;
				if (!cmd.getCosts().isEmpty() && !PermissionsUtils.isAllowedTo(player, "scrollingmenusign.create.cost"))
					return ReturnStatus.NO_PERMS;
			} else {
				// should never get here
				throw new IllegalArgumentException("unexpected run mode for parseCommandString()");
			}

			if (cmd.getStatus() == ReturnStatus.CMD_STOPPED || cmd.getStatus() == ReturnStatus.MACRO_STOPPED) {
				return cmd.getStatus();
			}
		}

		return rs;
	}

	private static boolean checkPlayer(Player player, String playerSpec) {
		if (playerSpec.startsWith("g:")) {
			return checkGroup(player, playerSpec.substring(2));
		} else if (playerSpec.startsWith("p:")) {
			return player.getName().equalsIgnoreCase(playerSpec.substring(2));
		} else if (playerSpec.startsWith("w:")) {
			return player.getWorld().getName().equalsIgnoreCase(playerSpec.substring(2));
		} else {
			return player.getName().equalsIgnoreCase(playerSpec);
		}
	}

	private static boolean checkGroup(Player player, String groupName) {
		return PermissionsUtils.isInGroup(player, groupName);
	}

	private static ReturnStatus execute(Player player, ParsedCommand cmd) {
		if (cmd.isRestricted())
			return ReturnStatus.CMD_RESTRICTED;

		if (!playerCanAfford(player, cmd.getCosts()))
			return ReturnStatus.CANT_AFFORD;

		chargePlayer(player, cmd.getCosts());

		if (cmd.getCommand() == null || cmd.getCommand().isEmpty())
			return cmd.getStatus();

		StringBuilder sb = new StringBuilder().append(cmd.getCommand()).append(" ");
		for (String a : cmd.getArgs()) {
			sb.append(a).append(" ");
		}
		String command = sb.toString().trim();

		String elevatedUser = SMSConfig.getConfiguration().getString("elevation_user", "&SMS");	
		FakePlayer fakePlayer = FakePlayer.fromPlayer(player, elevatedUser);

		if (cmd.isWhisper()) {
			// private message to the player
			MiscUtil.alertMessage(fakePlayer, command);
		} else if (cmd.isFakeuser()) {
			if (!PermissionsUtils.isAllowedTo(player, "scrollingmenusign.execute.elevated"))
				return ReturnStatus.NO_PERMS;

			// this is a /* command, to be run as the fake player
			Debugger.getDebugger().debug("execute (fakeuser): " + command);

			if (command.startsWith("/")) {
				if (!Bukkit.getServer().dispatchCommand(fakePlayer, command.substring(1)))
					return ReturnStatus.CMD_FAILED;
			} else {
				fakePlayer.chat(command);
			}
		} else if (cmd.isElevated()) {
			if (!PermissionsUtils.isAllowedTo(player, "scrollingmenusign.execute.elevated"))
				return ReturnStatus.NO_PERMS;

			// this is a /@ command, to be run as the real player but with borrowed permissions
			Debugger.getDebugger().debug("execute (elevated): " + sb.toString());

			Set<String>opsSet = null;
			if (fakePlayer.isOp())
				opsSet = PermissionsUtils.grantOpStatus(player);

			List<String> tempPerms = null;
			try {
				tempPerms = PermissionsUtils.elevate(player, elevatedUser);
				if (tempPerms == null && !player.isOp()) {
					MiscUtil.log(Level.WARNING,
					             "No permission nodes found for " + fakePlayer.getName() + " and " + player.getName() + " is not an op. " +
					"SMS permission elevation is not likely to succeed.");
				}
				player.chat(sb.toString().trim());
			} finally {
				PermissionsUtils.deElevate(player, tempPerms);
				PermissionsUtils.revokeOpStatus(player, opsSet);
			}
		} else {
			// just an ordinary command, no special privilege elevation
			Debugger.getDebugger().debug("execute (normal): " + sb.toString());
			player.chat(sb.toString().trim());
		}

		return cmd.getStatus();
	}

	@SuppressWarnings("deprecation")
	private static void chargePlayer(Player player, List<Cost> costs) {
		for (Cost c : costs) {
			if (c.getQuantity() == 0)
				continue;
			if (c.getId() == 0) {
				if (ScrollingMenuSign.getEconomy() != null) {
					ScrollingMenuSign.getEconomy().getAccount(player.getName()).subtract(c.getQuantity());
				}
			} else {
				if (c.getQuantity() > 0) 
					chargeItems(player, c);
				else
					grantItems(player, c);
			}
		}
		player.updateInventory();
	}

	/**
	 * Give items to a player.
	 * 
	 * @param player
	 * @param c
	 */
	private static void grantItems(Player player, Cost c) {
		int quantity = -c.getQuantity();

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
	private static void chargeItems(Player player, Cost c) {
		HashMap<Integer, ? extends ItemStack> matchingInvSlots = player.getInventory().all(Material.getMaterial(c.getId()));

		int remainingCheck = c.getQuantity();
		for (Entry<Integer, ? extends ItemStack> entry : matchingInvSlots.entrySet()) {
			if (c.getData() == null || (entry.getValue().getData() != null && entry.getValue().getData().getData() == c.getData())) {
				remainingCheck -= entry.getValue().getAmount();
				if (remainingCheck <= 0) {
					if (remainingCheck == 0)
						player.getInventory().remove(entry.getValue());
					else
						entry.getValue().setAmount(-remainingCheck);
					break;
				} else {
					player.getInventory().remove(entry.getValue());
				}
			}
		}
	}

	/**
	 * Check if the player can afford to pay the costs.
	 * 
	 * @param player
	 * @param costs
	 * @return
	 */
	private static boolean playerCanAfford(Player player, List<Cost> costs) {
		for (Cost c : costs) {
			if (c.getQuantity() <= 0)
				continue;
			if (c.getId() == 0) {
				if (ScrollingMenuSign.getEconomy() != null) {
					if (!ScrollingMenuSign.getEconomy().getAccount(player.getName()).hasEnough(c.getQuantity()))
						return false;
				}
			} else {
				HashMap<Integer, ? extends ItemStack> matchingInvSlots = player.getInventory().all(Material.getMaterial(c.getId()));
				int remainingCheck = c.getQuantity();
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
			}
		}
		return true;
	}

	static class Cost {
		private int id;
		private Byte data;
		private int quantity;

		Cost(int id) {
			this(id, (byte) 0, 1);
		}

		Cost(int id, byte data, int quantity) {
			this.id = id;
			this.data = data;
			this.quantity = quantity;
		}

		Cost(String costSpec) {
//			System.out.println("cost = " + costSpec);
			String[] s1 = costSpec.split(",");
			if (s1.length != 2)
				throw new IllegalArgumentException("cost: format must be <item,quantity>");
			String[] s2 = s1[0].split(":");
			if (s2.length < 1 || s2.length > 2)
				throw new IllegalArgumentException("cost: item format must be <id[:data]>");
			id = Integer.parseInt(s2[0]);
			data = s2.length == 2 ? Byte.parseByte(s2[1]) : null;
			quantity = Integer.parseInt(s1[1]);
		}

		public int getId() {
			return id;
		}

		public Byte getData() {
			return data;
		}

		public int getQuantity() {
			return quantity;
		}
	}

	static class ParsedCommand {
		private String command;
		private List<String> args;
		private boolean elevated;
		private boolean restricted;
		private List<Cost> costs;
		private ReturnStatus status;
		private boolean fakeuser;
		private boolean whisper;

		ParsedCommand (Player player, Scanner scanner) throws SMSException {
			args = new ArrayList<String>();
			costs = new ArrayList<Cost>();
			elevated = restricted = whisper = false;
			command = null;
			status = ReturnStatus.CMD_OK;

			while (scanner.hasNext()) {
				String token = scanner.next();

				if (token.startsWith("/@") && command == null) {
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
				} else if (token.startsWith("@!") && command == null) {
					// verify NOT player or group name
					if (checkPlayer(player, token.substring(2))) {
						restricted = true;
					}
				} else if (token.startsWith("@") && command == null) {
					// verify player or group name
					if (!checkPlayer(player, token.substring(1))) {
						restricted = true;
					}
				} else if (token.equals("$$$")) {
					// command terminator, and stop any macro too
					status = ReturnStatus.MACRO_STOPPED;
					return;
				} else if (token.equals("$$")) {
					// command terminator - run command and finish
					status = ReturnStatus.CMD_STOPPED;
					return;
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

		public List<Cost> getCosts() {
			return costs;
		}

		public ReturnStatus getStatus() {
			return status;
		}

		public boolean isFakeuser() {
			return fakeuser;
		}

		public boolean isWhisper() {
			return whisper;
		}
	}
}
