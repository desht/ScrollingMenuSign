package me.desht.scrollingmenusign.commands;

import me.desht.scrollingmenusign.PopupBook;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.views.PoppableView;
import me.desht.scrollingmenusign.views.SMSInventoryView;
import me.desht.scrollingmenusign.views.SMSMapView;
import me.desht.scrollingmenusign.views.SMSView;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.commands.AbstractCommand;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapView;
import org.bukkit.plugin.Plugin;

public class GiveCommand extends AbstractCommand {

	public GiveCommand() {
		super("sms gi", 2, 4);
		setPermissionNode("scrollingmenusign.commands.givemap");
		setUsage(new String[] {
				"/sms give map <id> [<amount>] [<player>]",
				"/sms give book <view-name> [<amount>] [<player>]",	
		});
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		int amount = 1;
		if (args.length >= 3) {
			try {
				amount = Math.min(64, Math.max(1, Integer.parseInt(args[2])));
			} catch (NumberFormatException e) {
				throw new SMSException("Invalid amount '" + args[1] + "'.");
			}
		}
		Player targetPlayer;
		if (args.length >= 4) {
			targetPlayer = Bukkit.getPlayer(args[3]);
			if (targetPlayer == null) {
				throw new SMSException("Player '" + args[3] + "' is not online.");
			}
		} else {
			notFromConsole(sender);
			targetPlayer = (Player) sender;
		}

		if (args[0].startsWith("m")) {
			short mapId = getMapId(targetPlayer, args[1]);
			giveMap(sender, targetPlayer, mapId, amount);
		} else if (args[0].startsWith("b")) {
			giveBook(sender, targetPlayer, args[1], amount);
		} else {
			showUsage(sender);
		}
		return true;
	}

	private short getMapId(Player target, String argStr) {
		short mapId;
		try {
			// first, see if it's a map ID
			mapId = Short.parseShort(argStr);
		} catch (NumberFormatException e) {
			// maybe it's a view name?
			if (SMSView.checkForView(argStr)) {
				SMSView v = SMSView.getView(argStr);
				if (!(v instanceof SMSMapView)) {
					throw new SMSException("View " + v.getName() + " is not a map view");
				}
				mapId = ((SMSMapView) v).getMapView().getId();
			} else {
				// or perhaps a menu name?
				SMSMenu menu = SMSMenu.getMenu(argStr);
				SMSView v = SMSView.findView(menu, SMSMapView.class);
				if (v == null) {
					// this menu doesn't have a map view - make one!
					mapId = Bukkit.createMap(target.getWorld()).getId();
					v = SMSMapView.addMapToMenu(menu, mapId);
				} else {
					// menu has a map view already - use that map ID
					mapId = ((SMSMapView) v).getMapView().getId();
				}
			}
		}

		return mapId;
	}

	@SuppressWarnings("deprecation")
	private void giveBook(CommandSender sender, Player targetPlayer, String argStr, int amount) {
		SMSView view;

		if (SMSView.checkForView(argStr)) {
			view = SMSView.getView(argStr);
			if (!(view instanceof PoppableView)) {
				throw new SMSException("View '" + argStr + "' isn't a poppable view.");
			}
		} else {
			SMSMenu menu = SMSMenu.getMenu(argStr);
			view = SMSView.findView(menu, PoppableView.class);
			if (view == null) {
				view = SMSInventoryView.addInventoryViewToMenu(menu);
			}
		}

		PopupBook book = new PopupBook(targetPlayer, view);
		ItemStack writtenbook = book.toItemStack();
		targetPlayer.getInventory().addItem(writtenbook);
		targetPlayer.updateInventory();

		String s = amount == 1 ? "" : "s";
		MiscUtil.statusMessage(sender, String.format("Gave %d book%s (&6%s&-) to &6%s", amount, s, argStr, targetPlayer.getName()));
		if (sender != targetPlayer) {
			MiscUtil.statusMessage(targetPlayer, String.format("You received %d books%s for menu &6%s", amount, s, view.getNativeMenu().getTitle()));
		}
	}

	@SuppressWarnings("deprecation")
	private void giveMap(CommandSender sender, Player targetPlayer, short mapId, int amount) {
		if (Bukkit.getServer().getMap(mapId) == null) {
			World world = targetPlayer.getWorld();
			MapView mv = Bukkit.getServer().createMap(world);
			mapId = mv.getId();
		}

		ItemStack stack = new ItemStack(Material.MAP, amount);
		stack.setDurability(mapId);
		targetPlayer.getInventory().addItem(stack);
		targetPlayer.updateInventory();

		String s = amount == 1 ? "" : "s";
		MiscUtil.statusMessage(sender, String.format("Gave %d map%s (&6map_%d&-) to &6%s", amount, s, mapId, targetPlayer.getName()));
		if (sender != targetPlayer) {
			MiscUtil.statusMessage(targetPlayer, String.format("You received %d map%s of type &6map_%d&-", amount, s, mapId));	
		}
	}

}
