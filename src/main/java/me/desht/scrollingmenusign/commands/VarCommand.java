package me.desht.scrollingmenusign.commands;

import me.desht.dhutils.MessagePager;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.commands.AbstractCommand;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSVariables;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class VarCommand extends AbstractCommand {

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
			String varSpec = args[0];
			String val = SMSVariables.get(sender, varSpec);
			if (val != null) {
				String def = SMSVariables.isSet(sender, varSpec) ? "" : " &6(default)";
				MiscUtil.statusMessage(sender, varSpec + " = '&e" + val + "&-'" + def);
			} else {
				throw new SMSException("No such variable: " + varSpec);
			}
		} else if (args.length >= 2) {

			int n = 0;
			boolean quiet = false;
			if (args[0].startsWith("-q")) {
				quiet = true;
				n = 1;
			}

			if (args[n].startsWith("-d")) {
				// delete the variable
				SMSVariables.set(sender, args[n+1], null);
				if (!quiet)
					MiscUtil.statusMessage(sender, "Deleted variable &f" + args[n+1] + "&-.");
			} else if (args[n].startsWith("-i")) {
				// increment by the given amount (if possible)
				try {
					int val = Integer.parseInt(SMSVariables.get(sender, args[n+1]));
					int incr = args.length > n+2 ? Integer.parseInt(args[n+2]) : 1;
					SMSVariables.set(sender, args[n+1], Integer.toString(val + incr));
					if (!quiet)
						MiscUtil.statusMessage(sender, args[n+1] + " = '&e" + (val+incr) + "&-'");
				} catch (NumberFormatException e) {
					throw new SMSException(e.getMessage() + ": not a number");
				}
			} else {
				// just set the variable
				SMSVariables.set(sender, args[n], args[n+1]);;
				if (!quiet)
					MiscUtil.statusMessage(sender, args[n] + " = '&e" + args[n+1] + "&-'");
			}
		}

		return true;
	}
}
