package me.desht.scrollingmenusign.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSPermissions;
import me.desht.scrollingmenusign.ScrollingMenuSign;

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
			if (cmd.matches(label, args)) {
				if (cmd.getPermissionNode() != null) {
					SMSPermissions.requirePerms(player, cmd.getPermissionNode());
				}
				String[] actualArgs = cmd.getArgs(args);
				res = cmd.execute(plugin, player, actualArgs);
				break;
			}
		}
		return res;
	}

	static boolean partialMatch(String[] args, int index, String match) {
		if (index >= args.length) {
			return false;
		}
		return partialMatch(args[index], match);
	}

	static Boolean partialMatch(String str, String match) {
		int l = match.length();
		if (str.length() < l) return false;
		return str.substring(0, l).equalsIgnoreCase(match);
	}
	

	static String combine(String[] args, int idx) {
		return combine(args, idx, args.length - 1);
	}
	
	static String combine(String[] args, int idx1, int idx2) {
		StringBuilder result = new StringBuilder();
		for (int i = idx1; i <= idx2 && i < args.length; i++) {
			result.append(args[i]);
			if (i < idx2) {
				result.append(" ");
			}
		}
		return result.toString();
	}
}
