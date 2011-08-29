package me.desht.scrollingmenusign.commands;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.scrollingmenusign.views.SMSMapView;
import me.desht.scrollingmenusign.views.SMSSignView;
import me.desht.scrollingmenusign.views.SMSView;
import me.desht.util.MiscUtil;
import me.desht.util.PermissionsUtils;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class AddViewCommand extends AbstractCommand {

	public AddViewCommand() {
		super("sms sy", 1, 1);
		setPermissionNode("scrollingmenusign.commands.sync");
		setUsage("/sms sync <menu-name>");
	}

	@Override
	public boolean execute(ScrollingMenuSign plugin, Player player, String[] args) throws SMSException {
		notFromConsole(player);
	
		Block b = player.getTargetBlock(null, 3);
		SMSMenu menu = plugin.getHandler().getMenu(args[0]);
		if (b.getTypeId() == 63 || b.getTypeId() == 68) {			// sign or signpost
			SMSView view = new SMSSignView(menu, b.getLocation());
			view.update(menu, SMSMenuAction.REPAINT);
			MiscUtil.statusMessage(player, String.format("Sign @ &f%s&- was added to menu &e%s&-.",
			                                             MiscUtil.formatLocation(b.getLocation()), menu.getName()));
		} else if (player.getItemInHand().getTypeId() == 358) {		// map
			PermissionsUtils.requirePerms(player, "scrollingmenusign.maps");
			short mapId = player.getItemInHand().getDurability();
			SMSMapView view = SMSMapView.addMapToMenu(mapId, menu);
			MiscUtil.statusMessage(player, String.format("Map &fmap_%d&- was added to menu &e%s&-.",
			                                             view.getMapView().getId(), menu.getName()));
		}
		
		return true;
	}

}
