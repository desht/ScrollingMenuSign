package me.desht.scrollingmenusign.commands;

import me.desht.dhutils.DHValidate;
import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.*;
import me.desht.scrollingmenusign.util.SMSUtil;
import me.desht.scrollingmenusign.views.PoppableView;
import me.desht.scrollingmenusign.views.SMSMapView;
import me.desht.scrollingmenusign.views.SMSView;
import me.desht.scrollingmenusign.views.ViewManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapView;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.List;

public class GiveCommand extends SMSAbstractCommand {

    public GiveCommand() {
        super("sms give", 2, 5);
        setPermissionNode("scrollingmenusign.commands.give");
        setUsage(new String[]{
                "/sms give map <menu-name|view-name|map-id> [<amount>] [<player>]",
                "/sms give book <menu-name|view-name> [<amount>] [<player>]",
                "/sms give popup <menu-name|view-name> <material-name>[:<data>] [<amount>] [<player>]",
        });
        setQuotedArgs(true);
    }

    @Override
    public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
        int amount = 1;
        int amountArg = 2, playerArg = 3;
        if (args[0].startsWith("p")) {
            // popup item has an extra argument
            amountArg++; playerArg++;
        }

        if (args.length > amountArg) {
            try {
                amount = Math.min(64, Math.max(1, Integer.parseInt(args[amountArg])));
            } catch (NumberFormatException e) {
                throw new SMSException("Invalid amount '" + args[amountArg] + "'.");
            }
        }

        Player targetPlayer;
        if (args.length > playerArg) {
            //noinspection deprecation
            targetPlayer = Bukkit.getPlayer(args[playerArg]);
            if (targetPlayer == null) {
                throw new SMSException("Player '" + args[playerArg] + "' is not online.");
            }
        } else {
            notFromConsole(sender);
            targetPlayer = (Player) sender;
        }

