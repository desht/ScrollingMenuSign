package me.desht.scrollingmenusign.commands;

import me.desht.scrollingmenusign.SMSConfig;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.util.MiscUtil;

import org.bukkit.entity.Player;

public class SetConfigCommand extends AbstractCommand {

	public SetConfigCommand() {
		super("sms se", 2, 2);
		setPermissionNode("scrollingmenusign.commands.setcfg");
		setUsage("/sms setcfg <key> <value>");
		setQuotedArgs(true);
	}

	@Override
	public boolean execute(ScrollingMenuSign plugin, Player player, String[] args) throws SMSException {
		String key = args[0], val = args[1];
		
		try {
			SMSConfig.setConfigItem(player, key, val);
			if (key.matches("item_(justify|prefix.*)")) {
				SMSMenu.updateAllMenus();
			}
			MiscUtil.statusMessage(player, key + " is now set to \"&e" + val + "&-\"");
		} catch (SMSException e) {
			MiscUtil.errorMessage(player, "No such config key " + key);
			MiscUtil.errorMessage(player, "Use /sms getcfg to list all valid keys");
		}
		return true;
	}

}
