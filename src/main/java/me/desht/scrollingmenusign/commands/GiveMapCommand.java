package me.desht.scrollingmenusign.commands;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.util.MiscUtil;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapView;

public class GiveMapCommand extends AbstractCommand {

	public GiveMapCommand() {
		super("sms gi", 1, 2);
		setPermissionNode("scrollingmenusign.commands.givemap");
		setUsage("/sms givemap <id> [<amount>]");
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean execute(ScrollingMenuSign plugin, Player player, String[] args) throws SMSException {
		int amount = 1;
		if (args.length >= 2) {
			try {
				amount = Integer.parseInt(args[1]);
			} catch (NumberFormatException e) {
				throw new SMSException("Invalid amount '" + args[1] + "'.");
			}
		}
		short mapId;
		try {
			mapId = Short.parseShort(args[0]);
		} catch (NumberFormatException e) {
			throw new SMSException("Invalid amount '" + args[0] + "'.");
		}
		
		if (amount < 1)
			amount = 1;
		if (amount > 64)
			amount = 64;
		
		if (Bukkit.getServer().getMap(mapId) == null) {
			MapView mv = Bukkit.getServer().createMap(player.getWorld());
			mapId = mv.getId();
		}
		
		ItemStack stack = new ItemStack(Material.MAP, amount);
		stack.setDurability(mapId);
		player.getInventory().addItem(stack);
		player.updateInventory();
		
		String s = amount == 1 ? "" : "s";
		MiscUtil.statusMessage(player, String.format("Created %d map%s (map_%d)", amount, s, mapId));
		
		return true;
	}

}
