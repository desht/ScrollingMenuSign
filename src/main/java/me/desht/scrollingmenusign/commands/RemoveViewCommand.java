package me.desht.scrollingmenusign.commands;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.util.MiscUtil;
import me.desht.scrollingmenusign.util.PermissionsUtils;
import me.desht.scrollingmenusign.views.SMSMapView;
import me.desht.scrollingmenusign.views.SMSView;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class RemoveViewCommand extends AbstractCommand {

	public RemoveViewCommand() {
		super("sms b", 0, 2);
		setPermissionNode("scrollingmenusign.commands.break");
		setUsage(new String[] {
				"/sms break",
				"/sms break -loc <x,y,z,world>",
				"/sms break -view <view-name>"
		});
	}

	@Override
	public boolean execute(ScrollingMenuSign plugin, Player player, String[] args) throws SMSException {
		SMSView view = null;

		if (args.length == 2 && args[0].equals("-view")) {
			// detaching a view by view name
			view = SMSView.getView(args[1]);
		} else if (args.length == 2 && args[0].equals("-loc")) {
			// detaching a view by location
			try {
				view = SMSView.getViewForLocation(MiscUtil.parseLocation(args[0], player));
			} catch (IllegalArgumentException e) {
				throw new SMSException(e.getMessage());
			}
			view = SMSView.getViewForLocation(MiscUtil.parseLocation(args[0], player));
		} else if (player != null && player.getItemInHand().getTypeId() == 358) {
			// detaching a map view 
			PermissionsUtils.requirePerms(player, "scrollingmenusign.maps");
			view = SMSMapView.getViewForId(player.getItemInHand().getDurability());
		} else if (args.length == 0) {
			// detaching a view that the player is looking at
			notFromConsole(player);
			Block b = player.getTargetBlock(null, 3);
			view = SMSView.getViewForLocation(b.getLocation());
		}

		if (view == null) {
			throw new SMSException("No suitable view found to remove.");
		} else {
			view.deletePermanent();
			MiscUtil.statusMessage(player, String.format("Removed &9%s&- view &e%s&- from menu &e%s&-.",
			                                             view.getType(), view.getName(), view.getMenu().getName()));
		}

		return true;
	}
}
