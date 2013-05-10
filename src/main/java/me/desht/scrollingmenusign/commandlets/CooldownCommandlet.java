package me.desht.scrollingmenusign.commandlets;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import me.desht.dhutils.Duration;
import me.desht.dhutils.LogUtils;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSValidate;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.parser.CommandUtils;
import me.desht.scrollingmenusign.views.SMSView;

import org.bukkit.command.CommandSender;

import com.google.common.base.Joiner;

public class CooldownCommandlet extends BaseCommandlet {

	private static final String GLOBAL_PLAYER = "[GLOBAL]";

	private final Map<String,Long> cooldowns = new HashMap<String, Long>();

	public CooldownCommandlet() {
		super("COOLDOWN");
	}

	@Override
	public boolean execute(ScrollingMenuSign plugin, CommandSender sender, SMSView view, String cmd, String[] args) {
		SMSValidate.isTrue(args.length >= 4, "Usage: " + cmd + " <cooldown-name> <delay> <command string>");

		String cooldownName = args[1];

		Duration delay;
		try {
			delay = new Duration(args[2]);
		} catch (IllegalArgumentException e) {
			throw new SMSException("Invalid duration: " + args[2]);
		}

		final String command = Joiner.on(" ").join(Arrays.copyOfRange(args, 3, args.length));

		if (isOnCooldown(sender, cooldownName, delay.getTotalDuration())) {
			return false;
		} else {
			CommandUtils.executeCommand(sender, command, view);
			updateCooldown(sender, cooldownName);
			return true;
		}
	}

	private void updateCooldown(CommandSender sender, String name) {
		String key = makeKey(name, sender.getName());
		cooldowns.put(key, System.currentTimeMillis());
	}

	private boolean isOnCooldown(CommandSender sender, String name, long delay) {
		String key = makeKey(name, sender.getName());
		if (!cooldowns.containsKey(key)) {
			cooldowns.put(key, 0L);
		}
		long last = cooldowns.get(key);
		LogUtils.fine("cooldown: " + key + "=" + last + ", delay = " + delay);
		return System.currentTimeMillis() - last < delay;
	}

	private String makeKey(String cooldownName, String player) {
		if (cooldownName.toLowerCase().startsWith("global:")) {
			player = GLOBAL_PLAYER;
		}
		return player + "|"	+ cooldownName;
	}
}