package me.desht.scrollingmenusign.commands;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.ScrollingMenuSign;
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

		String attr = args[1];
		
		if (args.length == 3) {
			view.setAttribute(attr, args[2]);
		}
		MiscUtil.statusMessage(player, String.format("&e%s.%s&- = &e%s&-", view.getName(), attr, view.getAttributeAsString(attr)));
			
//		if (attr.equalsIgnoreCase("owner")) {
//			if (val != null) {
//				view.setOwner(val);
//			}
//			String vo = view.getOwner().isEmpty() ? "(no one)" : view.getOwner();
//			MiscUtil.statusMessage(player, view.getName() + ".owner = &e" + vo);
//		} else if (attr.equalsIgnoreCase("spoutkeys")) {
//			if (view instanceof SMSSpoutView) {
//				SMSSpoutView spv = (SMSSpoutView) view;
//				if (val != null) {
//					try {
//						spv.setActivationKeys(new SMSSpoutKeyMap(val));
//					} catch (IllegalArgumentException e) {
//						throw new SMSException("Bad Spout key definition: " + val);
//					}
//				}
//				MiscUtil.statusMessage(player, view.getName() + ".spoutkeys = &e" + spv.getActivationKeys());
//			} else {
//				throw new SMSException("This view type doesn't support the 'spoutkeys' attribute");
//			}
//		} else {
//			throw new SMSException("Unknown attribute '" + attr + "'.");
//		}
		return true;
	}

}
