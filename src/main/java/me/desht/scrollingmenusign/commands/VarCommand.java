package me.desht.scrollingmenusign.commands;

import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.commands.AbstractCommand;
import me.desht.scrollingmenusign.SMSConfig;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.ScrollingMenuSign;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

public class VarCommand extends AbstractCommand {
	
	public VarCommand() {
		super("sms va", 0, 2);
		setPermissionNode("scrollingmenusign.commands.var");
		setUsage("/sms var <variable> <value>");
		setQuotedArgs(true);
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		notFromConsole(sender);
		
		Configuration cfg = SMSConfig.getConfig();
		
		if (args.length == 0) {
			// list all variables for the player
			ConfigurationSection cs = cfg.getConfigurationSection("uservar." + sender.getName());
			if (cs != null && !cs.getKeys(false).isEmpty()) {
				for (String key : cs.getKeys(false)) {
					MiscUtil.statusMessage(sender, "&f" + key + " = '&e" + cs.getString(key) + "&-'");	
				}
			} else {
				MiscUtil.statusMessage(sender, "You have no variables defined.");
			}
		} else if (args.length == 1) {
			// show the given variable for the player
			String key = "uservar." + sender.getName() + "." + args[0];
			if (cfg.contains(key)) {
				MiscUtil.statusMessage(sender, "&f" + args[0] + " = '&e" + cfg.get(key) + "&-'");
			} else {
				throw new SMSException("No such variable '" + args[0] + "'");
			}
		} else {
			// set or delete the given variable for the player
			String key = "uservar." + sender.getName() + "." + args[0];
			if (args[1].startsWith("-d")) {
				cfg.set(key, null);
				MiscUtil.statusMessage(sender, "Variable &f" + args[0] + "&- deleted.");
			} else {
				cfg.set(key, args[1]);
				MiscUtil.statusMessage(sender, "&f" + args[0] + " = '&e" + cfg.get(key) + "&-'");
			}
			
			ScrollingMenuSign.getInstance().saveConfig();
		}
		
		return true;
	}

}
