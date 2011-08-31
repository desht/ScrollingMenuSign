package me.desht.util;

import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;

public class QuietConsoleCommandSender extends ConsoleCommandSender {

	public QuietConsoleCommandSender(Server server) {
		super(server);
	}

	@Override
	public void sendMessage(String message) {
		// do nothing!
	}

}
