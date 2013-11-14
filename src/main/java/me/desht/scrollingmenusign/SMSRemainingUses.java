package me.desht.scrollingmenusign;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;

public class SMSRemainingUses {
	private static final String GLOBAL_MAX = "&GLOBAL";
	private static final String GLOBAL_REMAINING = "&GLOBALREMAINING";
	private static final String PER_PLAYER_MAX = "&PERPLAYER";

	private final SMSUseLimitable attachedTo;
	private final Map<String, Integer> uses = new HashMap<String, Integer>();

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
	 * @param player The player name to check for
	 * @return True if there are limitations, false if not
	 * @deprecated use #hasLimitedUses()
	 */
	@Deprecated
	public boolean hasLimitedUses(String player) {
		return uses.containsKey(GLOBAL_MAX) || uses.containsKey(PER_PLAYER_MAX);
	}

	/**
	 * Check if this usage item limits uses.
	 *
	 * @return True if there are limitations, false if not
	 */
	public boolean hasLimitedUses() {
		return uses.containsKey(GLOBAL_MAX) || uses.containsKey(PER_PLAYER_MAX);
	}

	/**
	 * Return the remaining uses for the player.
	 *
	 * @param player The player name to check for
	 * @return The number of uses remaining (Integer.MAX_VALUE if there is no limit)
	 */
	public int getRemainingUses(String player) {
		if (uses.containsKey(GLOBAL_MAX)) {
			return uses.get(GLOBAL_REMAINING);
		} else if (uses.containsKey(PER_PLAYER_MAX)) {
			return uses.containsKey(player) ? uses.get(player) : uses.get(PER_PLAYER_MAX);
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
	 * @param player The player name to remove usage limits for
	 */
	public void clearUses(String player) {
		uses.remove(player);
		autosave();
	}

	/**
	 * Set the usage limits per player.  This is the total number of times an item/menu can be
	 * used by each player.
	 *
	 * @param useCount The usage limit
	 */
	public void setUses(int useCount) {
		uses.clear();
		uses.put(PER_PLAYER_MAX, useCount);
		autosave();
	}

	/**
	 * Set the global usage limit.  This is the total number of times an item/menu can be
	 * used by any player.
	 *
	 * @param useCount the usage count
	 */
	public void setGlobalUses(int useCount) {
		uses.clear();
		uses.put(GLOBAL_MAX, useCount);
		uses.put(GLOBAL_REMAINING, useCount);
		autosave();
	}

	/**
	 * Record a usage event against this item.
	 *
	 * @param player Name of the player who used the menu/item
	 */
	public void use(String player) {
		if (uses.containsKey(GLOBAL_MAX)) {
			decrementUses(GLOBAL_REMAINING);
		} else {
			if (!uses.containsKey(player))
				uses.put(player, uses.get(PER_PLAYER_MAX));
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
		if (uses.containsKey(GLOBAL_MAX)) {
			return String.format("%d/%d (global)", uses.get(GLOBAL_REMAINING), uses.get(GLOBAL_MAX));
		} else if (uses.containsKey(PER_PLAYER_MAX)) {
			return String.format("%d (per-player)", uses.get(PER_PLAYER_MAX));
		} else {
			return "";
		}
	}

	/**
	 * Return a formatted description of the total and remaining usage for the given player.
	 *
	 * @param player The player name
	 * @return Formatted string
	 */
	public String toString(String player) {
		if (uses.containsKey(GLOBAL_MAX)) {
			return String.format("%d/%d (global)", uses.get(GLOBAL_REMAINING), uses.get(GLOBAL_MAX));
		} else if (uses.containsKey(PER_PLAYER_MAX)) {
			return String.format("%d/%d (for %s)", getRemainingUses(player), uses.get(PER_PLAYER_MAX), player);
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
