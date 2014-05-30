package me.desht.scrollingmenusign;

import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.parser.CommandParser;
import me.desht.scrollingmenusign.parser.ParsedCommand;
import me.desht.scrollingmenusign.parser.SubstitutionHandler;
import me.desht.scrollingmenusign.util.Substitutions;
import me.desht.scrollingmenusign.variables.VariablesManager;
import me.desht.scrollingmenusign.views.ViewManager;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class SMSHandlerImpl implements SMSHandler {
    private final MenuManager menuManager;

    SMSHandlerImpl(ScrollingMenuSign plugin) {
        menuManager = plugin.getMenuManager();
    }

    @Override
    @Deprecated
    public SMSMenu createMenu(String name, String title, String owner) {
        SMSMenu menu;
        try {
            //noinspection deprecation
            menu = new SMSMenu(name, MiscUtil.parseColourSpec(title), owner);
        } catch (SMSException e) {
            // should not get here
            return null;
        }
        menuManager.registerMenu(name, menu);
        return menu;
    }

    @Override
    public SMSMenu createMenu(String name, String title, Player owner) {
        SMSMenu menu;
        try {
            menu = new SMSMenu(name, MiscUtil.parseColourSpec(title), owner);
        } catch (SMSException e) {
            e.printStackTrace();
            // should not get here
            return null;
        }
        menuManager.registerMenu(name, menu);
        return menu;
    }

    @Override
    public SMSMenu createMenu(String name, String title, Plugin owner) {
        SMSMenu menu;
        try {
            menu = new SMSMenu(name, MiscUtil.parseColourSpec(title), owner);
        } catch (SMSException e) {
            // should not get here
            return null;
        }
        menuManager.registerMenu(name, menu);
        return menu;
    }

    @Override
    public SMSMenu getMenu(String name) throws SMSException {
        return menuManager.getMenu(name);
    }

    @Override
    public boolean checkMenu(String name) {
        return menuManager.checkForMenu(name);
    }

    @Override
    public void deleteMenu(String name) throws SMSException {
        menuManager.getMenu(name).deletePermanent();
    }

    @Override
    public SMSMenu getMenuAt(Location loc) throws SMSException {
        return menuManager.getMenuAt(loc);
    }

    @Override
    public String getMenuNameAt(Location loc) {
        return menuManager.getMenuNameAt(loc);
    }

    @Override
    public List<SMSMenu> listMenus() {
        return menuManager.listMenus();
    }

    @Override
    public List<SMSMenu> listMenus(boolean isSorted) {
        return menuManager.listMenus(isSorted);
    }

    @Override
    public ParsedCommand executeCommand(CommandSender sender, String command) throws SMSException {
        return new CommandParser().executeCommand(sender, command);
    }

    @Override
    public ViewManager getViewManager() {
        return ScrollingMenuSign.getInstance().getViewManager();
    }

    @Override
    public VariablesManager getVariablesManager() {
        return ScrollingMenuSign.getInstance().getVariablesManager();
    }

    @Override
    public MenuManager getMenuManager() {
        return menuManager;
    }

    @Override
    public void addCommandSubstitution(String sub, SubstitutionHandler handler) {
        Substitutions.addSubstitutionHandler(sub, handler);
    }
}
