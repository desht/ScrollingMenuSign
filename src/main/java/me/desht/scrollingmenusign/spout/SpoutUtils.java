package me.desht.scrollingmenusign.spout;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.getspout.spoutapi.SpoutManager;

import me.desht.scrollingmenusign.SMSConfig;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.util.MiscUtil;
import me.desht.scrollingmenusign.views.SMSSpoutView;
import me.desht.scrollingmenusign.views.SMSView;

public class SpoutUtils {
	private static final Map<String, SMSSpoutKeyMap> wantedKeys = new HashMap<String, SMSSpoutKeyMap>();

	public static void setSpoutMapName(short mapID, String name) {
		new SMSSpoutMapItem(mapID).setName(name);
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
		String wanted = SMSConfig.getConfig().getString(key);
		try {
			wantedKeys.put(key, new SMSSpoutKeyMap(wanted));
		} catch (IllegalArgumentException e) {
			MiscUtil.log(Level.WARNING, "invalid key definition [" + wanted + "] for " + key);
			wantedKeys.put(key, new SMSSpoutKeyMap(SMSConfig.getConfig().getDefaults().getString(key)));
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
}
