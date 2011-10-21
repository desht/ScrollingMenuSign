package me.desht.scrollingmenusign.commands;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.views.SMSSpoutView;
import me.desht.scrollingmenusign.views.SMSView;
import me.desht.util.MessagePager;
import me.desht.util.MiscUtil;

import org.bukkit.entity.Player;

public class ViewCommand extends AbstractCommand {

	public ViewCommand() {
		super("sms v", 1, 3);
		setPermissionNode("scrollingmenusign.commands.view");
		setUsage("sms view <view-name> <attribute> [<new-value>]");
		setQuotedArgs(true);
	}

	@Override
	public boolean execute(ScrollingMenuSign plugin, Player player, String[] args) throws SMSException {
		SMSView view = SMSView.getView(args[0]);

		if (args.length == 1) {
			MessagePager.clear(player);
			MessagePager.add(player, "View &e" + view.getName() + "&-:");
			for (String k : view.listAttributeKeys(true)) {
				MessagePager.add(player, String.format("* &e%s&- = &e%s&-", k, view.getAttributeAsString(k)));
			}
			MessagePager.showPage(player);
			return true;
		}

		if (args[1].equals("-popup")) {
			notFromConsole(player);
			if (view instanceof SMSSpoutView) {
				SMSSpoutView spv = (SMSSpoutView) view;
				spv.toggleGUI(player);
			}
		} else {
			String attr = args[1];

			if (args.length == 3) {
				view.setAttribute(attr, args[2]);
				view.autosave();
			}

			MiscUtil.statusMessage(player, String.format("&e%s.%s&- = &e%s&-", view.getName(), attr, view.getAttributeAsString(attr)));
		}
		
		return true;
	}

}
