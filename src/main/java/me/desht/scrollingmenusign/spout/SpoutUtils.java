package me.desht.scrollingmenusign.spout;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.material.Material;
import org.getspout.spoutapi.material.MaterialData;
import org.getspout.spoutapi.player.SpoutPlayer;

import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.dhutils.LogUtils;
import me.desht.scrollingmenusign.views.SMSSpoutView;
import me.desht.scrollingmenusign.views.SMSView;

/**
 * @author des
 *
 */
public class SpoutUtils {
	private static final Map<String, SMSSpoutKeyMap> wantedKeys = new HashMap<String, SMSSpoutKeyMap>();

	public static void setSpoutMapName(short mapID, String name) {
//		new SMSSpoutMapItem(mapID).setName(name);
		Material mat;
		try {
			mat = MaterialData.getOrCreateMaterial(358, mapID);
		} catch (Exception e) {
			LogUtils.warning("Spout problem: MaterialData.getOrCreateMaterial(358," + mapID +") threw Exception: " + e);
			e.printStackTrace();
			mat = null;
		}
		if (mat == null) {
			LogUtils.warning("can't get Spout material for 358:" + mapID + " (item tooltip will be missing)");
		} else {
			mat.setName(name);
		}
	}

	public static void loadKeyDefinitions() {
		addKeyDefinition("sms.actions.spout.up");
		addKeyDefinition("sms.actions.spout.down");
		addKeyDefinition("sms.actions.spout.execute");
	}

	public static boolean tryKeyboardMatch(String key, SMSSpoutKeyMap pressed) {
		return wantedKeys.get(key).equals(pressed);
	}

	private static void addKeyDefinition(String key) {
		String wanted = ScrollingMenuSign.getInstance().getConfig().getString(key);
		try {
			wantedKeys.put(key, new SMSSpoutKeyMap(wanted));
		} catch (IllegalArgumentException e) {
			LogUtils.warning("invalid key definition [" + wanted + "] for " + key);
			wantedKeys.put(key, new SMSSpoutKeyMap(ScrollingMenuSign.getInstance().getConfig().getDefaults().getString(key)));
		}
	}

	/**
	 * Ensure that the textures for all Spout views are added to the pre-login cache.
	 * This must be called from onEnable(), after views have been loaded.
	 */
	public static void precacheTextures() {
		for (SMSView v : SMSView.listViews()) {
			if (v instanceof SMSSpoutView) {
				String url = v.getAttributeAsString(SMSSpoutView.TEXTURE);
				if (!url.isEmpty()) {
					SpoutManager.getFileManager().addToPreLoginCache(ScrollingMenuSign.getInstance(), url);
				}
			}
		}
	}

	public static boolean showTextEntryPopup(final Player player, final String prompt) {
		final SpoutPlayer sp = (SpoutPlayer)player;
		if (!sp.isSpoutCraftEnabled()) {
			return false;
		}
		
		// delaying this makes it play nicely with any spout view that might currently be showing
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(ScrollingMenuSign.getInstance(), new Runnable() {
			@Override
			public void run() {
				TextEntryPopup.show(sp, prompt);
			}
		});
		return true;
	}
}
