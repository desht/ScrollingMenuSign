package me.desht.scrollingmenusign.commands;

import me.desht.scrollingmenusign.MessageBuffer;
import me.desht.scrollingmenusign.SMSConfig;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSUtils;
import me.desht.scrollingmenusign.ScrollingMenuSign;

import org.bukkit.entity.Player;

public class GetConfigCommand extends AbstractCommand {

	public GetConfigCommand() {
		super("sms g", 0, 1);
		setPermissionNode("scrollingmenusign.commands.getcfg");
		setUsage("/sms getcfg [<key>]");
	}

	@Override
	public boolean execute(ScrollingMenuSign plugin, Player player, String[] args) throws SMSException {	
		MessageBuffer.clear(player);
		if (args.length == 0) {
			for (String line : SMSConfig.getConfigList()) {
				MessageBuffer.add(player, line);
			}
			MessageBuffer.showPage(player);
		} else {
			String res = SMSConfig.getConfiguration().getString(args[0]);
			if (res != null) {
				SMSUtils.statusMessage(player, args[0] + " = '" + res + "'");
			} else {
				SMSUtils.errorMessage(player, "No such config item " + args[0]);
			}
		}
		
		return true;
	}

}
