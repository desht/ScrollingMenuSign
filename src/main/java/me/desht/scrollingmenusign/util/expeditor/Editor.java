package me.desht.scrollingmenusign.util.expeditor;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;


/**
 * Thanks to feildmaster for the original implementation.
 * Thanks to Southpaw018 for the workaround for the enchant/die xp loss problem.
 * 
 * This class integrates the above two concepts.
 *
 */

public class Editor implements Listener {
	private final Player player;
	private static final Set<String> recalcNeeded = new HashSet<String>();
	
	public Editor(Plugin plugin, Player p) {
		player = p;

		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	// Handle experience
	public void setExp(int exp) {
		player.setExp(0);
		player.setLevel(0);
		player.setTotalExperience(0);

		if(exp <= 0) return;

		giveExp(exp);
	}

	public void giveExp(int exp) {
		if (isRecalcNeeded()) {
			recalcTotalExp();
		}
		while(exp > 0) {
			int xp = getExpToLevel()-getExp();
			if(xp > exp)
				xp = exp;
			player.giveExp(xp);
			exp -= xp;
		}
	}

	public boolean isRecalcNeeded() {
		return recalcNeeded.contains(player.getName());
	}

	public void takeExp(int exp) {
		takeExp(exp, true);
	}

	public void takeExp(int exp, boolean fromTotal) {
		if (isRecalcNeeded()) {
			recalcTotalExp();
		}
		
		int xp = getTotalExp();

		if (fromTotal) {
			xp -= exp;
		} else {
			int m = getExp() - exp;
			if(m < 0) m = 0;
			xp -= getExp() + m;
		}

		setExp(xp);
	}

	// Get experience functions
	public int getExp() {
		return (int) (getExpToLevel() * player.getExp());
	}

	// This function is ugly!
	public int getTotalExp() {
		return getTotalExp(false);
	}
	public int getTotalExp(boolean recalc) {
		if (recalc) recalcTotalExp();
		return player.getTotalExperience();
	}

	public int getLevel() {
		return player.getLevel();
	}

	public int getExpToLevel() {
		return getExpToLevel(getLevel());
	}

	public int getExpToLevel(int i) {
		return 7 + (i * 7 >> 1);
	}

	public void recalcTotalExp() {
		int total = getExp();
		for(int i = 0; i < player.getLevel(); i++) {
			total += getExpToLevel(i);
		}
		player.setTotalExperience(total);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerDeath(PlayerDeathEvent event) {
		if (!(event.getEntity() instanceof Player))
			return;
		Player p = (Player)event.getEntity();
		recalcNeeded.remove(p.getName());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onEnchantItem(EnchantItemEvent event) {
		if (event.isCancelled()) return;
		recalcNeeded.add(event.getEnchanter().getName());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event) {
		recalcNeeded.remove(event.getPlayer().getName());
	}
	
}
