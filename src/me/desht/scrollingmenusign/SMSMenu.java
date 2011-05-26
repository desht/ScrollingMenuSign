package me.desht.scrollingmenusign;

import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;

public class SMSMenu {
	private String name;
	private String title;
	private String owner;
	private ArrayList<SMSMenuItem> items;
	private Location loc;
	private int curPos;
	
	private static SMSMenuItem blankItem = new SMSMenuItem("", "", "");

	// Construct a new menu
	public SMSMenu(String n, String t, String o, Location l) {
		items = new ArrayList<SMSMenuItem>();
		name = n;
		title = t;
		owner = o;
		loc = l;
		curPos = 0;
	}

	// Construct a new menu which is a copy of otherMenu
	public SMSMenu(SMSMenu other, String n, String o, Location l) {
		items = new ArrayList<SMSMenuItem>();
		name = n;
		title = other.getTitle();
		owner = o;
		loc = l;
		curPos = 0;
		for (SMSMenuItem item: other.getItems()) {
			add(item.getLabel(), item.getCommand(), item.getMessage());
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
	
	public Location getLocation() {
		return loc;
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

	public SMSMenuItem getCurrentItem() {
		if (items.size() == 0) {
			return null;
		}
		return items.get(curPos);
	}
	
	// add a new item to the menu
	public void add(String label, String command, String message) {
		SMSMenuItem item = new SMSMenuItem(label, command, message);
		items.add(item);
	}
	
	// remove an item from the menu
	public void remove(int index) {
		// Java is 0-indexed, our signs are 1-indexed
		items.remove(index - 1);
		if (curPos >= items.size()) {
			curPos = items.size() - 1;
		}
	}
	
	// update the menu's sign according to the current menu state
	public void updateSign() {
		Sign sign = getSign();
		if (sign == null) return;
		String[] lines = buildSignText();
		for (int i = 0; i < 4; i++) {
			sign.setLine(i, lines[i]);
		}
		sign.update();
	}
	
	// blank the sign
	public void blankSign() {
		Sign sign = getSign();
		if (sign == null) return;
		for (int i = 0; i < 4; i++) {
			sign.setLine(i, "");
		}
		sign.update();
	}
	
	// build the text for the sign based on current menu contents
	private String[] buildSignText() {
		String[] res = new String[4];
		
		// first line of the sign in the menu title
		res[0] = title;
		
		// line 2-4 are the menu items around the current menu position
		// line 3 is the current position
		res[1] = String.format("  %1$-13s", getLine2Item().getLabel());
		res[2] = String.format("> %1$-13s", getLine3Item().getLabel());
		res[3] = String.format("  %1$-13s", getLine4Item().getLabel());
		
		return res;
	}

	// Get line 2 of the sign (item before the current item, or blank
	// if the menu has less than 3 items)
	private SMSMenuItem getLine2Item() {
		if (items.size() < 3) {
			return blankItem;	
		}
		int prev_pos = curPos - 1;
		if (prev_pos < 0) {
			prev_pos = items.size() - 1;
		}
		return items.get(prev_pos);
	}

	// Get line 3 of the sign (this is the currently selected item)
	private SMSMenuItem getLine3Item() {
		if (items.size() < 1) {
			return blankItem;
		}
		return items.get(curPos);
	}
	
	// Get line 4 of the sign (item after the current item, or blank
	// if the menu has less than 2 items)
	private SMSMenuItem getLine4Item() {
		if (items.size() < 2) {
			return blankItem;
		}
		int next_pos = curPos + 1;
		if (next_pos >= items.size()) {
			next_pos = 0;
		}
		return items.get(next_pos);
	}
	
	// move to the next item in the menu
	public void nextItem() {
		curPos++;
		if (curPos >= items.size()) {
			curPos = 0;
		}
	}

	// move to the previous item in the menu
	public void prevItem() {
		if (items.size() == 0) return;
		curPos--;
		if (curPos < 0) {
			curPos = items.size() - 1;
		}
	}

	// get the sign at the menu's location, if any
	public Sign getSign() {
		Block b = loc.getBlock();
		if (b.getType() != Material.SIGN_POST && b.getType() != Material.WALL_SIGN) {
			// This should never happen - a menu can't be created without a
			// sign, and the block break event handler should ensure the menu is
			// removed if the sign is destroyed.
			return null;
		}
		return (Sign) b.getState();
	}

}
