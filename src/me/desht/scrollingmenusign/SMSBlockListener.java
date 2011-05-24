package me.desht.scrollingmenusign;

import me.desht.scrollingmenusign.ScrollingMenuSign.MenuRemoveAction;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.material.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPhysicsEvent;

public class SMSBlockListener extends BlockListener {

	private ScrollingMenuSign plugin;
	
	public SMSBlockListener(ScrollingMenuSign plugin) {
		this.plugin = plugin;
	}

	@Override
	public void onBlockDamage(BlockDamageEvent event) {
		Block b = event.getBlock();
		if (b.getType() != Material.SIGN_POST && b.getType() != Material.WALL_SIGN) {
			return;
		}
		String menuName = plugin.getMenuName(b.getLocation());
		if (menuName == null) {
			return;
		}
		Player p = event.getPlayer();
		SMSMenu menu = plugin.getMenu(menuName);
		if (p.getName().equals(menu.getOwner()) || plugin.isAllowedTo(p, "scrollingmenusign.destroy")) {
			// do nothing, allow damage to continue
		} else {
			// don't allow destruction
			event.setCancelled(true);
			plugin.getMenu(menuName).updateSign();
		}
	}
	
	@Override
	public void onBlockBreak(BlockBreakEvent event) {
		Block b = event.getBlock();
		Player p = event.getPlayer();

		if (b.getType() == Material.SIGN_POST || b.getType() == Material.WALL_SIGN) {
			String menuName = plugin.getMenuName(b.getLocation());
			if (menuName != null) {
				plugin.removeMenu(menuName, ScrollingMenuSign.MenuRemoveAction.DO_NOTHING);
				plugin.status_message(p, "Destroyed menu sign: " + menuName);
			}
		}
	}

	@Override
	public void onBlockPhysics(BlockPhysicsEvent event) {
		Block b = event.getBlock();
		
		if (b.getType() == Material.SIGN_POST || b.getType() == Material.WALL_SIGN) {
			String menuName = plugin.getMenuName(b.getLocation());
			if (menuName != null) {
				Sign s = (Sign) b.getState().getData();
				Block attachedBlock = b.getFace(s.getAttachedFace());
				if (attachedBlock.getTypeId() == 0) {
					// attached to air? looks like the sign has become detached
					plugin.removeMenu(menuName, MenuRemoveAction.DO_NOTHING);
				}
			}
		}
	}
	
}
