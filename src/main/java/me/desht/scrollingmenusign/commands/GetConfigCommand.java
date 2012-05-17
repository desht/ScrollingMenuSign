package me.desht.scrollingmenusign.commands;

import me.desht.scrollingmenusign.SMSConfig;
import me.desht.dhutils.MessagePager;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.commands.AbstractCommand;

import org.bukkit.command.CommandSender;
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
			for (String line : SMSConfig.getPluginConfiguration()) {
				pager.add(line);
			}
			pager.showPage();
		} else {
			String key = args[0];
			Object res = SMSConfig.getPluginConfiguration(key);
			MiscUtil.statusMessage(sender, "&f" + key + " = '&e" + res + "&-'");
		}
		
		return true;
	}

}
