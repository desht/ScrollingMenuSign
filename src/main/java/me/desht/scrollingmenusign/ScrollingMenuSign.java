package me.desht.scrollingmenusign;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;

import me.desht.dhutils.*;
import me.desht.dhutils.MetaFaker.MetadataFilter;
import me.desht.dhutils.commands.CommandManager;
import me.desht.dhutils.cost.EconomyCost;
import me.desht.dhutils.responsehandler.ResponseHandler;
import me.desht.scrollingmenusign.commandlets.AfterCommandlet;
import me.desht.scrollingmenusign.commandlets.CloseSubmenuCommandlet;
import me.desht.scrollingmenusign.commandlets.CommandletManager;
import me.desht.scrollingmenusign.commandlets.CooldownCommandlet;
import me.desht.scrollingmenusign.commandlets.PopupCommandlet;
import me.desht.scrollingmenusign.commandlets.QuickMessageCommandlet;
import me.desht.scrollingmenusign.commandlets.ScriptCommandlet;
import me.desht.scrollingmenusign.commandlets.SubmenuCommandlet;
import me.desht.scrollingmenusign.commands.AddItemCommand;
import me.desht.scrollingmenusign.commands.AddMacroCommand;
import me.desht.scrollingmenusign.commands.AddViewCommand;
import me.desht.scrollingmenusign.commands.CreateMenuCommand;
import me.desht.scrollingmenusign.commands.DeleteMenuCommand;
import me.desht.scrollingmenusign.commands.EditMenuCommand;
import me.desht.scrollingmenusign.commands.FontCommand;
import me.desht.scrollingmenusign.commands.GetConfigCommand;
import me.desht.scrollingmenusign.commands.GiveCommand;
import me.desht.scrollingmenusign.commands.ItemUseCommand;
import me.desht.scrollingmenusign.commands.ListMacroCommand;
import me.desht.scrollingmenusign.commands.ListMenusCommand;
import me.desht.scrollingmenusign.commands.MenuCommand;
import me.desht.scrollingmenusign.commands.PageCommand;
import me.desht.scrollingmenusign.commands.ReloadCommand;
import me.desht.scrollingmenusign.commands.RemoveItemCommand;
import me.desht.scrollingmenusign.commands.RemoveMacroCommand;
import me.desht.scrollingmenusign.commands.RemoveViewCommand;
import me.desht.scrollingmenusign.commands.SaveCommand;
import me.desht.scrollingmenusign.commands.SetConfigCommand;
import me.desht.scrollingmenusign.commands.UndeleteMenuCommand;
import me.desht.scrollingmenusign.commands.VarCommand;
import me.desht.scrollingmenusign.commands.ViewCommand;
import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.scrollingmenusign.listeners.SMSBlockListener;
import me.desht.scrollingmenusign.listeners.SMSEntityListener;
import me.desht.scrollingmenusign.listeners.SMSPlayerListener;
import me.desht.scrollingmenusign.listeners.SMSSpoutKeyListener;
import me.desht.scrollingmenusign.listeners.SMSWorldListener;
import me.desht.scrollingmenusign.parser.CommandParser;
import me.desht.scrollingmenusign.spout.SpoutUtils;
import me.desht.scrollingmenusign.variables.VariablesManager;
import me.desht.scrollingmenusign.views.*;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.mcstats.Metrics;
import org.mcstats.Metrics.Graph;
import org.mcstats.Metrics.Plotter;

import com.comphenix.protocol.ProtocolLibrary;

/**
 * ScrollingMenuSign
 *
 * @author desht
 */
public class ScrollingMenuSign extends JavaPlugin implements ConfigurationListener {

    public static final int BLOCK_TARGET_DIST = 4;
    public static final String CONSOLE_OWNER = "[console]";

    private static ScrollingMenuSign instance = null;

    public static Economy economy = null;
    public static Permission permission = null;

    private final SMSHandlerImpl handler = new SMSHandlerImpl();
    private final CommandManager cmds = new CommandManager(this);
    private final CommandletManager cmdlets = new CommandletManager(this);
    private final ViewManager viewManager = new ViewManager(this);
    private final LocationManager locationManager = new LocationManager();
    private VariablesManager variablesManager = new VariablesManager(this);

    private boolean spoutEnabled = false;

    private ConfigurationManager configManager;

    public final ResponseHandler responseHandler = new ResponseHandler(this);
    private boolean protocolLibEnabled = false;
    private MetaFaker faker;

