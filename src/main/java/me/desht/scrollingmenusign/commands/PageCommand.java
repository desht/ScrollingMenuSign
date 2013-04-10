package me.desht.scrollingmenusign.commands;

import me.desht.dhutils.MessagePager;
import me.desht.dhutils.MiscUtil;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class PageCommand extends SMSAbstractCommand {

	public PageCommand() {
		super("sms page", 0, 1);
		setUsage("/sms page [<page-number>|<next>|<prev>]");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		MessagePager pager = MessagePager.getPager(sender);
		if (args.length == 0 || args[0].startsWith("n")) {
			// default is to advance one page and display
			pager.nextPage();
			pager.showPage();
		} else if (args[0].startsWith("p")) { //$NON-NLS-1$
			pager.prevPage();
			pager.showPage();
		} else {
			try {
				int pageNum = Integer.parseInt(args[0]);
				pager.showPage(pageNum);
			} catch (NumberFormatException e) {
				MiscUtil.errorMessage(sender, "Invalid page number " + args[1]);
			}
		}

		return true;
	}

}
