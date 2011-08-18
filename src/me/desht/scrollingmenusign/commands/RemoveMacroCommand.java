package me.desht.scrollingmenusign.commands;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMacro;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.util.MiscUtil;

import org.bukkit.entity.Player;

public class RemoveMacroCommand extends AbstractCommand {

	public RemoveMacroCommand() {
		super("sms m r", 1, 2);
		setPermissionNode("scrollingmenusign.commands.macro");
		setUsage("/sms macro remove <macro> [<command>]");
	}

	@Override
	public boolean execute(ScrollingMenuSign plugin, Player player, String[] args) throws SMSException {
		if (args.length == 1) {
			SMSMacro.removeMacro(args[0]);
			MiscUtil.statusMessage(player, "Removed macro &e" + args[0] + "&-.");	
		} else {
			try { 
				int index = Integer.parseInt(args[1]);
				SMSMacro.removeCommand(args[0], index - 1);
				MiscUtil.statusMessage(player, "Removed command #" + index + " from macro &e" + args[0] + "&-.");
			} catch (NumberFormatException e) {
				MiscUtil.errorMessage(player, "invalid index: " + args[1]);
			} catch (IndexOutOfBoundsException e) {
				MiscUtil.errorMessage(player, "invalid index: " + args[1]);	
			}
		}
	
		plugin.saveMacros();
		
		return true;
	}

}
