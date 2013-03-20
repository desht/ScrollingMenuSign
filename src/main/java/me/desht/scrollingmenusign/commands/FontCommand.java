package me.desht.scrollingmenusign.commands;

import java.awt.Font;
import java.awt.GraphicsEnvironment;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import me.desht.dhutils.MessagePager;
import me.desht.dhutils.commands.AbstractCommand;

public class FontCommand extends AbstractCommand {

	public FontCommand() {
		super("sms f", 0, 1);
		setPermissionNode("sms.commands.font");
		setUsage("/sms font");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
	    Font[] fonts = ge.getAllFonts();

	    MessagePager pager = MessagePager.getPager(sender).clear();
	    
	    int matched = 0;
	    for (int i = 0; i < fonts.length; i++) {
	    	String fn = fonts[i].getName();
	    	if (args.length >= 1 && !fn.toLowerCase().contains(args[0].toLowerCase())) {
	    		continue;
	    	}
	    	pager.add(MessagePager.BULLET + ChatColor.WHITE + fn);
	    	matched++;
	    }
	    pager.add(matched + " fonts matched (of " + fonts.length + " total)");
	    pager.showPage();
	    
		return true;
	}

}
