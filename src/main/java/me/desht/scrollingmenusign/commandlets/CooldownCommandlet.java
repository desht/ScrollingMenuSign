package me.desht.scrollingmenusign.commandlets;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import me.desht.dhutils.Debugger;
import me.desht.dhutils.Duration;
import me.desht.dhutils.LogUtils;
import me.desht.scrollingmenusign.DirectoryStructure;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSValidate;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.enums.ReturnStatus;
import me.desht.scrollingmenusign.parser.CommandUtils;
import me.desht.scrollingmenusign.views.CommandTrigger;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.scheduler.BukkitTask;

import com.google.common.base.Joiner;

public class CooldownCommandlet extends BaseCommandlet implements Listener {
	private static final String COOLDOWNS_YML = "cooldowns.yml";

	private final Map<String, Long> cooldowns = new HashMap<String, Long>();
	private BukkitTask saveTask;

	public CooldownCommandlet() {
		super("COOLDOWN");

		load();
		Bukkit.getPluginManager().registerEvents(this, ScrollingMenuSign.getInstance());
	}

	@EventHandler
	public void onDisable(PluginDisableEvent event) {
		if (event.getPlugin() == ScrollingMenuSign.getInstance()) {
			save();
		}
	}

	@Override
	public boolean execute(ScrollingMenuSign plugin, CommandSender sender, CommandTrigger trigger, String cmd, String[] args) {
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
			CommandUtils.executeCommand(sender, command, trigger);
			updateCooldown(sender, cooldownName);
			ReturnStatus rs = CommandUtils.getLastReturnStatus();
			return rs == ReturnStatus.CMD_OK || rs == ReturnStatus.UNKNOWN;
		}
	}

	private void updateCooldown(CommandSender sender, String name) {
		String key = makeKey(name, sender.getName());
		cooldowns.put(key, System.currentTimeMillis());
		if (saveTask == null) {
			saveTask = Bukkit.getScheduler().runTaskLater(ScrollingMenuSign.getInstance(), new Runnable() {
				@Override
				public void run() {
					save();
				}
			}, 600L);
		}
	}

	private boolean isOnCooldown(CommandSender sender, String name, long delay) {
		String key = makeKey(name, sender.getName());
		if (!cooldowns.containsKey(key)) {
			cooldowns.put(key, 0L);
		}
		long last = cooldowns.get(key);
		Debugger.getInstance().debug("cooldown: " + key + "=" + last + ", delay = " + delay);
		return System.currentTimeMillis() - last < delay;
	}

	private String makeKey(String cooldownName, String player) {
		return cooldownName.toLowerCase().startsWith("global:") ? cooldownName : player + "," + cooldownName;
	}

	public void save() {
		File saveFile = new File(DirectoryStructure.getDataFolder(), COOLDOWNS_YML);
		YamlConfiguration conf = new YamlConfiguration();
		for (Entry<String, Long> e : cooldowns.entrySet()) {
			conf.set(e.getKey(), e.getValue());
		}
		try {
			Debugger.getInstance().debug("saving cooldown data");
			conf.save(saveFile);
			saveTask = null;
		} catch (IOException e) {
			LogUtils.warning("can't save " + saveFile + ": " + e.getMessage());
		}
	}

	public void load() {
		File saveFile = new File(DirectoryStructure.getDataFolder(), COOLDOWNS_YML);
		cooldowns.clear();
		YamlConfiguration conf = new YamlConfiguration();
		try {
			if (saveFile.exists()) {
				Debugger.getInstance().debug("loading cooldown data");
				conf.load(saveFile);
				for (String k : conf.getKeys(false)) {
					cooldowns.put(k, conf.getLong(k));
				}
			}
		} catch (Exception e) {
			LogUtils.warning("can't load " + saveFile + ": " + e.getMessage());
		}
	}
}
