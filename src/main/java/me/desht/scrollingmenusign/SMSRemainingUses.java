package me.desht.scrollingmenusign;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;

public class SMSRemainingUses {
	private static final String globalMax = "&GLOBAL";
	private static final String global = "&GLOBALREMAINING";
	private static final String perPlayerMax = "&PERPLAYER";

	private final SMSUseLimitable attachedTo;
	private final Map<String,Integer> uses = new HashMap<String, Integer>();

	SMSRemainingUses(SMSUseLimitable lim) {
		this.attachedTo = lim;
	}

	SMSRemainingUses(SMSUseLimitable lim, ConfigurationSection node) {
		this.attachedTo = lim;
		if (node == null)
			return;
		for (String key : node.getKeys(false)) {
			uses.put(key, node.getInt(key, 0));
		}
	}

	/**
	 * Check if this usage item limits uses.
	 * 
	 * @param player	The player name to check for
	 * @return			True if there are limitations, false if not
	 */
	public boolean hasLimitedUses(String player) {
		return uses.containsKey(globalMax) || uses.containsKey(perPlayerMax);
	}

	/**
	 * Return the remaining uses for the player.
	 * 
	 * @param player	The player name to check for
	 * @return			The number of uses remaining (Integer.MAX_VALUE if there is no limit)
	 */
	public int getRemainingUses(String player) {
		if (uses.containsKey(globalMax)) {
			return uses.get(global);
		} else if (uses.containsKey(perPlayerMax)) {
			return uses.containsKey(player) ? uses.get(player) : uses.get(perPlayerMax);
		} else {
			return Integer.MAX_VALUE;
		}
	}

	/**
	 * Clear all usage limits for this item, for all players.
	 */
	public void clearUses() {
		uses.clear();
		autosave();
	}

	/**
	 * Clear usage limits for the given player.
	 * 
	 * @param player	The player name to remove usage limits for
	 */
	public void clearUses(String player) {
		uses.remove(player);
		autosave();
	}

	/**
	 * Set the usage limits per player.  This is the total number of times an item/menu can be
	 * used by each player.
	 * 
	 * @param useCount	The usage limit
	 */
	public void setUses(int useCount) {
		uses.clear();
		uses.put(perPlayerMax, useCount);
		autosave();
	}

	/**
	 * Set the global usage limit.  This is the total number of times an item/menu can be
	 * used by any player.
	 * 
	 * @param useCount
	 */
	public void setGlobalUses(int useCount) {
		uses.clear();
		uses.put(globalMax, useCount);
		uses.put(global, useCount);
		autosave();
	}

	/**
	 * Record a usage event against this item.
	 * 
	 * @param player	Name of the player who used the menu/item
	 */
	public void use(String player) {
		if (uses.containsKey(globalMax)) {
			decrementUses(global);
		} else {
			if (!uses.containsKey(player))
				uses.put(player, uses.get(perPlayerMax));
			decrementUses(player);
		}
		autosave();
	}

	private void autosave() {
		attachedTo.autosave();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		if (uses.containsKey(globalMax)) {
			return String.format("%d/%d (global)", uses.get(global), uses.get(globalMax));
		} else if (uses.containsKey(perPlayerMax)) {
			return String.format("%d (per-player)", uses.get(perPlayerMax));
		} else {
			return "";
		}
	}

	/**
	 * Return a formatted description of the total and remaining usage for the given player.
	 * 
	 * @param player	The player name
	 * @return			Formatted string
	 */
	public String toString(String player) {
		if (uses.containsKey(globalMax)) {
			return String.format("%d/%d (global)", uses.get(global), uses.get(globalMax));
		} else if (uses.containsKey(perPlayerMax)) {
			return String.format("%d/%d (per-player)", getRemainingUses(player), uses.get(perPlayerMax));
		} else {
			return "";
		}
	}

	private void decrementUses(String who) {
		uses.put(who, uses.get(who) - 1);
	}

	Map<String, Integer> freeze() {
		return uses;
	}

	String getDescription() {
		return attachedTo.getDescription();
	}
}
