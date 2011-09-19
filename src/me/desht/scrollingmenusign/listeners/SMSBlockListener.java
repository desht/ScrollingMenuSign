package me.desht.scrollingmenusign.listeners;

import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.views.SMSView;
import me.desht.util.Debugger;
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
		if (event.isCancelled())
			return;
		
		Block b = event.getBlock();
		if (b.getType() != Material.SIGN_POST && b.getType() != Material.WALL_SIGN) {
			return;
		}	
		Location loc = b.getLocation();
		SMSView view = SMSView.getViewForLocation(loc);
		if (view == null)
			return;
		
		SMSMenu menu = view.getMenu();
		Debugger.getDebugger().debug("block damage event @ " + MiscUtil.formatLocation(loc) + ", menu=" + menu.getName());
		Player p = event.getPlayer();
		if (p.getName().equalsIgnoreCase(menu.getOwner()) || PermissionsUtils.isAllowedTo(p, "scrollingmenusign.destroy")) 
			return;

		// don't allow destruction
		event.setCancelled(true);
	}
	
	@Override
	public void onBlockBreak(BlockBreakEvent event) {
		if (event.isCancelled())
			return;
		
		Block b = event.getBlock();
		Player p = event.getPlayer();
		
		if (b.getType() == Material.SIGN_POST || b.getType() == Material.WALL_SIGN) {
			Location loc = b.getLocation();
			SMSView view = SMSView.getViewForLocation(loc);
			if (view != null) {
				Debugger.getDebugger().debug("block break event @ " + b.getLocation() + ", menu=" + view.getMenu().getName());
				if (plugin.getConfiguration().getBoolean("sms.no_destroy_signs", false)) {
					event.setCancelled(true);
				} else {
					view.deletePermanent();
					MiscUtil.statusMessage(p, "Sign @ &f" + MiscUtil.formatLocation(loc) + "&- was removed from menu &e" + view.getMenu().getName() + "&-");
				}
			}
		}
	}

	@Override
	public void onBlockPhysics(BlockPhysicsEvent event) {
		if (event.isCancelled())
			return;
		
		Block b = event.getBlock();
		if (b.getType() == Material.SIGN_POST || b.getType() == Material.WALL_SIGN) {
			Location loc = b.getLocation();
			SMSView view = SMSView.getViewForLocation(loc);
			if (view != null) {
				Debugger.getDebugger().debug("block physics event @ " + b.getLocation() + ", menu=" + view.getMenu().getName());
				if (plugin.getConfiguration().getBoolean("sms.no_physics", false)) {
					event.setCancelled(true);
				} else {
					Sign s = (Sign) b.getState().getData();
					Block attachedBlock = b.getRelative(s.getAttachedFace());
					if (attachedBlock.getTypeId() == 0) {
						// attached to air? looks like the sign has become detached
						view.deletePermanent();
					}
				}
			}
		}
	}
}