    @Override
    public void onLoad() {
        ConfigurationSerialization.registerClass(PersistableLocation.class);
    }

    @Override
    public void onEnable() {
        setInstance(this);

        LogUtils.init(this);

        DirectoryStructure.setupDirectoryStructure();

        configManager = new ConfigurationManager(this, this);
        configManager.setPrefix("sms");

        configCleanup();

        MiscUtil.init(this);
        MiscUtil.setColouredConsole(getConfig().getBoolean("sms.coloured_console"));

        Debugger.getInstance().setPrefix("[SMS] ");
        Debugger.getInstance().setLevel(getConfig().getInt("sms.debug_level"));
        Debugger.getInstance().setTarget(getServer().getConsoleSender());

        PluginManager pm = getServer().getPluginManager();
        setupSpout(pm);
        setupVault(pm);
        setupProtocolLib(pm);

        setupCustomFonts();

        new SMSPlayerListener(this);
        new SMSBlockListener(this);
        new SMSEntityListener(this);
        new SMSWorldListener(this);
        if (spoutEnabled) {
            new SMSSpoutKeyListener(this);
        }

        registerCommands();
        registerCommandlets();

        MessagePager.setPageCmd("/sms page [#|n|p]");
        MessagePager.setDefaultPageSize(getConfig().getInt("sms.pager.lines", 0));

        SMSScrollableView.setDefaultScrollType(SMSScrollableView.ScrollType.valueOf(getConfig().getString("sms.scroll_type").toUpperCase()));

        loadPersistedData();
        variablesManager.checkForUUIDMigration();

        if (spoutEnabled) {
            SpoutUtils.precacheTextures();
        }
        if (protocolLibEnabled) {
            ItemGlow.init(this);
            setupItemMetaFaker();
        }

        setupMetrics();

        Debugger.getInstance().debug(getDescription().getName() + " version " + getDescription().getVersion() + " is enabled!");

        UUIDMigration.migrateToUUID(this);
    }

    @Override
    public void onDisable() {
        SMSPersistence.saveMenusAndViews();
        SMSPersistence.saveMacros();
        SMSPersistence.saveVariables();
        for (SMSMenu menu : SMSMenu.listMenus()) {
            // this also deletes all the menu's views...
            menu.deleteTemporary();
        }
        for (SMSMacro macro : SMSMacro.listMacros()) {
            macro.deleteTemporary();
        }

        if (faker != null) {
            faker.shutdown();
        }

        economy = null;
        permission = null;
        setInstance(null);

        Debugger.getInstance().debug(getDescription().getName() + " version " + getDescription().getVersion() + " is disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return cmds.dispatch(sender, command, label, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return cmds.onTabComplete(sender, command, label, args);
    }

    public SMSHandler getHandler() {
        return handler;
    }

    public boolean isSpoutEnabled() {
        return spoutEnabled;
    }

    public boolean isProtocolLibEnabled() {
        return protocolLibEnabled;
    }

    public static ScrollingMenuSign getInstance() {
        return instance;
    }

    public CommandletManager getCommandletManager() {
        return cmdlets;
    }

    public ConfigurationManager getConfigManager() {
        return configManager;
    }

    /**
     * @return the viewManager
     */
    public ViewManager getViewManager() {
        return viewManager;
    }

    /**
     * @return the locationManager
     */
    public LocationManager getLocationManager() {
        return locationManager;
    }

    private void setupMetrics() {
        if (!getConfig().getBoolean("sms.mcstats")) {
            return;
        }

        try {
            Metrics metrics = new Metrics(this);

            Graph graphM = metrics.createGraph("Menu/View/Macro count");
            graphM.addPlotter(new Plotter("Menus") {
                @Override
                public int getValue() {
                    return SMSMenu.listMenus().size();
                }
            });
            graphM.addPlotter(new Plotter("Views") {
                @Override
                public int getValue() {
                    return viewManager.listViews().size();
                }
            });
            graphM.addPlotter(new Plotter("Macros") {
                @Override
                public int getValue() {
                    return SMSMacro.listMacros().size();
                }
            });

            Graph graphV = metrics.createGraph("View Types");
            for (final Entry<String, Integer> e : viewManager.getViewCounts().entrySet()) {
                graphV.addPlotter(new Plotter(e.getKey()) {
                    @Override
                    public int getValue() {
                        return e.getValue();
                    }
                });
            }
            metrics.start();
        } catch (IOException e) {
            LogUtils.warning("Can't submit metrics data: " + e.getMessage());
        }
    }

    private static void setInstance(ScrollingMenuSign plugin) {
        instance = plugin;
    }

    private void setupSpout(PluginManager pm) {
        Plugin spout = pm.getPlugin("Spout");
        if (spout != null && spout.isEnabled()) {
            spoutEnabled = true;
            Debugger.getInstance().debug("Hooked Spout v" + spout.getDescription().getVersion());
        }
    }

    private void setupVault(PluginManager pm) {
        Plugin vault = pm.getPlugin("Vault");
        if (vault != null && vault instanceof net.milkbowl.vault.Vault && vault.isEnabled()) {
            Debugger.getInstance().debug("Hooked Vault v" + vault.getDescription().getVersion());
            if (!setupEconomy()) {
                LogUtils.warning("No economy plugin detected - economy command costs not available");
            }
            if (!setupPermission()) {
                LogUtils.warning("No permissions plugin detected - no permission elevation support");
            }
        } else {
            LogUtils.warning("Vault not loaded: no economy support & no permission elevation support");
        }
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
            EconomyCost.setEconomy(economy);
        }

        return economy != null;
    }

