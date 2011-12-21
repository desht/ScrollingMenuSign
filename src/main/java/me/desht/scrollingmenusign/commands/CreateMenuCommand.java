package me.desht.scrollingmenusign.commands;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSHandler;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.util.MiscUtil;
import me.desht.scrollingmenusign.util.PermissionsUtils;
import me.desht.scrollingmenusign.views.SMSMapView;
import me.desht.scrollingmenusign.views.SMSSignView;

public class CreateMenuCommand extends AbstractCommand {

	public CreateMenuCommand() {
		super("sms c", 2);
		setPermissionNode("scrollingmenusign.commands.create");
		setUsage(new String[] { 
				"/sms create <menu> <title>",
				"/sms create <menu> from <other-menu>",
		});
	}

	@Override
	public boolean execute(ScrollingMenuSign plugin, Player player, String[] args) throws SMSException {
		String menuName = args[0];

		SMSHandler handler = plugin.getHandler();

		if (handler.checkMenu(menuName)) {
			throw new SMSException("A menu called '" + menuName + "' already exists.");
		}

		Location signLoc = null;
		short mapId = -1;
		String owner = "&console";	// dummy owner if menu created from console

		if (player != null) {
			owner = player.getName();
			Block b = player.getTargetBlock(null, 3);
			if (b.getType() == Material.SIGN_POST || b.getType() == Material.WALL_SIGN) {
				if (plugin.getHandler().getMenuNameAt(b.getLocation()) == null) {
					PermissionsUtils.requirePerms(player, "scrollingmenusign.use.sign");
					signLoc = b.getLocation();
				}
			} else if (player.getItemInHand().getType() == Material.MAP) {
				short id = player.getItemInHand().getDurability();
				if (!SMSMapView.checkForMapId(id)) {
					PermissionsUtils.requirePerms(player, "scrollingmenusign.use.map");
					mapId = id;
				}
			}
		}

		String menuTitle = MiscUtil.parseColourSpec(player, combine(args, 1));
		SMSMenu menu = handler.createMenu(menuName, menuTitle, owner);

		if (signLoc != null) {
			SMSSignView.addSignToMenu(menu, signLoc);
			MiscUtil.statusMessage(player, "Created new menu &e" + menuName + "&- with sign view @ &f" + MiscUtil.formatLocation(signLoc));
		} else if (mapId >= 0) {
			SMSMapView.addMapToMenu(menu, mapId);
			MiscUtil.statusMessage(player, "Created new menu &e" + menuName + "&- with map view map_" + mapId);
		} else {
			MiscUtil.statusMessage(player, "Created new menu &e" + menuName + "&- with no views");
		}

		return true;
	}

}
