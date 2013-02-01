package me.desht.scrollingmenusign;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map.Entry;

import me.desht.dhutils.ConfigurationListener;
import me.desht.dhutils.ConfigurationManager;
import me.desht.dhutils.Cost;
import me.desht.dhutils.DHUtilsException;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MessagePager;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PersistableLocation;
import me.desht.dhutils.commands.CommandManager;
import me.desht.dhutils.responsehandler.ResponseHandler;
import me.desht.scrollingmenusign.commandlets.CloseSubmenuCommandlet;
import me.desht.scrollingmenusign.commandlets.CommandletManager;
import me.desht.scrollingmenusign.commandlets.PopupCommandlet;
import me.desht.scrollingmenusign.commandlets.SubmenuCommandlet;
import me.desht.scrollingmenusign.commands.AddItemCommand;
import me.desht.scrollingmenusign.commands.AddMacroCommand;
import me.desht.scrollingmenusign.commands.AddViewCommand;
import me.desht.scrollingmenusign.commands.CreateMenuCommand;
import me.desht.scrollingmenusign.commands.DebugCommand;
import me.desht.scrollingmenusign.commands.DefaultCmdCommand;
import me.desht.scrollingmenusign.commands.DeleteMenuCommand;
import me.desht.scrollingmenusign.commands.GetConfigCommand;
import me.desht.scrollingmenusign.commands.GiveCommand;
import me.desht.scrollingmenusign.commands.ItemUseCommand;
import me.desht.scrollingmenusign.commands.ListMacroCommand;
import me.desht.scrollingmenusign.commands.ListMenusCommand;
import me.desht.scrollingmenusign.commands.MenuTitleCommand;
import me.desht.scrollingmenusign.commands.PageCommand;
import me.desht.scrollingmenusign.commands.ReloadCommand;
import me.desht.scrollingmenusign.commands.RemoveItemCommand;
import me.desht.scrollingmenusign.commands.RemoveMacroCommand;
import me.desht.scrollingmenusign.commands.RemoveViewCommand;
import me.desht.scrollingmenusign.commands.EditMenuCommand;
import me.desht.scrollingmenusign.commands.SaveCommand;
import me.desht.scrollingmenusign.commands.SetConfigCommand;
import me.desht.scrollingmenusign.commands.ShowMenuCommand;
import me.desht.scrollingmenusign.commands.SortMenuCommand;
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
import me.desht.scrollingmenusign.views.SMSView;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.mcstats.Metrics;
import org.mcstats.Metrics.Graph;
import org.mcstats.Metrics.Plotter;

public class ScrollingMenuSign extends JavaPlugin implements ConfigurationListener {

	public static final int BLOCK_TARGET_DIST = 4;

	private static ScrollingMenuSign instance = null;
	
	public static Economy economy = null;
	public static Permission permission = null;
	
	private final SMSHandlerImpl handler = new SMSHandlerImpl();
	private final CommandManager cmds = new CommandManager(this);
	private final CommandletManager cmdlets = new CommandletManager(this);
	
	private boolean spoutEnabled = false;
	private ConfigurationManager configManager;

	public final ResponseHandler responseHandler = new ResponseHandler();

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
		
		LogUtils.setLogLevel(getConfig().getString("sms.log_level", "INFO"));
		
		PluginManager pm = getServer().getPluginManager();
		setupSpout(pm);
		setupVault(pm);

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

		loadPersistedData();

		if (spoutEnabled) {
			SpoutUtils.precacheTextures();
		}

		setupMetrics();

