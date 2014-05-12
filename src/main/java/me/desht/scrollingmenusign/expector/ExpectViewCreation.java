package me.desht.scrollingmenusign.expector;

import me.desht.dhutils.DHUtilsException;
import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.views.SMSView;
import me.desht.scrollingmenusign.views.ViewManager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ExpectViewCreation extends ExpectLocation {
    private final String viewName;
    private final SMSMenu menu;
    private final String arg;

    public ExpectViewCreation(String viewName, SMSMenu menu, String arg) {
        this.viewName = viewName;
        this.menu = menu;
        this.arg = arg;
    }

    @Override
    public void doResponse(UUID playerId) {
        SMSView view = null;
        ViewManager vm = ScrollingMenuSign.getInstance().getViewManager();
        Player player = Bukkit.getPlayer(playerId);
        try {
            // TODO: code smell
            if (arg.equals("sign")) {
                view = vm.addSignToMenu(viewName, menu, getLocation(), player);
            } else if (arg.equals("redstone")) {
                view = vm.addRedstoneViewToMenu(viewName, menu, getLocation(), player);
            } else if (arg.equals("multisign")) {
                view = vm.addMultiSignToMenu(viewName, menu, getLocation(), player);
            }
        } catch (SMSException e) {
            throw new DHUtilsException(e.getMessage());
        }

        if (view != null && player != null) {
            MiscUtil.statusMessage(player, String.format("Added %s view &e%s&- to menu &e%s&-.",
                    view.getType(), view.getName(), menu.getName()));
        }
    }
}