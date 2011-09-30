package me.desht.scrollingmenusign.commands;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.views.SMSView;
import me.desht.util.MiscUtil;

import org.bukkit.entity.Player;

public class ViewCommand extends AbstractCommand {

	public ViewCommand() {
		super("sms v", 2, 3);
		setPermissionNode("scrollingmenusign.commands.view");
		setUsage("sms view <view-name> <attribute> [<new-value>]");
		setQuotedArgs(true);
	}

	@Override
	public boolean execute(ScrollingMenuSign plugin, Player player, String[] args) throws SMSException {
		SMSView view = SMSView.getView(args[0]);
		String attr = args[1];
		String val = args.length >= 3 ? args[2] : null;

		if (attr.equalsIgnoreCase("owner")) {
			if (val != null) {
				view.setOwner(val);
			}
			String vo = view.getOwner().isEmpty() ? "(no one)" : view.getOwner();
			MiscUtil.statusMessage(player, view.getName() + ".owner = &e" + vo);
		} else {
			throw new SMSException("Unknown attribute '" + attr + "'.");
		}
		return true;
	}

}
