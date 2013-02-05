package me.desht.scrollingmenusign.commands;

import me.desht.dhutils.MessagePager;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.commands.AbstractCommand;
import me.desht.scrollingmenusign.RedstoneControlSign;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.scrollingmenusign.views.PoppableView;
import me.desht.scrollingmenusign.views.SMSGlobalScrollableView;
import me.desht.scrollingmenusign.views.SMSMapView;
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
				"sms view <view-name> [<attribute|$var>] [<new-value>]",
				"sms view <view-name> -d [<$var>]",
				"sms view <view-name> -popup"
		});
		setQuotedArgs(true);
		setOptions(new String[] { "popup", "d:s" });
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
		
		if (getBooleanOption("popup")) {
			notFromConsole(sender);
			view.ensureAllowedToUse((Player) sender);
			if (view instanceof PoppableView) {
				PoppableView pop = (PoppableView) view;
				pop.toggleGUI((Player) sender);
			} else {
				throw new SMSException("This view type can't be popped up");
			}
		} else if (hasOption("d")) {
			String varName = getStringOption("d");
			if (varName.startsWith("$")) {
				varName = varName.substring(1);
				view.setVariable(varName, null);
				view.autosave();
				view.update(null, SMSMenuAction.REPAINT);
				MiscUtil.statusMessage(sender, "Deleted view variable: &a" + varName + "&-.");
			}
		} else if (args.length <= 1) {
			showViewDetails(sender, view);
		} else {
			String attr = args[1];
			if (attr.startsWith("$")) {
				String varName = attr.substring(1);
				// user-defined view variable
				if (args.length == 3) {
					view.setVariable(varName, args[2]);
					view.autosave();
					view.update(null, SMSMenuAction.REPAINT);
				}
				MiscUtil.statusMessage(sender, String.format("&a%s.$%s&- = &a%s&-", view.getName(), varName, view.getVariable(varName)));
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
		pager.add(String.format("View &e%s&- (%s) :",
		                        view.getName(), view.toString()));
		pager.add(String.format("Native menu: &e%s&-, your active menu: &e%s&-",
		                        view.getNativeMenu().getName(), view.getActiveMenu(sender.getName()).getName()));
		for (String k : view.listAttributeKeys(true)) {
			pager.add(String.format(MessagePager.BULLET + "&e%s&- = &e%s&-", k, view.getAttributeAsString(k, "")));
		}
		for (String k : MiscUtil.asSortedList(view.listVariables())) {
			pager.add(String.format("&4\u2022 &a$%s&- = &a%s", k, view.getVariable(k)));
		}
		if (view instanceof SMSGlobalScrollableView) {
			SMSGlobalScrollableView gsv = (SMSGlobalScrollableView) view;
			int nSwitches = gsv.getSwitches().size();
			if (nSwitches > 0) {
				String s = nSwitches > 1 ? "s"	: "";
				pager.add("&f" + nSwitches + "&- redstone output" + s + ":");
				for (Switch sw: gsv.getSwitches()) {
					pager.add(String.format(MessagePager.BULLET + "&e%s&- @ &e%s",
					                        sw.getTrigger(), MiscUtil.formatLocation(sw.getLocation())));
				}
			}
			int nCtrlSigns = gsv.getControlSigns().size();
			if (nCtrlSigns > 0) {
				String s = nCtrlSigns > 1 ? "s"	: "";
				pager.add("&f" + nCtrlSigns + "&- redstone control sign" + s + ":");
				for (RedstoneControlSign sign: gsv.getControlSigns()) {
					pager.add(String.format(MessagePager.BULLET + "&e%s&-", sign));
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
