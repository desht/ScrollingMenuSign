package me.desht.scrollingmenusign;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;

public class SMSMenu {
	private String name;
	private String title;
	private String owner;
	private ArrayList<SMSMenuItem> items;
	Map<Location, Integer> locations;
	
	private static SMSMenuItem blankItem = new SMSMenuItem("", "", "");

	// Construct a new menu
	public SMSMenu(String n, String t, String o, Location l) {
		items = new ArrayList<SMSMenuItem>();
		name = n;
		title = t;
		owner = o;
		locations = new HashMap<Location, Integer>();
		if (l != null) locations.put(l, 0);
	}

	// Construct a new menu which is a copy of otherMenu
	public SMSMenu(SMSMenu other, String n, String o, Location l) {
		items = new ArrayList<SMSMenuItem>();
		name = n;
		title = other.getTitle();
		owner = o;
		locations = new HashMap<Location, Integer>();
		if (l != null) locations.put(l, 0);
		for (SMSMenuItem item: other.getItems()) {
			addItem(item.getLabel(), item.getCommand(), item.getMessage());
		}
	}

	public String getName() {
		return name;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String newTitle) {
		title = newTitle;
	}

	public Map<Location,Integer> getLocations() {
		return locations;
	}

	public String getOwner() {
		return owner;
	}

	public ArrayList<SMSMenuItem> getItems() {
		return items;
	}

	public int getNumItems() {
		return items.size();
	}

	public SMSMenuItem getCurrentItem(Location l) {
		if (items.size() == 0) {
			return null;
		}
		return items.get(locations.get(l));
	}
	
	// add a new sign to the menu
	public void addSign(Location l) {
		Block b = l.getBlock();
		if (b.getType() == Material.SIGN_POST || b.getType() == Material.WALL_SIGN) {
			locations.put(l, 0);
		}
	}
	
	// remove a sign from the menu
	public void removeSign(Location l) {
		Block b = l.getBlock();
		if (b.getType() == Material.SIGN_POST || b.getType() == Material.WALL_SIGN) {
			locations.remove(l);
		}
	}
	
	// add a new item to the menu
	public void addItem(String label, String command, String message) {
		SMSMenuItem item = new SMSMenuItem(label, command, message);
		items.add(item);
	}
	
	// remove an item from the menu
	public void removeItem(int index) {
		// Java is 0-indexed, our signs are 1-indexed
		items.remove(index - 1);
		for (Location l : locations.keySet()) {
			if (locations.get(l) >= items.size()) {
				locations.put(l, items.size() == 0 ? 0 : items.size() - 1);
			}
		}
	}

	// update the menu's signs according to the current menu state
	public void updateSigns() {
		for (Location l : locations.keySet()) {
			String[] lines = buildSignText(l);
			updateSign(l, lines);
		}
	}		
	// update a sign according to the current menu state
	public void updateSign(Location l) {
		updateSign(l, null);
	}	
	public void updateSign(Location l, String[] lines) {
		Sign sign = getSign(l);
		if (sign == null) return;
		if (lines == null) lines = buildSignText(l);
		for (int i = 0; i < 4; i++) {
			sign.setLine(i, lines[i]);
		}
		sign.update();
	}
	
	// blank the signs
	public void blankSigns() {
		for (Location l : locations.keySet()) {
			blankSign(l);
		}
	}
	// blank the sign at location l
	public void blankSign(Location l) {
		Sign sign = getSign(l);
		if (sign == null) return;
		for (int i = 0; i < 4; i++) {
			sign.setLine(i, "");
		}
		sign.update();
	}
	
	// build the text for the sign based on current menu contents
	private String[] buildSignText(Location l) {
		String[] res = new String[4];
		
		// first line of the sign in the menu title
		res[0] = title;
		
		// line 2-4 are the menu items around the current menu position
		// line 3 is the current position
		res[1] = String.format("  %1$-13s", getLine2Item(l).getLabel());
		res[2] = String.format("> %1$-13s", getLine3Item(l).getLabel());
		res[3] = String.format("  %1$-13s", getLine4Item(l).getLabel());
		
		return res;
	}

	// Get line 2 of the sign (item before the current item, or blank
	// if the menu has less than 3 items)
	private SMSMenuItem getLine2Item(Location l) {
		if (items.size() < 3) {
			return blankItem;	
		}
		int prev_pos = locations.get(l) - 1;
		if (prev_pos < 0) {
			prev_pos = items.size() - 1;
		}
		return items.get(prev_pos);
	}

	// Get line 3 of the sign (this is the currently selected item)
	private SMSMenuItem getLine3Item(Location l) {
		if (items.size() < 1) {
			return blankItem;
		}
		return items.get(locations.get(l));
	}
	
	// Get line 4 of the sign (item after the current item, or blank
	// if the menu has less than 2 items)
	private SMSMenuItem getLine4Item(Location l) {
		if (items.size() < 2) {
			return blankItem;
		}
		int next_pos = locations.get(l) + 1;
		if (next_pos >= items.size()) {
			next_pos = 0;
		}
		return items.get(next_pos);
	}
	
	// move to the next item in the menu
	public void nextItem(Location l) {
		int pos = locations.get(l) + 1;
		if (pos >= items.size()) pos = 0;
		locations.put(l, pos);
	}

	// move to the previous item in the menu
	public void prevItem(Location l) {
		if (items.size() == 0) return;
		int pos = locations.get(l) - 1;
		if (pos < 0) pos = items.size() - 1;
		locations.put(l, pos);
	}

	// return the sign at location l
	public Sign getSign(Location l) {
		if (locations.get(l) == null) {
			return null;
		}
		Block b = l.getBlock();
		if (b.getType() != Material.SIGN_POST && b.getType() != Material.WALL_SIGN) {
			return null;
		}
		return (Sign) b.getState();
	}

	// destroy all signs for this menu
	// you would normally only do this when about to delete the menu!
	public void destroySigns() {
		for (Location l: getLocations().keySet()) {
			destroySign(l);
		}
	}
	
	public void destroySign(Location l) {
		l.getBlock().setTypeId(0);
	}
}
