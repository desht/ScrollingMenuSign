package me.desht.scrollingmenusign.commands;

import java.util.ArrayList;
import java.util.List;

import me.desht.scrollingmenusign.SMSConfig;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.util.MiscUtil;

import org.bukkit.entity.Player;

public class SetConfigCommand extends AbstractCommand {

	public SetConfigCommand() {
		super("sms se", 2);
		setPermissionNode("scrollingmenusign.commands.setcfg");
		setUsage("/sms setcfg <key> <value>");
		setQuotedArgs(true);
	}

	@Override
	public boolean execute(ScrollingMenuSign plugin, Player player, String[] args) throws SMSException {
		String key = args[0], val = args[1];
		
		try {
			if (args.length > 2) {
				List<String> list = new ArrayList<String>(args.length - 1);
				for (int i = 1; i < args.length; i++)
					list.add(args[i]);
				SMSConfig.setPluginConfiguration(key, list);
			} else {
				SMSConfig.setPluginConfiguration(key, val);
			}
			MiscUtil.statusMessage(player, key + " is now set to '&e" + SMSConfig.getPluginConfiguration(key) + "&-'");
		} catch (SMSException e) {
			MiscUtil.errorMessage(player, e.getMessage());
			MiscUtil.errorMessage(player, "Use /sms getcfg to list all valid keys");
		} catch (IllegalArgumentException e) {
			MiscUtil.errorMessage(player, e.getMessage());
		}
		return true;
	}

}
