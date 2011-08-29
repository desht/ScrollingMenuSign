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
		super("sms b", 0, 1);
		setPermissionNode("scrollingmenusign.commands.break");
		setUsage("/sms break [<loc>]");
	}

	@Override
	public boolean execute(ScrollingMenuSign plugin, Player player, String[] args) throws SMSException {
		if (player != null && player.getItemInHand().getTypeId() == 358) {
			PermissionsUtils.requirePerms(player, "scrollingmenusign.maps");
			SMSMapView view = SMSMapView.getViewForId(player.getItemInHand().getDurability());
			if (view != null) {
				view.deletePermanent();
				MiscUtil.statusMessage(player, "Map &fmap_" + view.getMapView().getId() +
				                       "&- was removed from menu &e" + view.getMenu().getName() + "&-.");
			}
		} else {
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
			MiscUtil.statusMessage(player, "Sign @ &f" + MiscUtil.formatLocation(loc) +
			                       "&- was removed from menu &e" + view.getMenu().getName() + "&-.");	
		}
			
		return true;
	}
}
