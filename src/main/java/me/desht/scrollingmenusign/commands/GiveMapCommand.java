package me.desht.scrollingmenusign.commands;

import me.desht.scrollingmenusign.SMSException;
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

public class GiveMapCommand extends AbstractCommand {

	public GiveMapCommand() {
		super("sms gi", 1, 3);
		setPermissionNode("scrollingmenusign.commands.givemap");
		setUsage("/sms givemap <id> [<amount>] [<player>]");
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {		
		int amount = 1;
		if (args.length >= 2) {
			try {
				amount = Integer.parseInt(args[1]);
			} catch (NumberFormatException e) {
				throw new SMSException("Invalid amount '" + args[1] + "'.");
			}
		}
		Player targetPlayer;
		if (args.length >= 3) {
			targetPlayer = Bukkit.getPlayer(args[2]);
		} else {
			notFromConsole(sender);
			targetPlayer = (Player) sender;
		}
		short mapId;
		try {
			mapId = Short.parseShort(args[0]);
		} catch (NumberFormatException e) {
			throw new SMSException("Invalid map ID '" + args[0] + "'.");
		}
		
		if (amount < 1)
			amount = 1;
		if (amount > 64)
			amount = 64;
		
		if (Bukkit.getServer().getMap(mapId) == null) {
			World world = (sender instanceof Player) ? ((Player) sender).getWorld() : Bukkit.getWorlds().get(0);
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
		return true;
	}

}
