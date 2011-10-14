package me.desht.scrollingmenusign.spout;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import me.desht.scrollingmenusign.SMSConfig;
import me.desht.util.MiscUtil;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.keyboard.Keyboard;
import org.getspout.spoutapi.packet.PacketItemName;
import org.getspout.spoutapi.player.SpoutPlayer;

public class SpoutUtils {
	private static final Map<String, Set<Keyboard>> wantedKeys = new HashMap<String, Set<Keyboard>>();

	public static void setSpoutMapName(short mapID, String name) {
		SpoutManager.getItemManager().setItemName(Material.MAP, mapID, name);
		for (Player p : Bukkit.getServer().getOnlinePlayers()) {
			SpoutPlayer sp = (SpoutPlayer)p;
			if (sp.isSpoutCraftEnabled()) {
				sp.sendPacket(new PacketItemName(Material.MAP.getId(), mapID, name));
			}
		}
	}

	public static void loadKeyDefinitions() {
		addKeyDefinition("sms.actions.spout.up");
		addKeyDefinition("sms.actions.spout.down");
		addKeyDefinition("sms.actions.spout.execute");
	}

	public static boolean tryKeyboardMatch(String key, Set<Keyboard> pressed) {
		return wantedKeys.get(key).equals(pressed);
	}

	public static Set<Keyboard> parseKeyDefinition(String definition) {
		Set<Keyboard> result = new HashSet<Keyboard>();
		if (definition == null || definition.isEmpty()) {
			return result;
		}
		String[] wanted = definition.split("\\+");
		for (String w : wanted) {
			w = w.toUpperCase();
			if (!w.startsWith("KEY_"))
				w = "KEY_" + w;
			result.add(Keyboard.valueOf(w));
		}
		return result;
	}

	private static void addKeyDefinition(String key) {
		String wanted = SMSConfig.getConfig().getString(key);
		try {
			wantedKeys.put(key, parseKeyDefinition(wanted));
		} catch (IllegalArgumentException e) {
			MiscUtil.log(Level.WARNING, "invalid key definition [" + wanted + "] for " + key);
			wantedKeys.put(key, parseKeyDefinition(SMSConfig.getConfig().getDefaults().getString(key)));
		}
	}
}
