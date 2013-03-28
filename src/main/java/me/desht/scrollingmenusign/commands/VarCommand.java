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
		super("sms var", 1, 4);
		setPermissionNode("scrollingmenusign.commands.var");
		setUsage(new String[] {
				"/sms var -l [<player>]",
				"/sms var [-q] [<player>.]<variable> [<value>]",
				"/sms var [-q] -d [<player>.]<variable>",
				"/sms var [-q] -i [<player>.]<variable> [<amount>]",
		});
		setOptions(new String[] { "q", "l", "d", "i" });
		setQuotedArgs(true);
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {

		boolean quiet = getBooleanOption("q");

		if (getBooleanOption("l")) {
			// list all variables for the player

			if (args.length == 0) notFromConsole(sender);

			String target = args.length >= 1 ? args[0] : sender.getName();

			SMSVariables vars = SMSVariables.getVariables(target, false);
			MessagePager pager = MessagePager.getPager(sender).clear();
			for (String key : vars.getVariables()) {
				pager.add(key + " = '&e" + vars.get(key) + "&-'");	
			}
			pager.showPage();
			return true;
		} 
		
		String varSpec = args[0];
		
		if (getBooleanOption("d")) {
			// delete the variable
			SMSVariables.set(sender, varSpec, null);
			if (!quiet)
				MiscUtil.statusMessage(sender, "Deleted variable &f" + varSpec + "&-.");
		} else if (getBooleanOption("i")) {
			// add a numeric quantity to the variable
			try {
				int val = Integer.parseInt(SMSVariables.get(sender, varSpec));
				int incr = args.length >= 2 ? Integer.parseInt(args[1]) : 1;
				SMSVariables.set(sender, varSpec, Integer.toString(val + incr));
				if (!quiet)
					MiscUtil.statusMessage(sender, varSpec + " = '&e" + (val+incr) + "&-'");
			} catch (NumberFormatException e) {
				throw new SMSException(e.getMessage() + ": not a number");
			}
		} else if (args.length >= 2) {
			// set the variable
			SMSVariables.set(sender, varSpec, args[1]);
			if (!quiet)
				MiscUtil.statusMessage(sender, varSpec + " = '&e" + args[1] + "&-'");
		} else {
			// just display the variable
			String def = SMSVariables.isSet(sender, varSpec) ? "" : " &6(default)";
			String val = SMSVariables.get(sender, varSpec);
			MiscUtil.statusMessage(sender, varSpec + " = '&e" + val + "&-'" + def);
		}

		return true;
	}
}
