package me.desht.scrollingmenusign.expector;

import me.desht.dhutils.DHUtilsException;
import me.desht.dhutils.responsehandler.ExpectBase;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.parser.CommandUtils;
import me.desht.scrollingmenusign.views.SMSView;
import me.desht.dhutils.LogUtils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ExpectCommandSubstitution extends ExpectBase {
	private final String command;
	private final SMSView view;
	private final boolean isPassword;

	private String sub;

	public ExpectCommandSubstitution(String command, SMSView view, boolean isPassword) {
		this.command = command;
		this.view = view;
		this.isPassword = isPassword;
	}

	public ExpectCommandSubstitution(String command, SMSView view) {
		this(command, view, false);
	}

	public String getCommand() {
		return command;
	}

	public String getSub() {
		return sub;
	}

	public void setSub(String sub) {
		this.sub = sub;
	}

	public boolean isPassword() {
		return isPassword;
	}

	@Override
	public void doResponse(String playerName) {
		final String newCommand;
		if (isPassword) {
			newCommand = command.replaceFirst("<\\$p:.+?>", sub);
		} else {
			newCommand = command.replaceFirst("<\\$:.+?>", sub);	
		}

		LogUtils.fine("command substitution: sub = [" + sub + "], cmd = [" + newCommand + "]");
		try {
			final Player player = Bukkit.getPlayer(playerName);
			if (player != null) {
				// Using the scheduler here because this response handler is called by the AsyncPlayerChatEvent
				// event handler, which runs in a different thread.
				Bukkit.getScheduler().scheduleSyncDelayedTask(ScrollingMenuSign.getInstance(), new Runnable() {
					@Override
					public void run() { CommandUtils.executeCommand(player, newCommand, view);	}
				});
			}
		} catch (SMSException e) {
			throw new DHUtilsException(e.getMessage());
		}
	}
}
