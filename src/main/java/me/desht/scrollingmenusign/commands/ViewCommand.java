package me.desht.scrollingmenusign.commands;

import me.desht.dhutils.MessagePager;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PermissionUtils;
import me.desht.dhutils.commands.AbstractCommand;
import me.desht.scrollingmenusign.views.SMSGlobalScrollableView;
import me.desht.scrollingmenusign.views.SMSMapView;
import me.desht.scrollingmenusign.views.SMSSpoutView;
import me.desht.scrollingmenusign.views.SMSView;
import me.desht.scrollingmenusign.views.redout.Switch;

import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class ViewCommand extends AbstractCommand {

	public ViewCommand() {
		super("sms vi", 0, 3);
		setPermissionNode("scrollingmenusign.commands.view");
		setUsage(new String[] {
				"sms view",
				"sms view <view-name>",
				"sms view <view-name> <attribute>",
				"sms view <view-name> <attribute> <new-value>",		
		});
		setQuotedArgs(true);
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		SMSView view = null;
		if (args.length > 0 && !args[0].equals(".")) {
			view = SMSView.getView(args[0]);
		} else {
			notFromConsole(sender);
			Player player = (Player)sender;
			try {
				Block b = player.getTargetBlock(null, 3);
				view = SMSView.getViewForLocation(b.getLocation());
			} catch (IllegalStateException e) {
				view = null;
			}
			if (view == null) {
				view = SMSMapView.getHeldMapView(player);
			}
		}
		
		if (view == null) {
			// maybe the player's looking at an output switch
			if (lookingAtSwitch(sender)) {
				return true;
			} else {
				MiscUtil.errorMessage(sender, "No suitable view found.");
				return true;
			}
		}

		if (args.length <= 1) {
			MessagePager pager = MessagePager.getPager(sender).clear();
			pager.add(String.format("View &e%s&- (%s) :", view.getName(), view.toString()));
			for (String k : view.listAttributeKeys(true)) {
				pager.add(String.format("&5*&- &e%s&- = &e%s&-", k, view.getAttributeAsString(k, "")));
			}
			if (view instanceof SMSGlobalScrollableView) {
				SMSGlobalScrollableView gsv = (SMSGlobalScrollableView) view;
				int nSwitches = gsv.getSwitches().size();
				if (nSwitches > 0) {
					String s = nSwitches > 1 ? "es"	: "";
					pager.add("This view has &f" + nSwitches + "&- output switch" + s + ":");
					for (Switch sw: gsv.getSwitches()) {
						pager.add(String.format("&5*&- &e%s&- @ &e%s",
						                        sw.getTrigger(), MiscUtil.formatLocation(sw.getLocation())));
					}
				}
			}
			pager.showPage();
			return true;
		}

		if (args[1].equals("-popup")) {
			notFromConsole(sender);
			PermissionUtils.requirePerms(sender, "scrollingmenusign.use.spout");
			if (view instanceof SMSSpoutView) {
				SMSSpoutView spv = (SMSSpoutView) view;
				spv.toggleGUI((Player) sender);
			}
		} else {
			String attr = args[1];

			if (args.length == 3) {
				view.setAttribute(attr, args[2]);
				view.autosave();
			}

			MiscUtil.statusMessage(sender, String.format("&e%s.%s&- = &e%s&-", view.getName(), attr, view.getAttributeAsString(attr)));
		}
		
		return true;
	}

	private boolean lookingAtSwitch(CommandSender sender) {
		if (!(sender instanceof Player)) {
			return false;
		}
		try {
			Block b = ((Player) sender).getTargetBlock(null, 3);
			Switch sw = Switch.getSwitchAt(b.getLocation());
			if (sw != null) {
				MiscUtil.statusMessage(sender, String.format("Output switch @ &e%s&- for view &e%s&- / &e%s&-",
				                                             MiscUtil.formatLocation(sw.getLocation()),
				                                             sw.getView().getName(), sw.getTrigger()));
				return true;
			}
		} catch (IllegalStateException e) {
			return false;
		}
		return false;
	}

}
