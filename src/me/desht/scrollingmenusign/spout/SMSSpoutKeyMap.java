package me.desht.scrollingmenusign.spout;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.getspout.spoutapi.keyboard.Keyboard;

import com.google.common.base.Joiner;

public class SMSSpoutKeyMap implements ConfigurationSerializable {
	private Set<Keyboard> keys;

	public SMSSpoutKeyMap(String definition) {
		keys = new HashSet<Keyboard>();
		
		if (definition == null || definition.isEmpty()) {
			return;
		}
		String[] wanted = definition.split("\\+");
		for (String w : wanted) {
			w = w.toUpperCase();
			if (!w.startsWith("KEY_"))
				w = "KEY_" + w;
			keys.add(Keyboard.valueOf(w));
		}
	}
	
	public SMSSpoutKeyMap() {
		this(null);
	}

	public void add(Keyboard key) {
		keys.add(key);
	}
	
	public void remove(Keyboard key) {
		keys.remove(key);
	}
	
	public void clear() {
		keys.clear();
	}
	
	@Override
	public String toString() {
		return Joiner.on("+").join(keys);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((keys == null) ? 0 : keys.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SMSSpoutKeyMap other = (SMSSpoutKeyMap) obj;
		if (keys == null) {
			if (other.keys != null)
				return false;
		} else if (!keys.equals(other.keys))
			return false;
		return true;
	}

	@Override
	public Map<String, Object> serialize() {
		Map<String,Object> map = new HashMap<String, Object>();
		map.put("keymap", this.toString());
		return map;
	}
	
	public static SMSSpoutKeyMap deserialize(Map<String,Object> map) {
		return new SMSSpoutKeyMap((String) map.get("keymap"));
	}
}
