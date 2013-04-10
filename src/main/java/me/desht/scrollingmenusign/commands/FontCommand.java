package me.desht.scrollingmenusign.commands;

import java.awt.Font;
import java.awt.GraphicsEnvironment;

import me.desht.dhutils.MessagePager;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class FontCommand extends SMSAbstractCommand {

	public FontCommand() {
		super("sms font", 0, 1);
		setPermissionNode("scrollingmenusign.commands.font");
		setUsage("/sms font");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		Font[] fonts = ge.getAllFonts();

		MessagePager pager = MessagePager.getPager(sender).clear();

		int matched = 0;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < fonts.length; i++) {
			String fn = fonts[i].getName();
			if (args.length >= 1 && !fn.toLowerCase().contains(args[0].toLowerCase())) {
				continue;
			}
			if (sb.length() + fn.length() > 60) {
				pager.add(sb.toString());
				sb.setLength(0);
			}
			sb.append(fn + ", ");
			matched++;
		}
		pager.add(sb.toString());
		pager.add(matched + " fonts matched (of " + fonts.length + " total)");
		pager.showPage();

		return true;
	}

}
