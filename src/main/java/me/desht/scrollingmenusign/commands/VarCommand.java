package me.desht.scrollingmenusign.commands;

import me.desht.scrollingmenusign.SMSConfig;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.util.MiscUtil;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class VarCommand extends AbstractCommand {

	public VarCommand() {
		super("sms va", 0, 2);
		setPermissionNode("scrollingmenusign.commands.var");
		setUsage("/sms var <variable> <value>");
		setQuotedArgs(true);
	}

	@Override
	public boolean execute(ScrollingMenuSign plugin, Player player, String[] args) throws SMSException {
		notFromConsole(player);
		
		Configuration cfg = SMSConfig.getConfig();
		
		if (args.length == 0) {
			ConfigurationSection cs = cfg.getConfigurationSection("uservar." + player.getName());
			if (cs != null && !cs.getKeys(false).isEmpty()) {
				for (String key : cs.getKeys(false)) {
					MiscUtil.statusMessage(player, "&f" + key + " = '&e" + cs.getString(key) + "&-'");	
				}
			} else {
				MiscUtil.statusMessage(player, "You have no variables defined.");
			}
		} else if (args.length == 1) {
			String key = "uservar." + player.getName() + "." + args[0];
			if (cfg.contains(key)) {
				MiscUtil.statusMessage(player, "&f" + args[0] + " = '&e" + cfg.get(key) + "&-'");
			} else {
				throw new SMSException("No such variable '" + args[0] + "'");
			}
		} else {
			String key = "uservar." + player.getName() + "." + args[0];
			if (args[1].startsWith("-d")) {
				cfg.set(key, null);
				MiscUtil.statusMessage(player, "Variable &f" + args[0] + "&- deleted.");
			} else {
				cfg.set(key, args[1]);
				MiscUtil.statusMessage(player, "&f" + args[0] + " = '&e" + cfg.get(key) + "&-'");
			}
			
			ScrollingMenuSign.getInstance().saveConfig();
		}
		
		return true;
	}

}
