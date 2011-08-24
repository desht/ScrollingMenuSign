package me.desht.scrollingmenusign.commands;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.views.SMSView;
import me.desht.util.MiscUtil;

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
		
		MiscUtil.statusMessage(player, "Sign @ &f" + MiscUtil.formatLocation(loc) +
		                       "&- was removed from menu &e" + view.getMenu().getName() + "&-.");	
		view.deletePermanent();
		
		return true;
	}
}
