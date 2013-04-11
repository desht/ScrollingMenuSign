package me.desht.scrollingmenusign.commands;

import java.util.List;

import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PermissionUtils;
import me.desht.scrollingmenusign.PopupBook;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.views.SMSMapView;
import me.desht.scrollingmenusign.views.SMSView;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class RemoveViewCommand extends SMSAbstractCommand {

	public RemoveViewCommand() {
		super("sms break", 0, 2);
		setPermissionNode("scrollingmenusign.commands.break");
		setUsage(new String[] {
				"/sms break",
				"/sms break -loc <x,y,z,world>",
				"/sms break -view <view-name>"
		});
		setOptions(new String[] { "loc:s", "view:s"});
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		SMSView view = null;

		if (hasOption("view")) {
			// detaching a view by view name
			view = SMSView.getView(getStringOption("view"));
		} else if (hasOption("loc")) {
			// detaching a view by location
			try {
				view = SMSView.getViewForLocation(MiscUtil.parseLocation(getStringOption("loc"), sender));
			} catch (IllegalArgumentException e) {
				throw new SMSException(e.getMessage());
			}
		} else if (args.length == 0) {
			// detaching a view that the player is looking at?
			notFromConsole(sender);
			view = SMSView.getTargetedView((Player) sender, true);
		}

		if (view == null) {
			throw new SMSException("No suitable view found to remove.");
		} else {
			PermissionUtils.requirePerms(sender, "scrollingmenusign.use." + view.getType());
			view.deletePermanent();
			MiscUtil.statusMessage(sender, String.format("Removed &9%s&- view &e%s&- from menu &e%s&-.",
			                                             view.getType(), view.getName(), view.getNativeMenu().getName()));
		}

		return true;
	}

	@Override
	public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
		switch (args.length) {
		case 1:
			return getViewCompletions(sender, args[0]);
		default:
			return noCompletions(sender);
		}
	}
}
