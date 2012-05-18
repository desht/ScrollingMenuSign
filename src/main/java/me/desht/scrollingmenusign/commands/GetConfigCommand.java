package me.desht.scrollingmenusign.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.dhutils.MessagePager;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.commands.AbstractCommand;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.plugin.Plugin;

public class GetConfigCommand extends AbstractCommand {

	public GetConfigCommand() {
		super("sms ge", 0, 1);
		setPermissionNode("scrollingmenusign.commands.getcfg");
		setUsage("/sms getcfg [<key>]");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {	
		MessagePager pager = MessagePager.getPager(sender).clear();

		if (args.length == 0) {
			for (String line : getPluginConfiguration()) {
				pager.add(line);
			}
			pager.showPage();
		} else {
			String key = args[0];
			Object res = ((ScrollingMenuSign) plugin).getConfigManager().get(key);
			MiscUtil.statusMessage(sender, "&f" + key + " = '&e" + res + "&-'");
		}

		return true;
	}

	private static List<String> getPluginConfiguration() {
		ArrayList<String> res = new ArrayList<String>();
		Configuration config = ScrollingMenuSign.getInstance().getConfig();
		for (String k : config.getDefaults().getKeys(true)) {
			if (config.isConfigurationSection(k))
				continue;
			res.add("&f" + k.replaceAll("^sms\\.", "") + "&- = '&e" + config.get(k) + "&-'");
		}
		Collections.sort(res);
		return res;
	}
}