    private boolean setupPermission() {
        RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(Permission.class);
        if (permissionProvider != null) {
            permission = permissionProvider.getProvider();
        }

        return permission != null;
    }

    private void setupProtocolLib(PluginManager pm) {
        Plugin pLib = pm.getPlugin("ProtocolLib");
        if (pLib != null && pLib instanceof ProtocolLibrary && pLib.isEnabled()) {
            protocolLibEnabled = true;
            Debugger.getInstance().debug("Hooked ProtocolLib v" + pLib.getDescription().getVersion());
        }
    }

    private void setupItemMetaFaker() {
        faker = new MetaFaker(this, new MetadataFilter() {
            @Override
            public ItemMeta filter(ItemMeta itemMeta, Player player) {
                if (ActiveItem.isActiveItem(itemMeta)) {
                    List<String> newLore = new ArrayList<String>(itemMeta.getLore());
                    newLore.remove(newLore.size() - 1);
                    ItemMeta newMeta = itemMeta.clone();
                    newMeta.setLore(newLore);
                    return newMeta;
                } else {
                    return null;
                }
            }
        });
    }

    private void registerCommands() {
        cmds.registerCommand(new AddItemCommand());
        cmds.registerCommand(new AddMacroCommand());
        cmds.registerCommand(new AddViewCommand());
        cmds.registerCommand(new CreateMenuCommand());
        cmds.registerCommand(new DeleteMenuCommand());
        cmds.registerCommand(new EditMenuCommand());
        cmds.registerCommand(new FontCommand());
        cmds.registerCommand(new GetConfigCommand());
        cmds.registerCommand(new GiveCommand());
        cmds.registerCommand(new ItemUseCommand());
        cmds.registerCommand(new ListMacroCommand());
        cmds.registerCommand(new ListMenusCommand());
        cmds.registerCommand(new MenuCommand());
        cmds.registerCommand(new PageCommand());
        cmds.registerCommand(new ReloadCommand());
        cmds.registerCommand(new RemoveItemCommand());
        cmds.registerCommand(new RemoveMacroCommand());
        cmds.registerCommand(new RemoveViewCommand());
        cmds.registerCommand(new SaveCommand());
        cmds.registerCommand(new SetConfigCommand());
        cmds.registerCommand(new UndeleteMenuCommand());
        cmds.registerCommand(new VarCommand());
        cmds.registerCommand(new ViewCommand());
    }

    private void registerCommandlets() {
        cmdlets.registerCommandlet(new AfterCommandlet());
        cmdlets.registerCommandlet(new CooldownCommandlet());
        cmdlets.registerCommandlet(new PopupCommandlet());
        cmdlets.registerCommandlet(new SubmenuCommandlet());
        cmdlets.registerCommandlet(new CloseSubmenuCommandlet());
        cmdlets.registerCommandlet(new ScriptCommandlet());
        cmdlets.registerCommandlet(new QuickMessageCommandlet());
    }

    private void loadPersistedData() {
        SMSPersistence.loadMacros();
        SMSPersistence.loadVariables();
        SMSPersistence.loadMenus();
        SMSPersistence.loadViews();
    }

