package me.desht.scrollingmenusign;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.Map.Entry;

import me.desht.util.MiscUtil;
import me.desht.util.PermissionsUtils;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CommandParser {
	enum ReturnStatus { CMD_OK, CMD_STOPPED, MACRO_STOPPED };
	
	static boolean runSimpleCommandString(Player player, String command) {
		player.chat(command);
		return true;
	}

	// TODO: this is not yet fully functional
	static ReturnStatus runCommandString(Player player, String command) {
		
		command = command.replace("<X>", "" + player.getLocation().getBlockX());
        command = command.replace("<Y>", "" + player.getLocation().getBlockY());
        command = command.replace("<Z>", "" + player.getLocation().getBlockZ());
        command = command.replace("<NAME>", player.getName());
        command = command.replace("<N>", player.getName());
        command = command.replace("<WORLD>", player.getWorld().getName());
        ItemStack stack =  player.getItemInHand();
        command = command.replace("<I>", stack != null ? "" + stack.getTypeId() : "0");
        
		Scanner scanner = new Scanner(command);

		String cmd = null;
		List<String> args = new ArrayList<String>();
		List<Cost> costs = new ArrayList<Cost>();

		boolean elevate = false;

		while (scanner.hasNext()) {
			String token = scanner.next();

			if (token.startsWith("/@") && cmd == null) {
				// elevated command
				cmd = "/" + token.substring(2);
				elevate = true;
			} else if (token.startsWith("/") && cmd == null) {
				// regular command
				cmd = token;
				elevate = false;
			} else if (token.startsWith("\\") && cmd == null) {
				// a chat string
				cmd = token.substring(1);
				elevate = false;
			} else if (token.startsWith("$") && cmd == null) {
				// apply a cost or costs
				System.out.println("token = [" + token + "]");
				for (String c : token.substring(1).split(";")) {
					if (!c.isEmpty())
						costs.add(new Cost(c));
				}
			} else if (token.startsWith("@!") && cmd == null) {
				// verify NOT player or group name
				if (checkPlayer(player, token.substring(2)))
					return ReturnStatus.CMD_STOPPED;
			} else if (token.startsWith("@") && cmd == null) {
				// verify player or group name
				if (!checkPlayer(player, token.substring(1)))
					return ReturnStatus.CMD_STOPPED;
			} else if (token.equals("$$$")) {
				// command terminator, and stop any macro too
				execute(player, costs, cmd, args, elevate);
				return ReturnStatus.MACRO_STOPPED;
			} else if (token.equals("$$")) {
				// command terminator - run command and finish
				execute(player, costs, cmd, args, elevate);
				return ReturnStatus.CMD_STOPPED;
			} else if (token.equals("&&")) {
				// command separator - start another command
				ReturnStatus rs = execute(player, costs, cmd, args, elevate);
				if (rs != ReturnStatus.CMD_OK)
					return rs;
				cmd = null;
				args.clear();
				costs.clear();
			} else {
				// just a plain string
				if (cmd == null)
					cmd = token;
				else
					args.add(token);
			}
		}
		
		return execute(player, costs, cmd, args, elevate);
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

	private static ReturnStatus execute(Player player, List<Cost> costs, String cmd, List<String> args, boolean elevate) {
		if (!playerCanAfford(player, costs)) {
			MiscUtil.errorMessage(player, "You can't afford that.");
			return ReturnStatus.CMD_STOPPED;
		} else {
			chargePlayer(player, costs);
		}
		
		if (cmd == null || cmd.isEmpty())
			return ReturnStatus.CMD_OK;
		

		StringBuilder sb = new StringBuilder().append(cmd).append(" ");
		for (String a : args) {
			sb.append(a).append(" ");
		}
		
		List<String> tempPerms = null;
		if (elevate)
			tempPerms = PermissionsUtils.elevate(player, "CS");
		
		System.out.println("execute: " + sb.toString());
		player.chat(sb.toString().trim());
		
		if (elevate) {
			PermissionsUtils.deElevate(player, tempPerms);
		}
		
		return ReturnStatus.CMD_OK;
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
			System.out.println("cost = " + costSpec);
			String[] s1 = costSpec.split(",");
			if (s1.length != 2)
				throw new IllegalArgumentException("cost format must be <item,quantity>");
			String[] s2 = s1[0].split(";");
			if (s2.length < 1 || s2.length > 2)
				throw new IllegalArgumentException("item format must be <id[,data]>");
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
}
