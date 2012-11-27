package me.desht.scrollingmenusign.commands;

import me.desht.scrollingmenusign.PopupBook;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.views.PoppableView;
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
			short mapId;
			try {
				mapId = Short.parseShort(args[0]);
				giveMap(sender, targetPlayer, mapId, amount);
			} catch (NumberFormatException e) {
				throw new SMSException("Invalid map ID '" + args[0] + "'.");
			}
		} else if (args[0].startsWith("b")) {
			giveBook(sender, targetPlayer, args[1], amount);
		} else {
			showUsage(sender);
		}
		return true;
		
	}
	
	@SuppressWarnings("deprecation")
	private void giveBook(CommandSender sender, Player targetPlayer, String viewName, int amount) {
		SMSView view = SMSView.getView(viewName);
		if (!(view instanceof PoppableView)) {
			throw new SMSException("View '" + viewName + "' isn't a poppable view.");
		}
		
		PopupBook book = new PopupBook(targetPlayer, view);
		ItemStack writtenbook = book.toItemStack();
		targetPlayer.getInventory().addItem(writtenbook);
		targetPlayer.updateInventory();
		
		String s = amount == 1 ? "" : "s";
		MiscUtil.statusMessage(sender, String.format("Gave %d book%s (%s) to %s", amount, s, viewName, targetPlayer.getName()));
		if (sender != targetPlayer) {
			MiscUtil.statusMessage(targetPlayer, String.format("You received %d books%s for menu %s", amount, s, view.getMenu().getTitle()));
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
		MiscUtil.statusMessage(sender, String.format("Gave %d map%s (map_%d) to %s", amount, s, mapId, targetPlayer.getName()));
		if (sender != targetPlayer) {
			MiscUtil.statusMessage(targetPlayer, String.format("You received %d map%s of type &6map_%d&-", amount, s, mapId));	
		}
	}

}
