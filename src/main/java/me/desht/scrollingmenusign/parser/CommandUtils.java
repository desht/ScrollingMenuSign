package me.desht.scrollingmenusign.parser;

import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.spout.SpoutUtils;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandUtils {
	/**
	 * High-level wrapper to run a command.  Return status is messaged to the calling command sender if necessary.
	 * 
	 * @param player	Player who is running the command
	 * @param command	The command to be run
	 * @throws SMSException
	 */
	public static void executeCommand(CommandSender sender, String command) {
		ParsedCommand pCmd = new CommandParser().executeCommand(sender, command);
		// pCmd could be null if this was an empty command
		if (pCmd != null) {
			switch(pCmd.getStatus()) {
			case CMD_OK:
			case RESTRICTED:
			case UNKNOWN:
				// these conditions don't need to be reported to the sender
				break;
			case SUBSTITUTION_NEEDED:
				if (sender instanceof Player) {
					if (!ScrollingMenuSign.getInstance().isSpoutEnabled() || !SpoutUtils.showTextEntryPopup((Player) sender, pCmd.getLastError())) {
						MiscUtil.alertMessage(sender, pCmd.getLastError() + " &6(Left or right-click anywhere to cancel)");
					}
				}
				break;
			default:
				// any other condition needs to be reported
				// this includes a cost check not being met, but not a restriction check not being met
				MiscUtil.errorMessage(sender, pCmd.getLastError());
				break;
			}
		}
	}
}
