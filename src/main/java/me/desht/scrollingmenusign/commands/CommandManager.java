package me.desht.scrollingmenusign.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.util.PermissionsUtils;

public class CommandManager {
	private ScrollingMenuSign plugin;
	private final List<AbstractCommand> cmdList = new ArrayList<AbstractCommand>();

	public CommandManager(ScrollingMenuSign plugin) {
		this.plugin = plugin;
	}
	
	public void registerCommand(AbstractCommand cmd) {
		cmdList.add(cmd);
	}

	public boolean dispatch(Player player, String label, String[] args) throws SMSException {
		boolean res = false;
		for (AbstractCommand cmd : cmdList) {
			if (cmd.matchesSubCommand(label, args)) {
				if (cmd.matchesArgCount(label, args)) {
					PermissionsUtils.requirePerms(player, cmd.getPermissionNode());
					String[] actualArgs = cmd.getArgs(args);
					res = cmd.execute(plugin, player, actualArgs);
				} else {
					cmd.showUsage(player);
					res = true;
				}
			}
		}
		return res;
	}
}
