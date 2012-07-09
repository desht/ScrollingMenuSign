package me.desht.scrollingmenusign.expector;

import me.desht.dhutils.DHUtilsException;
import me.desht.dhutils.responsehandler.ExpectBase;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.parser.CommandUtils;
import me.desht.dhutils.LogUtils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ExpectCommandSubstitution extends ExpectBase {
	private String command;
	private String sub;

	public ExpectCommandSubstitution(String command) {
		this.command = command;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public String getSub() {
		return sub;
	}

	public void setSub(String sub) {
		this.sub = sub;
	}

	@Override
	public void doResponse(String playerName) {
		String newCommand = command.replaceFirst("<\\$:.+?>", sub);
		LogUtils.fine("command substitution: sub = [" + sub + "], cmd = [" + newCommand + "]");
		try {
			Player player = Bukkit.getPlayer(playerName);
			if (player != null) {
				CommandUtils.executeCommand(player, newCommand);
			}
		} catch (SMSException e) {
			throw new DHUtilsException(e.getMessage());
		}
	}
}
