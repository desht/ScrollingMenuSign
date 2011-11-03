package me.desht.scrollingmenusign.commands;

import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.util.Debugger;
import me.desht.scrollingmenusign.util.MiscUtil;

import org.bukkit.entity.Player;

public class DebugCommand extends AbstractCommand {

	public DebugCommand() {
		super("sms deb", 0, 0);
		setPermissionNode("scrollingmenusign.commands.debug");
		setUsage("/sms debug");
	}

	@Override
	public boolean execute(ScrollingMenuSign plugin, Player player, String[] args) {
		Debugger.getDebugger().toggleDebug(player);
		int level = Debugger.getDebugger().getDebugLevel(player);
		if (level > 0) {
			MiscUtil.statusMessage(player, "Debugging enabled.");
		} else {
			MiscUtil.statusMessage(player, "Debugging disabled.");
		}
	
		return true;
	}

}
