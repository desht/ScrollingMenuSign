package me.desht.scrollingmenusign.parser;

import me.desht.scrollingmenusign.enums.CostType;

public class Cost {
	private CostType type;
	private int id;
	private Byte data;
	private double quantity;

	Cost(int id) {
		this(id, null, 1);
	}

	Cost(int id, Byte data, double quantity) {
		this(id == 0 ? CostType.MONEY : CostType.ITEM, id, data, quantity);
	}

	Cost(CostType type, int id, Byte data, double quantity) {
		this.type = type;
		this.id = id;
		this.data = data;
		this.quantity = quantity;
	}

	Cost(String costSpec) {
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
}