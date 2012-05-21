package me.desht.scrollingmenusign.commands;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.expector.ExpectViewCreation;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PermissionUtils;
import me.desht.dhutils.commands.AbstractCommand;
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

public class AddViewCommand extends AbstractCommand {

	public AddViewCommand() {
		super("sms sy", 1, 3);
		setPermissionNode("scrollingmenusign.commands.sync");
		setUsage("/sms sync <menu-name> [-map <id>|-sign <loc>|-spout]");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) throws SMSException {
		ScrollingMenuSign smsPlugin = (ScrollingMenuSign) plugin;
		
		SMSView view = null;
		SMSMenu menu = SMSMenu.getMenu(args[0]);

		boolean multiSign = false;
		
		if (args.length == 2 && args[1].equalsIgnoreCase("-spout")) {		// spout view
			if (smsPlugin.isSpoutEnabled())
				view = SMSSpoutView.addSpoutViewToMenu(menu);
			else
				throw new SMSException("Server is not Spout-enabled");
		} else if (args.length == 3 && args[1].equalsIgnoreCase("-sign")) {			// sign view
			Location loc = MiscUtil.parseLocation(args[2], sender);
			view = SMSSignView.addSignToMenu(menu, loc);
		}  else if (args.length == 3 && args[1].equalsIgnoreCase("-redstone")) {	// redstone view
			Location loc = MiscUtil.parseLocation(args[2], sender);
			view = SMSRedstoneView.addRedstoneViewToMenu(menu, loc);
		} else if (args.length == 2 && (args[1].equalsIgnoreCase("-sign") || args[1].equalsIgnoreCase("-redstone"))) {
			// create a new view interactively
			notFromConsole(sender);
			String type = args[1].substring(1);
			MiscUtil.statusMessage(sender, "Left-click a block to add it as a &9" + type + "&- view on menu &e" + menu.getName() + "&-.");
			MiscUtil.statusMessage(sender, "Right-click anywhere to cancel.");
			smsPlugin.responseHandler.expect((Player)sender, new ExpectViewCreation(menu, args[1]));
			return true;
		} else if (args.length == 2 && args[1].equalsIgnoreCase("-multi")) { 	// multi-sign view
			multiSign = true;
		} else if (args.length == 3 && args[1].equalsIgnoreCase("-map")) {	// map view
			try {
				short mapId = Short.parseShort(args[2]);
				view = SMSMapView.addMapToMenu(menu, mapId);
			} catch (NumberFormatException e) {
				throw new SMSException(e.getMessage());
			}
		} else if (args.length > 1) {
			MiscUtil.errorMessage(sender, "Unknown view type: " + args[1]);
			return false;
		}

		if (view == null) {
			// see if we can get a view from what the player is looking at or holding
			notFromConsole(sender);
			Player player = (Player) sender;
			if (player.getItemInHand().getType() == Material.MAP) {				// map view
				PermissionUtils.requirePerms(sender, "scrollingmenusign.use.map");
				short mapId = player.getItemInHand().getDurability();
				view = SMSMapView.addMapToMenu(menu, mapId);
			} else {
				try {
					Block b = player.getTargetBlock(null, 3);						// sign view ?
					if (multiSign && b.getType() == Material.WALL_SIGN) {
						view = SMSMultiSignView.addSignToMenu(menu, b.getLocation());
					} else if (b.getType() == Material.WALL_SIGN || b.getType() == Material.SIGN_POST) {
						view = SMSSignView.addSignToMenu(menu, b.getLocation());
					}
				} catch (IllegalStateException e) {
					// ignore
				}
			}
		}

		if (view != null) {
			MiscUtil.statusMessage(sender, String.format("Added &9%s&- view &e%s&- to menu &e%s&-.",
			                                             view.getType(), view.getName(), menu.getName()));
//			view.autosave();
		} else {
			throw new SMSException("Found nothing suitable to add as a menu view");
		}

		return true;
	}

}
