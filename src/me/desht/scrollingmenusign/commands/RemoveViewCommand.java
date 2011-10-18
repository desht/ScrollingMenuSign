package me.desht.scrollingmenusign.commands;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.views.SMSMapView;
import me.desht.scrollingmenusign.views.SMSView;
import me.desht.util.MiscUtil;
import me.desht.util.PermissionsUtils;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class RemoveViewCommand extends AbstractCommand {

	public RemoveViewCommand() {
		super("sms b", 0, 2);
		setPermissionNode("scrollingmenusign.commands.break");
		setUsage(new String[] {
				"/sms break [<loc>]",
				"/sms break -view <view-name>"
		});
	}

	@Override
	public boolean execute(ScrollingMenuSign plugin, Player player, String[] args) throws SMSException {
		if (args.length == 2 && args[0].equals("-view")) {
			// detaching any named view
			SMSView view = SMSView.getView(args[1]);
			view.deletePermanent();
			MiscUtil.statusMessage(player, String.format("Removed %s view &e%s&- from menu &e%s&-.",
			                                             view.getType(), view.getName(), view.getMenu().getName()));
		} else if (player != null && player.getItemInHand().getTypeId() == 358) {
			// detaching a map view 
			PermissionsUtils.requirePerms(player, "scrollingmenusign.maps");
			SMSMapView view = SMSMapView.getViewForId(player.getItemInHand().getDurability());
			if (view != null) {
				view.deletePermanent();
				MiscUtil.statusMessage(player, "Removed map view &e" + view.getName() +
				                       "&- from menu &e" + view.getMenu().getName() + "&-.");
			}
		} else {
			// detaching a sign view
			Location loc = null;
			if (args.length == 0) {
				notFromConsole(player);
				Block b = player.getTargetBlock(null, 3);
				loc = b.getLocation();
			} else {
				try {
					loc = MiscUtil.parseLocation(args[0], player);
				} catch (IllegalArgumentException e) {
					throw new SMSException(e.getMessage());
				}
			}
			
			SMSView view = SMSView.getViewForLocation(loc);
			if (view == null)
				throw new SMSException("You are not looking at a menu view.");
			view.deletePermanent();
			MiscUtil.statusMessage(player, "Removed sign view &e" + view.getName() +
			                       "&- from menu &e" + view.getMenu().getName() + "&-.");	
		}
			
		return true;
	}
}
