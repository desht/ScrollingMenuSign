package me.desht.scrollingmenusign.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import me.desht.dhutils.MessagePager;
import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.ScrollingMenuSign;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

public class GetConfigCommand extends SMSAbstractCommand {

	public GetConfigCommand() {
		super("sms getcfg", 0, 1);
		setPermissionNode("scrollingmenusign.commands.getcfg");
		setUsage("/sms getcfg [<key>]");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		List<String> lines = getPluginConfiguration(args.length >= 1 ? args[0] : null);
		if (lines.size() > 1) {
			MessagePager pager = MessagePager.getPager(sender).clear().setParseColours(true);
			for (String line : lines) {
				pager.add(line);
			}
			pager.showPage();
		} else if (lines.size() == 1) {
			MiscUtil.statusMessage(sender, lines.get(0));
		}
		return true;
	}

	public List<String> getPluginConfiguration() {
		return getPluginConfiguration(null);
	}

	public List<String> getPluginConfiguration(String section) {
		ArrayList<String> res = new ArrayList<String>();
		Configuration config = ScrollingMenuSign.getInstance().getConfig();
		ConfigurationSection cs = config.getRoot();

		Set<String> items;
		if (section == null) {
			items = config.getDefaults().getKeys(true);
		} else {
			section = "sms." + section;
			if (config.getDefaults().isConfigurationSection(section)) {
				cs = config.getConfigurationSection(section);
				items = config.getDefaults().getConfigurationSection(section).getKeys(true);
			} else {
				items = new HashSet<String>();
				if (config.getDefaults().contains(section))
					items.add(section);
			}
		}

		for (String k : items) {
			if (cs.isConfigurationSection(k))
				continue;
			res.add("&f" + k.replaceAll("^sms\\.", "") + "&- = '&e" + cs.get(k) + "&-'");
		}
		Collections.sort(res);
		return res;
	}

	@Override
	public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
		switch (args.length) {
		case 1:
			return getConfigCompletions(sender, ScrollingMenuSign.getInstance().getConfig().getConfigurationSection("sms"), args[0]);
		default:
			return noCompletions(sender);
		}
	}
}
