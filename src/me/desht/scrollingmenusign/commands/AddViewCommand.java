package me.desht.scrollingmenusign.commands;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.views.SMSMapView;
import me.desht.scrollingmenusign.views.SMSSignView;
import me.desht.scrollingmenusign.views.SMSSpoutView;
import me.desht.scrollingmenusign.views.SMSView;
import me.desht.util.MiscUtil;
import me.desht.util.PermissionsUtils;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class AddViewCommand extends AbstractCommand {

	public AddViewCommand() {
		super("sms sy", 1, 3);
		setPermissionNode("scrollingmenusign.commands.sync");
		setUsage("/sms sync <menu-name> [-map <id>|-sign <loc>|-spout]");
	}

	@Override
	public boolean execute(ScrollingMenuSign plugin, Player player, String[] args) throws SMSException {
		SMSView view = null;
		SMSMenu menu = plugin.getHandler().getMenu(args[0]);

		if (args.length == 2 && args[1].equalsIgnoreCase("-spout")) {		// spout view
			if (plugin.isSpoutEnabled())
				view = SMSSpoutView.addSpoutViewToMenu(menu);
			else
				throw new SMSException("Server is not Spout-enabled");
		} else if (args.length == 3 && args[1].equalsIgnoreCase("-sign")) {	// sign view
			Location loc = MiscUtil.parseLocation(args[2], player);
			view = SMSSignView.addSignToMenu(menu, loc);
		} else if (args.length == 3 && args[1].equalsIgnoreCase("-map")) {	// map view
			try {
				short mapId = Short.parseShort(args[2]);
				view = SMSMapView.addMapToMenu(mapId, menu);
			} catch (NumberFormatException e) {
				throw new SMSException(e.getMessage());
			}
		} else if (args.length > 1) {
			MiscUtil.errorMessage(player, "Unknown view type: " + args[1]);
			return false;
		}

		if (view == null) {
			// see if we can get a view from what the player is looking at or holding
			notFromConsole(player);
			Block b = player.getTargetBlock(null, 3);
			if (b.getTypeId() == 63 || b.getTypeId() == 68) {					// sign view
				view = SMSSignView.addSignToMenu(menu, b.getLocation());
			} else if (player.getItemInHand().getTypeId() == 358) {				// map view
				PermissionsUtils.requirePerms(player, "scrollingmenusign.maps");
				short mapId = player.getItemInHand().getDurability();
				view = SMSMapView.addMapToMenu(mapId, menu);
			}
		}

		if (view != null) {
			MiscUtil.statusMessage(player, String.format("Added %s view &e%s&- to menu &e%s&-.",
			                                             view.getType(), view.getName(), menu.getName()));
			view.register();
		} else {
			throw new SMSException("Found nothing suitable to add as a menu view");
		}

		return true;
	}

}
