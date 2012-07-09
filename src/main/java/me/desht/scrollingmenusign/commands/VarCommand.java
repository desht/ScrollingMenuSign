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
		super("sms va", 1, 4);
		setPermissionNode("scrollingmenusign.commands.var");
		setUsage(new String[] {
				"/sms var -l [<player>]",
				"/sms var [-q] [<player>.]<variable> [<value>]",
				"/sms var [-q] -d [<player>.]<variable>",
				"/sms var [-q] -i [<player>.]<variable> [<amount>]",
		});
		setQuotedArgs(true);
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {

		if (args[0].startsWith("-l")) {
			// list all variables for the player

			if (args.length == 1) notFromConsole(sender);

			String playerName = args.length > 1 ? args[1] : sender.getName();

			SMSVariables vars = SMSVariables.getVariables(playerName, false);
			MessagePager pager = MessagePager.getPager(sender).clear();
			for (String key : vars.getVariables()) {
				pager.add(key + " = '&e" + vars.get(key) + "&-'");	
			}
			pager.showPage();

		} else if (args.length == 1) {
			// show the given variable for the player
			parseVarSpec(sender, args[0]);
			SMSVariables vars = SMSVariables.getVariables(targetPlayer, false);

			if (vars.isSet(targetVar)) {
				MiscUtil.statusMessage(sender, "&f" + args[0] + " = '&e" + vars.get(targetVar) + "&-'");
			} else {
				throw new SMSException("No such variable '" + args[0] + "' for player " + targetPlayer);
			}
		} else if (args.length >= 2) {

			int n = 0;
			boolean quiet = false;
			if (args[0].startsWith("-q")) {
				quiet = true;
				n = 1;
			}

			// set or delete the given variable for the player
			if (args[n].startsWith("-d")) {
				parseVarSpec(sender, args[n+1]);
				SMSVariables vars = SMSVariables.getVariables(targetPlayer, false);
				vars.set(targetVar, null);
				if (!quiet) MiscUtil.statusMessage(sender, "Deleted variable &f" + args[n+1] + "&-.");
			} else if (args[n].startsWith("-i")) {
				// increment by the given amount (if possible)
				try {
					parseVarSpec(sender, args[n+1]);
					SMSVariables vars = SMSVariables.getVariables(targetPlayer, false);
					int val = Integer.parseInt(vars.get(targetVar));
					int incr = args.length > n+2 ? Integer.parseInt(args[n+2]) : 1;
					vars.set(targetVar, Integer.toString(val + incr));
					if (!quiet) MiscUtil.statusMessage(sender, args[n+1] + " = '&e" + vars.get(targetVar) + "&-'");
				} catch (NumberFormatException e) {
					throw new SMSException(e.getMessage() + ": not a number");
				}
			} else {
				parseVarSpec(sender, args[n]);
				SMSVariables vars = SMSVariables.getVariables(targetPlayer, true);
				vars.set(targetVar, args[n+1]);
				if (!quiet) MiscUtil.statusMessage(sender, args[n] + " = '&e" + vars.get(targetVar) + "&-'");
			}
		}

		return true;
	}

	private void parseVarSpec(CommandSender sender, String spec) {
		String[] parts = spec.split("\\.", 2);
		if (parts.length == 1) {
			notFromConsole(sender);
			targetPlayer = sender.getName();
			targetVar = parts[0];
		} else if (parts.length > 1) {
			targetPlayer = parts[0];
			targetVar = parts[1];
		} else {
			// shouldn't get here!
			throw new IllegalArgumentException("bad variable spec " + spec);
		}
		if (!targetVar.matches("[a-zA-Z0-9_]+")) {
			throw new SMSException("Invalid variable name " + spec + " (must be all alphanumeric)");
		}
		if (!targetPlayer.equalsIgnoreCase(sender.getName())) {
			PermissionUtils.requirePerms(sender, "scrollingmenusign.vars.other");
		}
	}
}
