package me.desht.scrollingmenusign.commands;

import me.desht.scrollingmenusign.SMSConfig;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.util.MessagePager;
import me.desht.util.MiscUtil;

import org.bukkit.entity.Player;

public class GetConfigCommand extends AbstractCommand {

	public GetConfigCommand() {
		super("sms ge", 0, 1);
		setPermissionNode("scrollingmenusign.commands.getcfg");
		setUsage("/sms getcfg [<key>]");
	}

	@Override
	public boolean execute(ScrollingMenuSign plugin, Player player, String[] args) throws SMSException {	
		MessagePager.clear(player);
		if (args.length == 0) {
			for (String line : SMSConfig.getConfigList()) {
				MessagePager.add(player, line);
			}
			MessagePager.showPage(player);
		} else {
			String res = SMSConfig.getConfiguration().getString(args[0]);
			if (res != null) {
				MiscUtil.statusMessage(player, args[0] + " = '" + res + "'");
			} else {
				MiscUtil.errorMessage(player, "No such config item " + args[0]);
			}
		}
		
		return true;
	}

}