		LogUtils.fine(getDescription().getName() + " version " + getDescription().getVersion() + " is enabled!" );
	}

	@Override
	public void onDisable() {
		SMSPersistence.saveMenusAndViews();
		SMSPersistence.saveMacros();
		for (SMSMenu menu : SMSMenu.listMenus()) {
			// this also deletes all the menu's views...
			menu.deleteTemporary();
		}
		for (SMSMacro macro : SMSMacro.listMacros()) {
			macro.deleteTemporary();
		}

		economy = null;
		permission = null;
		setInstance(null);

		LogUtils.fine(getDescription().getName() + " version " + getDescription().getVersion() + " is disabled!" );
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		try {
			return cmds.dispatch(sender, command.getName(), args);
		} catch (DHUtilsException e) {
			MiscUtil.errorMessage(sender, e.getMessage());
			return true;
		}
	}

	public SMSHandler getHandler() {
		return handler;
	}

	public boolean isSpoutEnabled() {
		return spoutEnabled;
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
					return SMSView.listViews().size();
				}
			});
			graphM.addPlotter(new Plotter("Macros") {
				@Override
				public int getValue() {
					return SMSMacro.listMacros().size();
				}
			});
			
			Graph graphV = metrics.createGraph("View Types");
			for (final Entry<String,Integer> e : SMSView.getViewCounts().entrySet()) {
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
			LogUtils.fine("Loaded Spout v" + spout.getDescription().getVersion());
		}
	}

	private void setupVault(PluginManager pm) {
		Plugin vault =  pm.getPlugin("Vault");
		if (vault != null && vault instanceof net.milkbowl.vault.Vault) {
			LogUtils.fine("Loaded Vault v" + vault.getDescription().getVersion());
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
		RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
		if (economyProvider != null) {
			economy = economyProvider.getProvider();
			Cost.setEconomy(economy);
		}

		return (economy != null);
	}

	private boolean setupPermission() {
		RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
		if (permissionProvider != null) {
			permission = permissionProvider.getProvider();
		}

		return (permission != null);
	}


	private void registerCommands() {
		cmds.registerCommand(new AddItemCommand());
		cmds.registerCommand(new AddMacroCommand());
		cmds.registerCommand(new AddViewCommand());
		cmds.registerCommand(new CreateMenuCommand());
		cmds.registerCommand(new DebugCommand());
		cmds.registerCommand(new DefaultCmdCommand());
		cmds.registerCommand(new DeleteMenuCommand());
		cmds.registerCommand(new GetConfigCommand());
		cmds.registerCommand(new GiveCommand());
		cmds.registerCommand(new ItemUseCommand());
		cmds.registerCommand(new ListMacroCommand());
		cmds.registerCommand(new ListMenusCommand());
		cmds.registerCommand(new MenuTitleCommand());
		cmds.registerCommand(new PageCommand());
		cmds.registerCommand(new ReloadCommand());
		cmds.registerCommand(new RemoveItemCommand());
		cmds.registerCommand(new RemoveMacroCommand());
		cmds.registerCommand(new RemoveViewCommand());
		cmds.registerCommand(new EditMenuCommand());
		cmds.registerCommand(new SaveCommand());
		cmds.registerCommand(new SetConfigCommand());
		cmds.registerCommand(new ShowMenuCommand());
		cmds.registerCommand(new SortMenuCommand());
		cmds.registerCommand(new VarCommand());
		cmds.registerCommand(new ViewCommand());
	}

	private void registerCommandlets() {
		cmdlets.registerCommandlet("POPUP", new PopupCommandlet());
		cmdlets.registerCommandlet("SUBMENU", new SubmenuCommandlet());
		cmdlets.registerCommandlet("BACK", new CloseSubmenuCommandlet());
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
	public void onConfigurationValidate(ConfigurationManager configurationManager, String key, String val) {
		// do nothing
	}

	@Override
	public void onConfigurationValidate(ConfigurationManager configurationManager, String key, List<?> val) {
		// do nothing
	}

	@Override
	public void onConfigurationChanged(ConfigurationManager configurationManager, String key, Object oldVal,
			Object newVal) {
		if (key.equalsIgnoreCase("ignore_view_ownership")) {
			// redraw map views
			repaintViews("map");
		} else if (key.startsWith("actions.spout") && isSpoutEnabled()) {
			// reload & re-cache spout key definitions
			SpoutUtils.loadKeyDefinitions();
		} else if (key.startsWith("spout.") && isSpoutEnabled()) {
			// catch-all for any setting which affects how spout views are drawn
			repaintViews("spout");
		} else if (key.equalsIgnoreCase("command_log_file")) {
			CommandParser.setLogFile(newVal.toString());
		} else if (key.equalsIgnoreCase("log_level")) {
			LogUtils.setLogLevel(newVal.toString());
		} else if (key.startsWith("item_prefix.") || key.endsWith("_justify") || key.equals("max_title_lines")) {
			repaintViews(null);
		}
	}

	private void repaintViews(String type) {
		for (SMSView v : SMSView.listViews()) {
			if (type == null || v.getType().equals(type)) {
				v.update(null, SMSMenuAction.REPAINT);
			}
		}
	}
}
