package me.desht.scrollingmenusign.commands;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.util.MessagePager;
import me.desht.scrollingmenusign.util.MiscUtil;

import org.bukkit.entity.Player;

public class PageCommand extends AbstractCommand {

	public PageCommand() {
		super("sms p", 0, 1);
		setUsage("/sms page [<page-number>|<next>|<prev>]");
	}

	@Override
	public boolean execute(ScrollingMenuSign plugin, Player player, String[] args) throws SMSException {
		if (args.length == 0 || partialMatch(args, 0, "n")) {
			// default is to advance one page and display
			MessagePager.nextPage(player);
			MessagePager.showPage(player);
		} else if (partialMatch(args, 0, "p")) { //$NON-NLS-1$
			MessagePager.prevPage(player);
			MessagePager.showPage(player);
		} else {
			try {
				int pageNum = Integer.parseInt(args[0]);
				MessagePager.showPage(player, pageNum);
			} catch (NumberFormatException e) {
				MiscUtil.errorMessage(player, "Invalid page number " + args[1]);
			}
		}
		
		return true;
	}

}