        if (args[0].startsWith("m")) {
            short mapId = getMapId(sender, targetPlayer, args[1]);
            giveMap(sender, targetPlayer, mapId, amount);
        } else if (args[0].startsWith("b")) {
            giveBook(sender, targetPlayer, args[1], amount);
        } else if (args[0].startsWith("p") && args.length > 2) {
            givePopupItem(sender, targetPlayer, args[1], args[2], amount);
        } else {
            showUsage(sender);
        }
        return true;
    }

    private short getMapId(CommandSender sender, Player target, String argStr) {
        short mapId;
        ViewManager vm = ScrollingMenuSign.getInstance().getViewManager();

        try {
            // first, see if it's a map ID
            mapId = Short.parseShort(argStr);
        } catch (NumberFormatException e) {
            // maybe it's a view name?
            if (vm.checkForView(argStr)) {
                SMSView view = vm.getView(argStr);
                if (!(view instanceof SMSMapView)) {
                    throw new SMSException("View " + view.getName() + " is not a map view");
                }
                mapId = ((SMSMapView) view).getMapView().getId();
            } else {
                // or perhaps a menu name?
                SMSMenu menu = getMenu(sender, argStr);
                SMSView v = vm.findView(menu, SMSMapView.class);
                if (v == null) {
                    // this menu doesn't have a map view - make one!
                    mapId = Bukkit.createMap(target.getWorld()).getId();
                    ScrollingMenuSign.getInstance().getViewManager().addMapToMenu(menu, mapId, sender);
                } else {
                    // menu has a map view already - use that map ID
                    mapId = ((SMSMapView) v).getMapView().getId();
                }
            }
        }

        return mapId;
    }

    private void giveBook(CommandSender sender, Player targetPlayer, String viewOrMenu, int amount) {
        SMSView view = getView(viewOrMenu, sender);

        PopupBook book = new PopupBook(targetPlayer, view);
        ItemStack writtenbook = book.toItemStack(amount);
        targetPlayer.getInventory().addItem(writtenbook);

        String s = amount == 1 ? "" : "s";
        MiscUtil.statusMessage(sender, String.format("Gave %d book%s (&6%s&-) to &6%s", amount, s, viewOrMenu, targetPlayer.getName()));
        if (sender != targetPlayer) {
            MiscUtil.statusMessage(targetPlayer, String.format("You received %d books%s for menu &6%s", amount, s, view.getNativeMenu().getTitle()));
        }
    }

    private void giveMap(CommandSender sender, Player targetPlayer, short mapId, int amount) {
        if (Bukkit.getServer().getMap(mapId) == null) {
            World world = targetPlayer.getWorld();
            MapView mv = Bukkit.getServer().createMap(world);
            mapId = mv.getId();
        }

        ItemStack stack = new ItemStack(Material.MAP, amount);
        stack.setDurability(mapId);
        SMSMapView v = ScrollingMenuSign.getInstance().getViewManager().getMapViewForId(mapId);
        if (v != null) {
            v.setMapItemName(stack);
        }
        targetPlayer.getInventory().addItem(stack);

        String s = amount == 1 ? "" : "s";
        MiscUtil.statusMessage(sender, String.format("Gave %d map%s (&6map_%d&-) to &6%s", amount, s, mapId, targetPlayer.getName()));
        if (sender != targetPlayer) {
            MiscUtil.statusMessage(targetPlayer, String.format("You received %d map%s of type &6map_%d&-", amount, s, mapId));
        }
    }

    private void givePopupItem(CommandSender sender, Player targetPlayer, String viewOrMenu, String matName, int amount) {
        try {
            SMSView view = getView(viewOrMenu, sender);
            PopupItem item = PopupItem.create(SMSUtil.parseMaterialSpec(matName), view);
            ItemStack stack = item.toItemStack(amount);
            targetPlayer.getInventory().addItem(stack);

            String s = amount == 1 ? "" : "s";
            MiscUtil.statusMessage(sender, String.format("Gave %d popup item%s (&6%s&-) to &6%s",
                    amount, s, view.getName(), targetPlayer.getName()));
            if (sender != targetPlayer) {
                MiscUtil.statusMessage(targetPlayer, String.format("You received %d popup item%s for &6%s&-",
                        amount, s, view.getNativeMenu().getTitle()));
            }
        } catch (IllegalArgumentException e) {
            throw new SMSException(e.getMessage());
        }
    }

    private SMSView getView(String viewOrMenu, CommandSender sender) {
        SMSView view;
        ViewManager vm = ScrollingMenuSign.getInstance().getViewManager();
        if (vm.checkForView(viewOrMenu)) {
            view = vm.getView(viewOrMenu);
            if (!(view instanceof PoppableView)) {
                throw new SMSException("View '" + viewOrMenu + "' isn't a poppable view.");
            }
        } else {
            SMSMenu menu = ScrollingMenuSign.getInstance().getMenuManager().getMenu(viewOrMenu);
            view = vm.findView(menu, PoppableView.class);
            if (view == null) {
                view = vm.addInventoryViewToMenu(menu, sender);
            }
        }
        return view;
    }

    @Override
    public List<String> onTabComplete(Plugin plugin, CommandSender sender, String[] args) {
        switch (args.length) {
            case 1:
                return filterPrefix(sender, Arrays.asList("book", "map", "popup"), args[0]);
            case 2:
                return getMenuCompletions(plugin, sender, args[1]);
            case 3:
                if (args[0].startsWith("p")) {
                    return getEnumCompletions(sender, Material.class, args[2]);
                }
                break;
            case 4:
                if (args[0].startsWith("m") || args[0].startsWith("b")) {
                    return null; // list online players
                }
                break;
            case 5:
                if (args[0].startsWith("p")) {
                    return null; // list online players
                }
                break;
            default:
                showUsage(sender);
                return noCompletions(sender);
        }
        return noCompletions(sender);
    }
}
