package me.desht.scrollingmenusign.parser;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.enums.CostType;

public class Cost {
	private CostType type;
	private int id;
	private Byte data;
	private double quantity;

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
	 * @param costSpec	The specification
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
			type = CostType.MONEY;
		} else if (itemType.equalsIgnoreCase("X")) {
			type = CostType.EXPERIENCE;
		} else if (itemType.equalsIgnoreCase("F")) {
			type = CostType.FOOD;
		} else if (itemType.equalsIgnoreCase("H")) {
			type = CostType.HEALTH;
		} else {
			id = Integer.parseInt(s2[0]);
			type = id == 0 ? CostType.MONEY : CostType.ITEM;
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
		int quantity = (int) -getQuantity();

		ItemStack stack = null;
		while (quantity > 64) {
			if (getData() == null)
				stack = new ItemStack(getId(), 64);
			else
				stack = new ItemStack(getId(), 64, (short)0, getData().byteValue());
			player.getInventory().addItem(stack);
			quantity -= 64;
		}

		if (getData() == null)
			stack = new ItemStack(getId(), quantity);
		else
			stack = new ItemStack(getId(), quantity, (short)0, getData().byteValue());
		player.getInventory().addItem(stack);
	}

	/**
	 * Take items from a player's inventory.  Doesn't check to see if there is enough -
	 * use playerCanAfford() for that.
	 * 
	 * @param player
	 */
	public void chargeItems(Player player) {
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
	
	@SuppressWarnings("deprecation")
	public static void chargePlayer(Player player, List<Cost> list) {
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
					c.chargeItems(player);
				else
					c.grantItems(player);
				player.updateInventory();
				break;
			case EXPERIENCE:
				// tricky, but seemingly the only way to set total experience correctly.
				int currentXP = player.getTotalExperience();
				int newXP = getNewQuantity(currentXP, c.getQuantity(), 0, Integer.MAX_VALUE);
				player.setExperience(0);
				player.setTotalExperience(0);
				player.setLevel(0);
				player.setExperience(newXP);
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
	 * @return
	 */
	public static boolean playerCanAfford(Player player, List<Cost> costs) {
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