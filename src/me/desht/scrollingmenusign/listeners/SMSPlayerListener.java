package me.desht.scrollingmenusign.listeners;

import java.util.logging.Level;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSHandler;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSMenuItem;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.scrollingmenusign.enums.SMSUserAction;
import me.desht.scrollingmenusign.views.SMSScrollableView;
import me.desht.scrollingmenusign.views.SMSSignView;
import me.desht.scrollingmenusign.views.SMSView;
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
		if (event.isCancelled())
			return;
		Block block = event.getClickedBlock();
		if (block == null)
			return;
		
		Player player = event.getPlayer();
		
		SMSView view = SMSView.getViewForLocation(block.getLocation());
		try {
			if (view == null && block.getState() instanceof Sign) {
				// No view present at this location, but a left-click could create a new sign view if the sign's
				// text is in the right format...
				if (event.getAction() == Action.LEFT_CLICK_BLOCK && player.getItemInHand().getTypeId() == 0) {
					tryToActivateSign(block, player); 
				}
			} else if (view != null) {
				// ok, there's a view here
				plugin.debug("player interact event @ " + block.getLocation() + ", " + player.getName() + " did " + event.getAction() + ", menu=" + view.getMenu().getName());
				SMSUserAction action = SMSUserAction.getAction(event);
				processAction(action, player, view, block.getLocation());
			}
		} catch (SMSException e) {
			MiscUtil.errorMessage(player, e.getMessage());
		}
	}

	@Override
	public void onItemHeldChange(PlayerItemHeldEvent event) {
		try {
			Player player = event.getPlayer();
			Block block = player.getTargetBlock(null, 3);
			SMSView view = SMSView.getViewForLocation(block.getLocation());
			if (view == null)
				return;
			SMSUserAction action = SMSUserAction.getAction(event);
			processAction(action, player, view, block.getLocation());
		} catch (SMSException e) {
			MiscUtil.log(Level.WARNING, e.getMessage());
		}
	}

	private void processAction(SMSUserAction action, Player p, SMSView view, Location l) throws SMSException {
		if (action == null)
			return;
		
		SMSScrollableView sview;
		if (view instanceof SMSScrollableView) {
			sview = (SMSScrollableView) view;
		} else {
			return;
		}
		SMSMenu menu = sview.getMenu();
		switch (action) {
		case EXECUTE:
			SMSMenuItem item = menu.getItem(sview.getScrollPos());
			executeItem(p, item);
			break;
		case SCROLLDOWN:
			sview.scrollDown();
			sview.update(menu, SMSMenuAction.REPAINT);
			break;
		case SCROLLUP:
			sview.scrollUp();
			sview.update(menu, SMSMenuAction.REPAINT);
			break;
		}
	}
	
	private void executeItem(Player player, SMSMenuItem item) throws SMSException {
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
					SMSSignView.addSignToMenu(menu, b.getLocation());
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
			SMSSignView.addSignToMenu(menu, b.getLocation());
			MiscUtil.statusMessage(player, "Sign @ &f" + MiscUtil.formatLocation(b.getLocation()) +
					"&- was added to new menu &e" + name + "&-");
		}

	}
	
}
