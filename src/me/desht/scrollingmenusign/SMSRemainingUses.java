package me.desht.scrollingmenusign;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class SMSRemainingUses {
	private static final String globalMax = "&GLOBAL";
	private static final String global = "&GLOBALREMAINING";
	private static final String perPlayerMax = "&PERPLAYER";
	
	private SMSMenuItem item;
	private final Map<String,Integer> uses = new HashMap<String, Integer>();

	SMSRemainingUses(SMSMenuItem item) {
		this.item = item;
	}
	
	SMSRemainingUses(SMSMenuItem item, Map<String, Integer> map) {
		this.item = item;
		for (Entry<String, Integer> e : map.entrySet()) {
			uses.put(e.getKey(), e.getValue());
		}
	}

	boolean hasLimitedUses(String player) {
		return uses.containsKey(globalMax) || uses.containsKey(perPlayerMax);
	}
	
	int getRemainingUses(String player) {
		if (uses.containsKey(globalMax)) {
			return uses.get(global);
		} else if (uses.containsKey(perPlayerMax)) {
			return uses.containsKey(player) ?  uses.get(player) : uses.get(perPlayerMax);
		} else {
			return Integer.MAX_VALUE;
		}
	}
	
	void clearUses() {
		uses.clear();
		item.autosave();
	}
	
	void clearUses(String player) {
		uses.remove(player);
		item.autosave();
	}
	
	void setUses(int useCount) {
		uses.clear();
		uses.put(perPlayerMax, useCount);
		item.autosave();
	}
	
	void setGlobalUses(int useCount) {
		uses.clear();
		uses.put(globalMax, useCount);
		uses.put(global, useCount);
		item.autosave();
	}

	void use(String player) {
		if (uses.containsKey(globalMax)) {
			decrementUses(global);
		} else {
			if (!uses.containsKey(player))
				uses.put(player, uses.get(perPlayerMax));
			decrementUses(player);
		}
		item.autosave();
	}
	
	@Override
	public String toString() {
		if (uses.containsKey(globalMax)) {
			return String.format("Uses remaining %d/%d (global)", uses.get(global), uses.get(globalMax));
		} else if (uses.containsKey(perPlayerMax)) {
			return String.format("Uses: %d (per-player)", uses.get(perPlayerMax));
		} else {
			return "";
		}
	}
	
	public String toString(String player) {
		if (uses.containsKey(globalMax)) {
			return String.format("Uses remaining %d/%d (global)", uses.get(global), uses.get(globalMax));
		} else if (uses.containsKey(perPlayerMax)) {
			return String.format("Uses: %d/%d (per-player)", getRemainingUses(player), uses.get(perPlayerMax));
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
}
