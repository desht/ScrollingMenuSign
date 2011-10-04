package me.desht.scrollingmenusign;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.packet.PacketItemName;
import org.getspout.spoutapi.player.SpoutPlayer;

public class SpoutUtils {
	public static void setSpoutMapName(short mapID, String name) {
		SpoutManager.getItemManager().setItemName(Material.MAP, mapID, name);
		for (Player p : Bukkit.getServer().getOnlinePlayers()) {
			SpoutPlayer sp = (SpoutPlayer)p;
			if (sp.isSpoutCraftEnabled()) {
				sp.sendPacket(new PacketItemName(Material.MAP.getId(), mapID, name));
			}
		}
	}
}
