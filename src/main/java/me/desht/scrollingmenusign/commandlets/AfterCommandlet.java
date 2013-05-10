package me.desht.scrollingmenusign.commandlets;

import java.util.Arrays;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSValidate;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.parser.CommandUtils;
import me.desht.scrollingmenusign.views.SMSView;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import com.google.common.base.Joiner;

public class AfterCommandlet extends BaseCommandlet {

	public AfterCommandlet() {
		super("AFTER");
	}

	@Override
	public boolean execute(ScrollingMenuSign plugin, final CommandSender sender, final SMSView view, String cmd, String[] args) {
		SMSValidate.isTrue(args.length >= 3, "Usage: " + cmd + " <delay> <command string>");

		int delay;
		try {
			delay = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			throw new SMSException("Invalid numeric quantity: " + args[1]);
		}

		final String command = Joiner.on(" ").join(Arrays.copyOfRange(args, 2, args.length));

		Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
			@Override
			public void run() {
				CommandUtils.executeCommand(sender, command, view);
			}
		}, delay);
		
		return true;
	}
}