    public static URL makeImageURL(String path) throws MalformedURLException {
        if (path == null || path.isEmpty()) {
            throw new MalformedURLException("file must be non-null and not an empty string");
        }

        return makeImageURL(ScrollingMenuSign.getInstance().getConfig().getString("sms.resource_base_url"), path);
    }

    public static URL makeImageURL(String base, String path) throws MalformedURLException {
        if (path == null || path.isEmpty()) {
            throw new MalformedURLException("file must be non-null and not an empty string");
        }
        if ((base == null || base.isEmpty()) && !path.startsWith("http:")) {
            throw new MalformedURLException("base URL must be set (use /sms setcfg resource_base_url ...");
        }
        if (path.startsWith("http:") || base == null) {
            return new URL(path);
        } else {
            return new URL(new URL(base), path);
        }
    }

    @Override
    public void onConfigurationValidate(ConfigurationManager configurationManager, String key, Object oldVal, Object newVal) {
        if (key.equals("scroll_type")) {
            try {
                SMSScrollableView.ScrollType t = SMSGlobalScrollableView.ScrollType.valueOf(newVal.toString().toUpperCase());
                DHValidate.isTrue(t != SMSGlobalScrollableView.ScrollType.DEFAULT, "Scroll type must be one of SCROLL/PAGE");
            } catch (IllegalArgumentException e) {
                throw new DHUtilsException("Scroll type must be one of SCROLL/PAGE");
            }
        } else if (key.equals("debug_level")) {
            DHValidate.isTrue((Integer) newVal >= 0, "Debug level must be >= 0");
        }
    }

    @Override
    public void onConfigurationChanged(ConfigurationManager configurationManager, String key, Object oldVal, Object newVal) {
        if (key.startsWith("actions.spout") && isSpoutEnabled()) {
            // reload & re-cache spout key definitions
            SpoutUtils.loadKeyDefinitions();
        } else if (key.startsWith("spout.") && isSpoutEnabled()) {
            // settings which affects how spout views are drawn
            repaintViews("spout");
        } else if (key.equalsIgnoreCase("command_log_file")) {
            CommandParser.setLogFile(newVal.toString());
        } else if (key.equalsIgnoreCase("debug_level")) {
            Debugger.getInstance().setLevel((Integer) newVal);
        } else if (key.startsWith("item_prefix.") || key.endsWith("_justify") || key.equals("max_title_lines") || key.startsWith("submenus.")) {
            // settings which affect how all views are drawn
            repaintViews(null);
        } else if (key.equals("coloured_console")) {
            MiscUtil.setColouredConsole((Boolean) newVal);
        } else if (key.equals("scroll_type")) {
            SMSScrollableView.setDefaultScrollType(SMSGlobalScrollableView.ScrollType.valueOf(newVal.toString().toUpperCase()));
            repaintViews(null);
        }
    }

    private void repaintViews(String type) {
        for (SMSView v : viewManager.listViews()) {
            if (type == null || v.getType().equals(type)) {
                v.update(null, SMSMenuAction.REPAINT);
            }
        }
    }

    public void setupCustomFonts() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

        for (File f : DirectoryStructure.getFontsFolder().listFiles()) {
            String n = f.getName().toLowerCase();
            int type;
            if (n.endsWith(".ttf")) {
                type = Font.TRUETYPE_FONT;
            } else if (n.endsWith(".pfa") || n.endsWith(".pfb") || n.endsWith(".pfm") || n.endsWith(".afm")) {
                type = Font.TYPE1_FONT;
            } else {
                continue;
            }
            try {
                ge.registerFont(Font.createFont(type, f));
                Debugger.getInstance().debug("registered font: " + f.getName());
            } catch (Exception e) {
                LogUtils.warning("can't load custom font " + f + ": " + e.getMessage());
            }
        }
    }

    private void configCleanup() {
        String[] obsolete = new String[]{
                "sms.maps.break_block_id", "sms.autosave", "sms.menuitem_separator",
                "sms.persistent_user_vars", "uservar",
        };

        boolean changed = false;
        Configuration config = getConfig();
        for (String k : obsolete) {
            if (config.contains(k)) {
                config.set(k, null);
                LogUtils.info("removed obsolete config item: " + k);
                changed = true;
            }
        }
        if (changed) {
            saveConfig();
        }
    }

    public VariablesManager getVariablesManager() {
        return variablesManager;
    }
}
