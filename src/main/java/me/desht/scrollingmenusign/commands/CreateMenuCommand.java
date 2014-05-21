package me.desht.scrollingmenusign.commands;

import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PermissionUtils;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSHandler;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSValidate;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.util.SMSUtil;
import me.desht.scrollingmenusign.views.SMSMapView;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class CreateMenuCommand extends SMSAbstractCommand {

    public CreateMenuCommand() {
        super("sms create", 2);
        setPermissionNode("scrollingmenusign.commands.create");
        setUsage(new String[]{
                "/sms create <menu> <title>",
                "/sms create <menu> from <other-menu>",
        });
        setQuotedArgs(true);
    }

    @Override
    public boolean execute(Plugin plugin, CommandSender sender, String[] args) throws SMSException {
        String menuName = args[0];

        ScrollingMenuSign smsPlugin = (ScrollingMenuSign) plugin;
        SMSHandler handler = smsPlugin.getHandler();

        SMSValidate.isFalse(handler.checkMenu(menuName), "A menu called '" + menuName + "' already exists.");

        Location signLoc = null;
        short mapId = -1;

        boolean autoCreateView = ScrollingMenuSign.getInstance().getConfig().getBoolean("sms.autocreate_views");

        if (autoCreateView && sender instanceof Player) {
            Player player = (Player) sender;
            Block b = null;
            try {
                b = player.getTargetBlock(null, ScrollingMenuSign.BLOCK_TARGET_DIST);
            } catch (IllegalStateException e) {
                // ignore
            }
            if (b != null && (b.getType() == Material.SIGN_POST || b.getType() == Material.WALL_SIGN)) {
                if (handler.getMenuNameAt(b.getLocation()) == null) {
                    PermissionUtils.requirePerms(sender, "scrollingmenusign.use.sign");
                    signLoc = b.getLocation();
                }
            } else if (player.getItemInHand().getType() == Material.MAP) {
                short id = player.getItemInHand().getDurability();
                if (!getViewManager(plugin).checkForMapId(id) && !getViewManager(plugin).isMapUsedByOtherPlugin(id)) {
                    PermissionUtils.requirePerms(sender, "scrollingmenusign.use.map");
                    mapId = id;
                }
            }
        }

        String menuTitle = SMSUtil.unEscape(combine(args, 1));
        SMSMenu menu = handler.createMenu(menuName, menuTitle, sender instanceof Player ? (Player) sender : null);

        if (signLoc != null) {
            getViewManager(plugin).addSignToMenu(menu, signLoc, sender);
            MiscUtil.statusMessage(sender, "Created new menu &e" + menuName + "&- with sign view @ &f" + MiscUtil.formatLocation(signLoc));
        } else if (mapId >= 0) {
            SMSMapView mapView = getViewManager(plugin).addMapToMenu(menu, mapId, sender);
            MiscUtil.statusMessage(sender, "Created new menu &e" + menuName + "&- with map view &fmap_" + mapId);
            mapView.setMapItemName(((Player) sender).getItemInHand());
        } else {
            MiscUtil.statusMessage(sender, "Created new menu &e" + menuName + "&- with no initial view");
        }

        return true;
    }

}
