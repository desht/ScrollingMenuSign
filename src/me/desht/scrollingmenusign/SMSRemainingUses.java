package me.desht.scrollingmenusign;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class SMSRemainingUses {
	private static final String global = "&GLOBAL";
	private static final String perPlayer = "&PERPLAYER";
	
	private final Map<String,Integer> uses = new HashMap<String, Integer>();

	public SMSRemainingUses() {
	}
	
	SMSRemainingUses(Map<String, Integer> map) {
		for (Entry<String, Integer> e : map.entrySet()) {
			uses.put(e.getKey(), e.getValue());
		}
	}

	int getUses(String player) {
		if (uses.containsKey(global)) {
			return uses.get(global);
		} else if (uses.containsKey(perPlayer)) {
			return uses.containsKey(player) ?  uses.get(player) : uses.get(perPlayer);
		} else {
			return Integer.MAX_VALUE;
		}
	}
	
	void clearUses() {
		uses.clear();
	}
	
	void clearUses(String player) {
		uses.remove(player);
	}
	
	void setUses(int useCount) {
		uses.clear();
		uses.put(perPlayer, useCount);
	}
	
	void setGlobalUses(int useCount) {
		uses.clear();
		uses.put(global, useCount);
	}

	void use(String player) {
		if (uses.containsKey(global)) {
			decrementUses(global);
		} else {
			if (!uses.containsKey(player))
				uses.put(player, uses.get(perPlayer));
			decrementUses(player);
		}
	}
	
	@Override
	public String toString() {
		if (uses.containsKey(global)) {
			return "Uses remaining: " + uses.get(global) + " (global)";
		} else if (uses.containsKey(perPlayer)) {
			return "Uses: " + uses.get(perPlayer) + " (per-player)";
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
