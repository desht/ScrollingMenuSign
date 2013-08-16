package me.desht.scrollingmenusign.commandlets;

import java.util.Arrays;

import me.desht.dhutils.ItemMessage;
import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.SMSValidate;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.views.CommandTrigger;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.google.common.base.Joiner;

public class QuickMessageCommandlet extends BaseCommandlet {

	public QuickMessageCommandlet() {
		super("QUICKMSG");
	}

	@Override
	public boolean execute(ScrollingMenuSign plugin, CommandSender sender, CommandTrigger trigger, String cmd, String[] args) {
		SMSValidate.isTrue(args.length >= 3, "Usage: " + cmd + " <duration> <message>");
		SMSValidate.isTrue(sender instanceof Player, "Not from the console!");
		SMSValidate.isTrue(args[1].matches("^\\d+$"), "Invalid numeric quantity: " + args[1]);
		int duration = Integer.parseInt(args[1]);
		String msg = Joiner.on(" ").join(Arrays.copyOfRange(args, 2, args.length));
		if (plugin.isProtocolLibEnabled()) {
			ItemMessage im = new ItemMessage(plugin);
			im.setFormats(ChatColor.RED + "\u258b " + ChatColor.RESET + "%s", "  %s");
			im.sendMessage((Player)sender, MiscUtil.parseColourSpec(msg), duration);
		} else {
			sender.sendMessage(MiscUtil.parseColourSpec(msg));
		}
		return true;
	}

}
