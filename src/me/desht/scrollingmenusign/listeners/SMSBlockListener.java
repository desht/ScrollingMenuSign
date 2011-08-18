package me.desht.scrollingmenusign.listeners;

import java.util.logging.Level;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSHandler;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.util.MiscUtil;
import me.desht.util.PermissionsUtils;

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
		
		SMSHandler handler = plugin.getHandler();
		Block b = event.getBlock();
		if (b.getType() != Material.SIGN_POST && b.getType() != Material.WALL_SIGN) {
			return;
		}
		String menuName = handler.getMenuNameAt(b.getLocation());
		if (menuName == null) {
			return;
		}
		plugin.debug("block damage event @ " + b.getLocation() + ", menu=" + menuName);
		Player p = event.getPlayer();
		try { 
			SMSMenu menu = handler.getMenu(menuName);
			if (p.getName().equals(menu.getOwner()) || PermissionsUtils.isAllowedTo(p, "scrollingmenusign.destroy")) {
				// do nothing, allow damage to continue
			} else {
				// don't allow destruction
				event.setCancelled(true);
				handler.getMenu(menuName).updateSign(b.getLocation());
			}
		} catch (SMSException e) {
			MiscUtil.errorMessage(event.getPlayer(), e.getMessage());
		}
	}
	
	@Override
	public void onBlockBreak(BlockBreakEvent event) {
		if (event.isCancelled()) return;
		
		Block b = event.getBlock();
		Player p = event.getPlayer();

		try {
			SMSHandler handler = plugin.getHandler();
			if (b.getType() == Material.SIGN_POST || b.getType() == Material.WALL_SIGN) {
				String menuName = handler.getMenuNameAt(b.getLocation());
				if (menuName != null) {
					SMSMenu menu = handler.getMenu(menuName);
					plugin.debug("block break event @ " + b.getLocation() + ", menu=" + menuName);
					Location l = b.getLocation();
					menu.removeSign(l);
					MiscUtil.statusMessage(p, "Sign @ &f" + MiscUtil.formatLocation(l) + "&- was removed from menu &e" + menuName + "&-");
				}
			}
		} catch (SMSException e) {
			MiscUtil.errorMessage(p, e.getMessage());
		}
	}

	@Override
	public void onBlockPhysics(BlockPhysicsEvent event) {
		if (event.isCancelled()) return;
		
		SMSHandler handler = plugin.getHandler();
		Block b = event.getBlock();
		if (b.getType() == Material.SIGN_POST || b.getType() == Material.WALL_SIGN) {
			String menuName = handler.getMenuNameAt(b.getLocation());
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
							SMSMenu menu = handler.getMenu(menuName);
							menu.removeSign(b.getLocation());
						}
					} catch (SMSException e) {
						MiscUtil.log(Level.WARNING, e.getMessage());
					}
				}
			}
		}
	}
}
