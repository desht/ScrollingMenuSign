package me.desht.scrollingmenusign.spout;

import java.util.HashMap;
import java.util.Map;

import me.desht.dhutils.LogUtils;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.expector.ExpectCommandSubstitution;
import me.desht.scrollingmenusign.views.SMSSpoutView;
import me.desht.scrollingmenusign.views.SMSView;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.player.SpoutPlayer;

/**
 * @author desht
 */
public class SpoutUtils {
	private static final Map<String, SMSSpoutKeyMap> wantedKeys = new HashMap<String, SMSSpoutKeyMap>();

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
		
		LogUtils.fine("show spout text entry popup for " + player.getName() + ", prompt = " + prompt);
		
		// delaying this makes it play nicely with any spout view that might currently be showing
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(ScrollingMenuSign.getInstance(), new Runnable() {
			@Override
			public void run() {
				TextEntryPopup.show(sp, prompt);
			}
		}, 5L);
		return true;
	}

	public static void setupPasswordPrompt(Player player, String command, SMSView view) {
		SpoutPlayer sp = (SpoutPlayer)player;
		if (!sp.isSpoutCraftEnabled()) {
			throw new SMSException("Password prompting is only supported when using Spoutcraft.");
		}
		ScrollingMenuSign.getInstance().responseHandler.expect(player.getName(), new ExpectCommandSubstitution(command, view, true));
	}
}
