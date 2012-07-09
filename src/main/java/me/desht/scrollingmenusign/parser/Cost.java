package me.desht.scrollingmenusign.parser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import me.desht.dhutils.ExperienceManager;
import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.enums.CostType;

public class Cost {

	private final CostType type;
	private final int id;
	private final Byte data;
	private final double quantity;

	/**
	 * Construct a new Cost object, charging 1 of the given item ID
	 * 
	 * @param id	ID of the item to charge, 0 for economy credits
	 */
	public Cost(int id) {
		this(id, null, 1);
	}

	/**
	 * Construct a new Cost object.
	 * 
	 * @param id	ID of the item to charge, 0 for economy credits
	 * @param data	Data value of the item, may be null
	 * @param quantity	Quantity to charge, may be negative
	 */
	public Cost(int id, Byte data, double quantity) {
		this(id == 0 ? CostType.MONEY : CostType.ITEM, id, data, quantity);
	}

	/**
	 * Construct a new Cost object of the given type.
	 * 
	 * @param type	Type of cost to apply
	 * @param id	ID of the item to charge, 0 for economy credits
	 * @param data	Data value of the item, may be null
	 * @param quantity	Quantity to charge, may be negative
	 */
	public Cost(CostType type, int id, Byte data, double quantity) {
		this.type = type;
		this.id = id;
		this.data = data;
		this.quantity = quantity;
	}

	/**
	 * Construct a new Cost object from the given string specification.
	 * 
	 * @param costSpec	The specification, in the format <i>id[:data],quantity</i>
	 * @throws IllegalArgumentException if the specification contains an error
	 */
	public Cost(String costSpec) {
		//System.out.println("cost = " + costSpec);
		String[] s1 = costSpec.split(",");
		if (s1.length != 2)
			throw new IllegalArgumentException("cost: format must be <item,quantity>");
		String[] s2 = s1[0].split(":");
		if (s2.length < 1 || s2.length > 2)
			throw new IllegalArgumentException("cost: item format must be <id[:data]>");
		String itemType = s2[0];
		if (itemType.equalsIgnoreCase("E")) {
			id = 0;
			type = CostType.MONEY;
		} else if (itemType.equalsIgnoreCase("X")) {
			id = 0;
			type = CostType.EXPERIENCE;
		} else if (itemType.equalsIgnoreCase("F")) {
			id = 0;
			type = CostType.FOOD;
		} else if (itemType.equalsIgnoreCase("H")) {
			id = 0;
			type = CostType.HEALTH;
		} else if (itemType.matches("[0-9]+")) {
			id = Integer.parseInt(s2[0]);
			type = id == 0 ? CostType.MONEY : CostType.ITEM;
		} else if (itemType.length() > 1) {
			Material mat = Material.matchMaterial(itemType);
			if (mat == null) throw new IllegalArgumentException("cost: unknown material '" + itemType + "'");
			id = mat.getId();
			type = CostType.ITEM;
		} else {
			throw new IllegalArgumentException("cost: unknown item type '" + itemType + "'");
		}
		data = s2.length == 2 ? Byte.parseByte(s2[1]) : null;
		quantity = Double.parseDouble(s1[1]);
	}

	public int getId() {
		return id;
	}

	public Byte getData() {
		return data;
	}

	public double getQuantity() {
		return quantity;
	}

	public CostType getType() {
		return type;
	}

	/**
	 * Give items to a player.
	 * 
	 * @param player
	 */
	public void grantItems(Player player) {
		if (player == null) {
			return;
		}

		int maxStackSize = player.getInventory().getMaxStackSize();
		int quantity = (int) -getQuantity();
		int dropped = 0;
		
		while (quantity > maxStackSize) {
			dropped += addItems(player, maxStackSize);
			quantity -= maxStackSize;
		}
		dropped += addItems(player, quantity);
		
		if (dropped > 0) {
			MiscUtil.statusMessage(player, "&6Your inventory is full.  Some items dropped.");
		}
	}

	private ItemStack makeStack(int quantity) {
		return data == null ? new ItemStack(getId(), quantity) : new ItemStack(getId(), quantity, (short)0, getData().byteValue());
	}

	private int addItems(Player player, int quantity) {
		Map<Integer, ItemStack> toDrop = player.getInventory().addItem(makeStack(quantity));
		if (toDrop.size() == 0) {
			return 0;
		}

		int dropped = 0;
		for (ItemStack is : toDrop.values()) {
			player.getWorld().dropItemNaturally(player.getLocation(), is);
			dropped += is.getAmount();
		}
		return dropped;
	}

	/**
	 * Take items from a player's inventory.  Doesn't check to see if there is enough -
	 * use playerCanAfford() for that.
	 * 
	 * @param player
	 */
	public void chargeItems(Player player) {
		if (player == null) {
			return;
		}

		HashMap<Integer, ? extends ItemStack> matchingInvSlots = player.getInventory().all(Material.getMaterial(getId()));

		int remainingCheck = (int) getQuantity();
		for (Entry<Integer, ? extends ItemStack> entry : matchingInvSlots.entrySet()) {
			if (getData() == null || (entry.getValue().getData() != null && entry.getValue().getData().getData() == getData())) {
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

	/**
	 * Charge a list of costs to the given player.
	 * 
	 * @param player	The player to charge
	 * @param costs		A List of Cost objects
	 */
	@SuppressWarnings("deprecation")
	public static void chargePlayer(CommandSender sender, List<Cost> costs) {
		if (!(sender instanceof Player)) {
			return;
		}
		Player player = (Player) sender;

		for (Cost c : costs) {
			if (c.getQuantity() == 0.0)
				continue;
			switch (c.getType()) {
			case MONEY:
				if (ScrollingMenuSign.economy != null) {
					EconomyResponse resp;
					if (c.getQuantity() < 0.0) {
						resp = ScrollingMenuSign.economy.depositPlayer(player.getName(), -c.getQuantity());
					} else {
						resp = ScrollingMenuSign.economy.withdrawPlayer(player.getName(), c.getQuantity());
					}
					if (!resp.transactionSuccess()) {
						throw new SMSException("Economy problem: " + resp.errorMessage);
					}
				}
				break;
			case ITEM:
				if (c.getQuantity() > 0) 
					c.chargeItems(player);
				else
					c.grantItems(player);
				player.updateInventory();
				break;
			case EXPERIENCE:
				ExperienceManager em = new ExperienceManager(player);
				em.changeExp((int) -c.getQuantity());
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

	/**
	 * Check if the player can afford to pay the costs.
	 * 
	 * @param player
	 * @param costs
	 * @return	True if the costs are affordable, false otherwise
	 */
	public static boolean playerCanAfford(CommandSender sender, List<Cost> costs) {
		if (!(sender instanceof Player)) {
			return true;
		}
		Player player = (Player) sender;

		for (Cost c : costs) {
			if (c.getQuantity() <= 0)
				continue;

			switch (c.getType()) {
			case MONEY:
				if (ScrollingMenuSign.economy == null) {
					return true;
				}
				if (ScrollingMenuSign.economy.getBalance(player.getName()) < c.getQuantity()) {
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
				ExperienceManager em = new ExperienceManager(player);
				if (em.getCurrentExp() < c.getQuantity())
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

	private static int getNewQuantity(int original, double adjust, int min, int max) {
		int newQuantity = original - (int) adjust;
		if (newQuantity < min) {
			newQuantity = min;
		} else if (newQuantity > max) {
			newQuantity = max;	
		}
		return newQuantity;
	}
}