package me.desht.scrollingmenusign.listeners;

import java.util.logging.Level;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSHandler;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSMenuItem;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.enums.SMSAction;
import me.desht.util.MiscUtil;
import me.desht.util.PermissionsUtils;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerListener;

public class SMSPlayerListener extends PlayerListener {
	private ScrollingMenuSign plugin;
	
	public SMSPlayerListener(ScrollingMenuSign plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.isCancelled()) {
			return;
		}
		Block block = event.getClickedBlock();
		if (block == null || !(block.getState() instanceof Sign)) {
			return;
		}
		Player player = event.getPlayer();
		
		SMSHandler handler = plugin.getHandler();
		String menuName = handler.getMenuNameAt(block.getLocation());
		try {
			if (menuName == null) {
				// No menu attached to this sign, but a left-click could create a new menu if the sign's
				// text is in the right format...
				if (event.getAction() == Action.LEFT_CLICK_BLOCK && player.getItemInHand().getTypeId() == 0) {
					tryToActivateSign(block, player); 
				}
			} else {
				// ok, it's a sign, and there's a menu on it
				plugin.debug("player interact event @ " + block.getLocation() + ", " + player.getName() + " did " + event.getAction() + ", menu=" + menuName);
				SMSMenu menu = handler.getMenu(menuName);
				SMSAction action = SMSAction.getAction(event);
				processAction(action, player, menu, block.getLocation());
			}
		} catch (SMSException e) {
			MiscUtil.errorMessage(player, e.getMessage());
		}
	}

	@Override
	public void onItemHeldChange(PlayerItemHeldEvent event) {
		try {
			SMSHandler handler = plugin.getHandler();
			Player player = event.getPlayer();
			Block b = player.getTargetBlock(null, 3);
			String menuName = handler.getMenuNameAt(b.getLocation());
			if (menuName == null)
				return;
			SMSMenu menu = handler.getMenu(menuName);
			SMSAction action = SMSAction.getAction(event);
			processAction(action, player, menu, b.getLocation());
		} catch (SMSException e) {
			MiscUtil.log(Level.WARNING, e.getMessage());
		}
	}

	private void processAction(SMSAction action, Player p, SMSMenu menu, Location l) throws SMSException {
		if (action == null)
			return;
		
		switch (action) {
		case EXECUTE:
			executeMenu(p, menu, l);
			break;
		case SCROLLDOWN:
		case SCROLLUP:
			scrollMenu(p, menu, l, action);
			break;
		}
	}
	
	private void scrollMenu(Player player, SMSMenu menu, Location l, SMSAction dir) throws SMSException {
		if (!PermissionsUtils.isAllowedTo(player, "scrollingmenusign.scroll"))
			return;
		
		switch (dir) {
		case SCROLLDOWN:
			menu.nextItem(l);
			break;
		case SCROLLUP:
			menu.prevItem(l);
			break;
		}
		menu.updateSign(l);
	}

	private void executeMenu(Player player, SMSMenu menu, Location l) throws SMSException {
		if (!PermissionsUtils.isAllowedTo(player, "scrollingmenusign.execute"))
			return;
		
		SMSMenuItem item = menu.getCurrentItem(l);
		if (item != null) {
			item.execute(player);
			item.feedbackMessage(player);
		}
	}

	private void tryToActivateSign(Block b, Player player) throws SMSException {
		Sign sign = (Sign) b.getState();
		if (!sign.getLine(0).equals("[sms]"))
			return;

		String name = sign.getLine(1);
		String title = MiscUtil.parseColourSpec(player, sign.getLine(2));
		if (name.isEmpty())
			return;
		
		SMSHandler handler = plugin.getHandler();
		if (handler.checkMenu(name)) {
			if (title.isEmpty()) {
				PermissionsUtils.requirePerms(player, "scrollingmenusign.commands.sync");
				try {
					SMSMenu menu = handler.getMenu(name);
					menu.addSign(b.getLocation(), true);
					MiscUtil.statusMessage(player, "Sign @ &f" + MiscUtil.formatLocation(b.getLocation()) +
							"&- was added to menu &e" + name + "&-");
				} catch (SMSException e) {
					MiscUtil.errorMessage(player, e.getMessage());
				}
			} else {
				MiscUtil.errorMessage(player, "A menu called '" + name + "' already exists.");
			}
		} else if (title.length() > 0) {
			PermissionsUtils.requirePerms(player, "scrollingmenusign.commands.create");
			SMSMenu menu = plugin.getHandler().createMenu(name, title, player.getName());
			menu.addSign(b.getLocation(), true);
			MiscUtil.statusMessage(player, "Sign @ &f" + MiscUtil.formatLocation(b.getLocation()) +
					"&- was added to new menu &e" + name + "&-");
		}

	}
	
}
