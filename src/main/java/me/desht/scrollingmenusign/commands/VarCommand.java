package me.desht.scrollingmenusign.commands;

import me.desht.dhutils.MessagePager;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PermissionUtils;
import me.desht.dhutils.commands.AbstractCommand;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSVariables;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class VarCommand extends AbstractCommand {
	
	private String targetPlayer;
	private String targetVar;

	public VarCommand() {
		super("sms va", 1, 2);
		setPermissionNode("scrollingmenusign.commands.var");
		setUsage(new String[] {
				"/sms var -l",
				"/sms var -l <player>",
				"/sms var <variable> [<value>]",
				"/sms var <player>.<variable> [<value>]",
		});
		setQuotedArgs(true);
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		notFromConsole(sender);
		
		if (args[0].startsWith("-l")) {
			// list all variables for the player
			
			String playerName = args.length > 1 ? args[1] : sender.getName();
			SMSVariables vars = SMSVariables.getVariables(playerName);
			
			if (vars.getVariables().isEmpty()) {
				MiscUtil.statusMessage(sender, "Player &e" + playerName + "&- has no variables defined.");
			} else {
				MessagePager pager = MessagePager.getPager(sender).clear();
				for (String key : vars.getVariables()) {
					pager.add("&f" + key + " = '&e" + vars.get(key) + "&-'");	
				}
				pager.showPage();
			}
		} else if (args.length == 1) {
			// show the given variable for the player
			parseVarSpec(sender, args[0]);
			SMSVariables vars = SMSVariables.getVariables(targetPlayer);
			
			if (vars.isSet(targetVar)) {
				MiscUtil.statusMessage(sender, "&f" + args[0] + " = '&e" + vars.get(targetVar) + "&-'");
			} else {
				throw new SMSException("No such variable '" + args[0] + "' for player " + targetPlayer);
			}
		} else if (args.length == 2) {
			// set or delete the given variable for the player
			if (args[0].startsWith("-d")) {
				parseVarSpec(sender, args[1]);
				SMSVariables vars = SMSVariables.getVariables(targetPlayer);
				vars.set(targetVar, null);
				MiscUtil.statusMessage(sender, "Variable &f" + args[1] + "&- deleted.");
			} else {
				parseVarSpec(sender, args[0]);
				SMSVariables vars = SMSVariables.getVariables(targetPlayer);
				vars.set(targetVar, args[1]);
				MiscUtil.statusMessage(sender, "&f" + args[0] + " = '&e" + vars.get(targetVar) + "&-'");
			}
		}
		
		return true;
	}

	private void parseVarSpec(CommandSender sender, String spec) {
		String[] parts = spec.split("\\.", 2);
		if (parts.length == 1) {
			targetPlayer = sender.getName();
			targetVar = parts[0];
		} else if (parts.length > 1) {
			targetPlayer = parts[0];
			targetVar = parts[1];
		} else {
			throw new IllegalArgumentException("bad variable spec " + spec);
		}
		if (!targetPlayer.equalsIgnoreCase(sender.getName())) {
			PermissionUtils.requirePerms(sender, "scrollingmenusign.vars.other");
		}
	}
}
