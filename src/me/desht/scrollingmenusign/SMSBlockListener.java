package me.desht.scrollingmenusign;

import java.util.logging.Level;

import org.bukkit.Location;
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
		if (event.isCancelled()) return;
		
		Block b = event.getBlock();
		if (b.getType() != Material.SIGN_POST && b.getType() != Material.WALL_SIGN) {
			return;
		}
		String menuName = SMSMenu.getMenuNameAt(b.getLocation());
		if (menuName == null) {
			return;
		}
		plugin.debug("block damage event @ " + b.getLocation() + ", menu=" + menuName);
		Player p = event.getPlayer();
		try { 
			SMSMenu menu = SMSMenu.getMenu(menuName);
			if (p.getName().equals(menu.getOwner()) || SMSPermissions.isAllowedTo(p, "scrollingmenusign.destroy")) {
				// do nothing, allow damage to continue
			} else {
				// don't allow destruction
				event.setCancelled(true);
				SMSMenu.getMenu(menuName).updateSign(b.getLocation());
			}
		} catch (SMSNoSuchMenuException e) {
			SMSUtils.errorMessage(event.getPlayer(), e.getError());
		}
	}
	
	@Override
	public void onBlockBreak(BlockBreakEvent event) {
		if (event.isCancelled()) return;
		
		Block b = event.getBlock();
		Player p = event.getPlayer();

		try {
			if (b.getType() == Material.SIGN_POST || b.getType() == Material.WALL_SIGN) {
				String menuName = SMSMenu.getMenuNameAt(b.getLocation());
				if (menuName != null) {
					SMSMenu menu = SMSMenu.getMenu(menuName);
					plugin.debug("block break event @ " + b.getLocation() + ", menu=" + menuName);
					Location l = b.getLocation();
					menu.removeSign(l);
					SMSUtils.statusMessage(p, "Sign @ &f" + SMSUtils.formatLocation(l) + "&- was removed from menu &e" + menuName + "&-");
					menu.autosave();
				}
			}
		} catch (SMSNoSuchMenuException e) {
			SMSUtils.errorMessage(p, e.getError());
		}
	}

	@Override
	public void onBlockPhysics(BlockPhysicsEvent event) {
		if (event.isCancelled()) return;
		
		Block b = event.getBlock();
		if (b.getType() == Material.SIGN_POST || b.getType() == Material.WALL_SIGN) {
			String menuName = SMSMenu.getMenuNameAt(b.getLocation());
			if (menuName != null) {
				plugin.debug("block physics event @ " + b.getLocation() + ", menu=" + menuName);
				if (plugin.getConfiguration().getBoolean("sms.no_physics", false)) {
					event.setCancelled(true);
				} else {
					try {
						Sign s = (Sign) b.getState().getData();
						Block attachedBlock = b.getRelative(s.getAttachedFace());
						if (attachedBlock.getTypeId() == 0) {
							// attached to air? looks like the sign has become detached
							SMSMenu menu = SMSMenu.getMenu(menuName);
							menu.removeSign(b.getLocation());
							menu.autosave();
						}
					} catch (SMSNoSuchMenuException e) {
						SMSUtils.log(Level.WARNING, e.getError());
					}
				}
			}
		}
	}
}
