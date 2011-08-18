package me.desht.scrollingmenusign.commands;

import me.desht.scrollingmenusign.MessageBuffer;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSUtils;
import me.desht.scrollingmenusign.ScrollingMenuSign;

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
			MessageBuffer.nextPage(player);
			MessageBuffer.showPage(player);
		} else if (partialMatch(args, 0, "p")) { //$NON-NLS-1$
			MessageBuffer.prevPage(player);
			MessageBuffer.showPage(player);
		} else {
			try {
				int pageNum = Integer.parseInt(args[0]);
				MessageBuffer.showPage(player, pageNum);
			} catch (NumberFormatException e) {
				SMSUtils.errorMessage(player, "Invalid page number " + args[1]);
			}
		}
		
		return true;
	}

}
