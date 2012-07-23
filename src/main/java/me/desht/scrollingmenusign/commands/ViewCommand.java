package me.desht.scrollingmenusign.commands;

import me.desht.dhutils.MessagePager;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.commands.AbstractCommand;
import me.desht.scrollingmenusign.RedstoneControlSign;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.enums.SMSMenuAction;
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
			view = SMSView.getTargetedView(player);
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
			showViewDetails(sender, view);
		} else if (args[1].equals("-popup")) {
			notFromConsole(sender);
			view.ensureAllowedToUse((Player) sender);
			if (view instanceof SMSSpoutView) {
				SMSSpoutView spv = (SMSSpoutView) view;
				spv.toggleGUI((Player) sender);
			}
		} else if (args[1].startsWith("-d") && args.length >= 3) {
			// delete user-defined view variable
			if (args[2].startsWith("$")) {
				String var = args[2].substring(1);
				view.setVariable(var, null);
				view.autosave();
				view.update(view.getMenu(), SMSMenuAction.REPAINT);
				MiscUtil.statusMessage(sender, "Deleted view variable: &a" + var + "&-.");
			}
		} else {
			String attr = args[1];

			if (attr.startsWith("$")) {
				String var = attr.substring(1);
				// user-defined view variable
				if (args.length == 3) {
					view.setVariable(var, args[2]);
					view.autosave();
					view.update(view.getMenu(), SMSMenuAction.REPAINT);
				}
				MiscUtil.statusMessage(sender, String.format("&a%s.$%s&- = &a%s&-", view.getName(), var, view.getVariable(var)));
			} else {
				// predefined view attribute
				if (args.length == 3) {
					view.setAttribute(attr, args[2]);
					view.autosave();
				}
				MiscUtil.statusMessage(sender, String.format("&e%s.%s&- = &e%s&-", view.getName(), attr, view.getAttributeAsString(attr)));
			}
		}

		return true;
	}

	private void showViewDetails(CommandSender sender, SMSView view) {
		MessagePager pager = MessagePager.getPager(sender).clear();
		pager.add(String.format("View &e%s&- (%s) :", view.getName(), view.toString()));
		for (String k : view.listAttributeKeys(true)) {
			pager.add(String.format("&5*&- &e%s&- = &e%s&-", k, view.getAttributeAsString(k, "")));
		}
		for (String k : MiscUtil.asSortedList(view.listVariables())) {
			pager.add(String.format("&4* &a$%s&- = &a%s", k, view.getVariable(k)));
		}
		if (view instanceof SMSGlobalScrollableView) {
			SMSGlobalScrollableView gsv = (SMSGlobalScrollableView) view;
			int nSwitches = gsv.getSwitches().size();
			if (nSwitches > 0) {
				String s = nSwitches > 1 ? "s"	: "";
				pager.add("&f" + nSwitches + "&- redstone output" + s + ":");
				for (Switch sw: gsv.getSwitches()) {
					pager.add(String.format("&5*&- &e%s&- @ &e%s",
					                        sw.getTrigger(), MiscUtil.formatLocation(sw.getLocation())));
				}
			}
			int nCtrlSigns = gsv.getControlSigns().size();
			if (nCtrlSigns > 0) {
				String s = nCtrlSigns > 1 ? "s"	: "";
				pager.add("&f" + nCtrlSigns + "&- redstone control sign" + s + ":");
				for (RedstoneControlSign sign: gsv.getControlSigns()) {
					pager.add(String.format("&5*&- &e%s&-", sign));
				}
			}
		}
		pager.showPage();
	}

	private boolean lookingAtSwitch(CommandSender sender) {
		if (!(sender instanceof Player)) {
			return false;
		}
		try {
			Block b = ((Player) sender).getTargetBlock(null, ScrollingMenuSign.BLOCK_TARGET_DIST);
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
