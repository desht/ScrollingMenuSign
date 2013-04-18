package me.desht.scrollingmenusign.commands;

import java.util.List;

import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PermissionUtils;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSValidate;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.expector.ExpectViewCreation;
import me.desht.scrollingmenusign.views.SMSInventoryView;
import me.desht.scrollingmenusign.views.SMSMapView;
import me.desht.scrollingmenusign.views.SMSMultiSignView;
import me.desht.scrollingmenusign.views.SMSRedstoneView;
import me.desht.scrollingmenusign.views.SMSSignView;
import me.desht.scrollingmenusign.views.SMSSpoutView;
import me.desht.scrollingmenusign.views.SMSView;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class AddViewCommand extends SMSAbstractCommand {

	public AddViewCommand() {
		super("sms sync", 1);
		setPermissionNode("scrollingmenusign.commands.sync");
		setUsage(new String[] {
				"/sms sync <menu-name>",
				"/sms sync <menu-name> -sign [-loc <x,y,z,world>]",
				"/sms sync <menu-name> -multi [-loc <x,y,z,world>]",
				"/sms sync <menu-name> -redstone [-loc <x,y,z,world>]",
				"/sms sync <menu-name> -map <map-id>",
				"/sms sync <menu-name> -spout",
				"/sms sync <menu-name> -inv",
				"  -viewname <name>  Choose a non-default view name"
		});
		setOptions("map:i,sign,spout,redstone,multi,inventory,inv,viewname:s,loc:s");	
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) throws SMSException {
		ScrollingMenuSign smsPlugin = (ScrollingMenuSign) plugin;

		SMSView view = null;
		SMSMenu menu = SMSMenu.getMenu(args[0]);
		String viewName = getStringOption("viewname");
		Location loc = hasOption("loc") ? MiscUtil.parseLocation(getStringOption("loc")) : null;

		if (hasOption("spout")) {		// spout view
			if (smsPlugin.isSpoutEnabled())
				view = SMSSpoutView.addSpoutViewToMenu(viewName, menu, sender);
			else
				throw new SMSException("Server is not Spout-enabled");
		} else if (hasOption("sign")) {			// sign view
			if (loc == null) {
				interactiveCreation(sender, viewName, menu, "sign");
				return true;
			} else {
				view = SMSSignView.addSignToMenu(viewName, menu, loc, sender);
			}
		} else if (hasOption("redstone")) {
			if (loc == null) {
				interactiveCreation(sender, viewName, menu, "redstone");
				return true;
			} else {
				view = SMSRedstoneView.addRedstoneViewToMenu(viewName, menu, loc, sender);
			}
		} else if (hasOption("inventory") || hasOption("inv")) {
			view = SMSInventoryView.addInventoryViewToMenu(viewName, menu, sender);
		} else if (hasOption("multi") && loc != null) { 	// multi-sign view
			view = SMSMultiSignView.addSignToMenu(viewName, menu, loc, sender);
		} else if (hasOption("map")) {	// map view
			try {
				short mapId = (short) getIntOption("map");
				view = SMSMapView.addMapToMenu(viewName, menu, mapId, sender);
			} catch (NumberFormatException e) {
				throw new SMSException(e.getMessage());
			}
		} else if (args.length > 1) {
			throw new SMSException("Unknown view type: " + args[1]);
		}

		if (view == null) {
			// see if we can get a view from what the player is looking at or holding
			notFromConsole(sender);
			Player player = (Player) sender;
			if (player.getItemInHand().getType() == Material.MAP) {		// map view?
				PermissionUtils.requirePerms(sender, "scrollingmenusign.use.map");
				short mapId = player.getItemInHand().getDurability();
				view = SMSMapView.addMapToMenu(viewName, menu, mapId, sender);
				((SMSMapView) view).setMapItemName(player.getItemInHand());
			} else {
				try {
					Block b = player.getTargetBlock(null, ScrollingMenuSign.BLOCK_TARGET_DIST);		// sign view ?
					if (hasOption("multi") && b.getType() == Material.WALL_SIGN) {
						view = SMSMultiSignView.addSignToMenu(viewName, menu, b.getLocation(), sender);
					} else if (b.getType() == Material.WALL_SIGN || b.getType() == Material.SIGN_POST) {
						view = SMSSignView.addSignToMenu(viewName, menu, b.getLocation(), sender);
					}
				} catch (IllegalStateException e) {
					// ignore
				}
			}
		}

		SMSValidate.notNull(view, "Found nothing suitable to add as a menu view");
		MiscUtil.statusMessage(sender, String.format("Added &9%s&- view &e%s&- to menu &e%s&-.",
		                                             view.getType(), view.getName(), menu.getName()));
		return true;
	}

	@Override
	public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
		if (args.length == 1) {
			return getMenuCompletions(plugin, sender, args[0]);
		} else {
			showUsage(sender);
			return noCompletions(sender);
		}
	}

	private void interactiveCreation(CommandSender sender, String viewName, SMSMenu menu, String viewType) {
		notFromConsole(sender);
		MiscUtil.statusMessage(sender, "Left-click a block to add it as a &9" + viewType + "&- view on menu &e" + menu.getName() + "&-.");
		MiscUtil.statusMessage(sender, "Right-click anywhere to cancel.");
		ScrollingMenuSign.getInstance().responseHandler.expect(sender.getName(), new ExpectViewCreation(viewName, menu, viewType));
	}
}