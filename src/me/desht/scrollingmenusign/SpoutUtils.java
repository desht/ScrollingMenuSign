package me.desht.scrollingmenusign;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

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
		addKeyDefinition("sms.actions.spout.up", "key_up");
		addKeyDefinition("sms.actions.spout.down", "key_down");
		addKeyDefinition("sms.actions.spout.execute", "key_return");
	}
	
	public static boolean tryKeyboardMatch(String key, Set<Keyboard> pressed) {
		return wantedKeys.get(key).equals(pressed);
	}
	
	private static void addKeyDefinition(String key, String def) {
		String[] wanted = SMSConfig.getConfiguration().getString(key, def).split("\\+");
		Set<Keyboard> result = new HashSet<Keyboard>();
		for (String w : wanted) {
			if (!w.startsWith("key_"))
				w = "key_" + w;
			try {
				result.add(Keyboard.valueOf(w.toUpperCase()));
			} catch (IllegalArgumentException e) {
				MiscUtil.log(Level.WARNING, "Unknown Spout key definition " + w + " in " + key);
			}
		}
		wantedKeys.put(key, result);
	}
}
