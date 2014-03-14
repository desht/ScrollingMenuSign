package me.desht.scrollingmenusign.commands;

import java.util.List;

import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PermissionUtils;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSValidate;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.views.ActiveItem;
import me.desht.scrollingmenusign.views.SMSMapView;
import me.desht.scrollingmenusign.views.SMSView;
import me.desht.scrollingmenusign.views.ViewManager;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class RemoveViewCommand extends SMSAbstractCommand {

	public RemoveViewCommand() {
		super("sms break", 0, 2);
		setPermissionNode("scrollingmenusign.commands.break");
		setUsage(new String[]{
				"/sms break",
				"/sms break <view-name>",
				"/sms break -loc <x,y,z,world>",
		});
		setOptions("loc:s", "view:s", "item", "frame");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		SMSView view = null;
		ViewManager viewManager = getViewManager(plugin);

		if (args.length == 0) {
			notFromConsole(sender);
			Player player = (Player) sender;
			if (hasOption("item")) {
				// deactivating an active item
				ActiveItem item = new ActiveItem(player.getItemInHand());
				item.deactivate();
				player.setItemInHand(item.toItemStack());
				MiscUtil.statusMessage(sender, "Deactivated held item: " + ChatColor.GOLD + player.getItemInHand().getType());
				return true;
			} else if (hasOption("frame")) {
				ItemFrame frame = viewManager.getMapFrame(player.getTargetBlock(null, ScrollingMenuSign.BLOCK_TARGET_DIST), player.getEyeLocation());
				if (frame != null) {
					ItemStack stack = frame.getItem();
					SMSMapView mv = viewManager.getMapViewForId(stack.getDurability());
					frame.getWorld().dropItemNaturally(frame.getLocation(), stack);
					frame.setItem(null);
					MiscUtil.statusMessage(player, "Removed map for menu &e" + mv.getNativeMenu().getName() + "&- from the item frame.");
					return true;
				} else {
					throw new SMSException("There is no item frame with a map view there.");
				}
			} else {
				// detaching a view that the player is looking at?
				view = viewManager.getTargetedView(player, true);
			}
		} else if (args.length == 1) {
			// detaching a view by view name
			view = getView(sender, args[0]);
		} else if (hasOption("loc")) {
			// detaching a view by location
			try {
				view = viewManager.getViewForLocation(MiscUtil.parseLocation(getStringOption("loc"), sender));
			} catch (IllegalArgumentException e) {
				throw new SMSException(e.getMessage());
			}
		}

		SMSValidate.notNull(view, "No suitable view found to remove.");
		PermissionUtils.requirePerms(sender, "scrollingmenusign.use." + view.getType());
		view.ensureAllowedToModify(sender);

		if (sender instanceof Player) {
			Player player = (Player) sender;
			if (view == viewManager.getHeldMapView(player)) {
				((SMSMapView) view).removeMapItemName(player.getItemInHand());
			}
		}
		viewManager.deleteView(view, true);
		MiscUtil.statusMessage(sender, String.format("Removed &9%s&- view &e%s&- from menu &e%s&-.",
				view.getType(), view.getName(), view.getNativeMenu().getName()));

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
