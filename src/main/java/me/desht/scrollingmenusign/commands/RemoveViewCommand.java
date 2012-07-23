package me.desht.scrollingmenusign.commands;

import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PermissionUtils;
import me.desht.dhutils.commands.AbstractCommand;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.views.SMSMapView;
import me.desht.scrollingmenusign.views.SMSView;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

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
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		SMSView view = null;

		if (args.length == 2 && args[0].equals("-view")) {
			// detaching a view by view name
			view = SMSView.getView(args[1]);
		} else if (args.length == 2 && args[0].equals("-loc")) {
			// detaching a view by location
			try {
				view = SMSView.getViewForLocation(MiscUtil.parseLocation(args[1], sender));
			} catch (IllegalArgumentException e) {
				throw new SMSException(e.getMessage());
			}
		} else if (sender instanceof Player && (view = SMSMapView.getHeldMapView((Player)sender)) != null) {
			// detaching a map view - nothing else to check here
		} else if (args.length == 0) {
			// detaching a view that the player is looking at?
			notFromConsole(sender);
			view = SMSView.getTargetedView((Player) sender);
		}

		if (view == null) {
			throw new SMSException("No suitable view found to remove.");
		} else {
			PermissionUtils.requirePerms(sender, "scrollingmenusign.use." + view.getType());
			view.deletePermanent();
			MiscUtil.statusMessage(sender, String.format("Removed &9%s&- view &e%s&- from menu &e%s&-.",
			                                             view.getType(), view.getName(), view.getMenu().getName()));
		}

		return true;
	}
}
