package me.desht.scrollingmenusign;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.desht.scrollingmenusign.Metrics.Graph;
import me.desht.scrollingmenusign.Metrics.Plotter;
import me.desht.scrollingmenusign.commands.AddItemCommand;
import me.desht.scrollingmenusign.commands.AddMacroCommand;
import me.desht.scrollingmenusign.commands.AddViewCommand;
import me.desht.scrollingmenusign.commands.CommandManager;
import me.desht.scrollingmenusign.commands.CreateMenuCommand;
import me.desht.scrollingmenusign.commands.DebugCommand;
import me.desht.scrollingmenusign.commands.DefaultCmdCommand;
import me.desht.scrollingmenusign.commands.DeleteMenuCommand;
import me.desht.scrollingmenusign.commands.GetConfigCommand;
import me.desht.scrollingmenusign.commands.GiveMapCommand;
import me.desht.scrollingmenusign.commands.ItemUseCommand;
import me.desht.scrollingmenusign.commands.ListMacroCommand;
import me.desht.scrollingmenusign.commands.ListMenusCommand;
import me.desht.scrollingmenusign.commands.MenuTitleCommand;
import me.desht.scrollingmenusign.commands.PageCommand;
import me.desht.scrollingmenusign.commands.ReloadCommand;
import me.desht.scrollingmenusign.commands.RemoveItemCommand;
import me.desht.scrollingmenusign.commands.RemoveMacroCommand;
import me.desht.scrollingmenusign.commands.RemoveViewCommand;
import me.desht.scrollingmenusign.commands.SaveCommand;
import me.desht.scrollingmenusign.commands.SetConfigCommand;
import me.desht.scrollingmenusign.commands.ShowMenuCommand;
import me.desht.scrollingmenusign.commands.SortMenuCommand;
import me.desht.scrollingmenusign.commands.VarCommand;
import me.desht.scrollingmenusign.commands.ViewCommand;
import me.desht.scrollingmenusign.expector.ResponseHandler;
import me.desht.scrollingmenusign.listeners.SMSBlockListener;
import me.desht.scrollingmenusign.listeners.SMSEntityListener;
import me.desht.scrollingmenusign.listeners.SMSPlayerListener;
import me.desht.scrollingmenusign.listeners.SMSSpoutKeyListener;
import me.desht.scrollingmenusign.listeners.SMSSpoutScreenListener;
import me.desht.scrollingmenusign.listeners.SMSWorldListener;
import me.desht.scrollingmenusign.spout.SpoutUtils;
import me.desht.scrollingmenusign.util.MessagePager;
import me.desht.scrollingmenusign.util.MiscUtil;
import me.desht.scrollingmenusign.util.SMSLogger;
import me.desht.scrollingmenusign.views.SMSView;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ScrollingMenuSign extends JavaPlugin {

	private final SMSPlayerListener playerListener = new SMSPlayerListener();
	private final SMSBlockListener blockListener = new SMSBlockListener();
	private final SMSEntityListener entityListener = new SMSEntityListener();
	private final SMSWorldListener worldListener = new SMSWorldListener();
	private final SMSHandlerImpl handler = new SMSHandlerImpl();
	private final CommandManager cmds = new CommandManager(this);
	private SMSSpoutKeyListener spoutKeyListener;
	private SMSSpoutScreenListener spoutScreenListener;
	private boolean spoutEnabled = false;
	private static ScrollingMenuSign instance = null;

	public static Economy economy = null;
	public static Permission permission = null;
	
	public final ResponseHandler responseHandler = new ResponseHandler();

	@Override
	public void onEnable() {
		setInstance(this);

		SMSLogger.init(this);
		
		PluginManager pm = getServer().getPluginManager();

		if (!validateVersions(getDescription().getVersion(), getServer().getVersion())) {		
			pm.disablePlugin(this);
			return;
		}	

		setupSpout(pm);
		setupVault(pm);

		SMSConfig.init(this);

		pm.registerEvents(playerListener, this);
		pm.registerEvents(blockListener, this);
		pm.registerEvents(entityListener, this);
		pm.registerEvents(worldListener, this);
		
		if (spoutEnabled) {
			spoutKeyListener = new SMSSpoutKeyListener();
			spoutScreenListener = new SMSSpoutScreenListener();
			pm.registerEvents(spoutKeyListener, this);
			pm.registerEvents(spoutScreenListener, this);
		}

		registerCommands();

		MessagePager.setPageCmd("/sms page [#|n|p]");

		loadPersistedData();

		if (spoutEnabled) {
			SpoutUtils.precacheTextures();
		}
		
		setupMetrics();
		
		MiscUtil.log(Level.INFO, getDescription().getName() + " version " + getDescription().getVersion() + " is enabled!" );
	}

	private void setupMetrics() {
		try {
			Metrics metrics = new Metrics(this);
			
			metrics.createGraph("Menu count").addPlotter(new Plotter() {
				@Override
				public int getValue() {
					return SMSMenu.listMenus().size();
				}
			});
			metrics.createGraph("Macro count").addPlotter(new Plotter() {
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
			SMSLogger.warning("Can't submit metrics data: " + e.getMessage());
		}
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

		MiscUtil.log(Level.INFO, getDescription().getName() + " version " + getDescription().getVersion() + " is disabled!" );
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Player player = null;
		if (sender instanceof Player) {
			player = (Player) sender;
		}
		try {
			return cmds.dispatch(player, command.getName(), args);
		} catch (SMSException e) {
			MiscUtil.errorMessage(player, e.getMessage());
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

	private static void setInstance(ScrollingMenuSign plugin) {
		instance = plugin;
	}

	private void setupSpout(PluginManager pm) {
		Plugin spout = pm.getPlugin("Spout");
		if (spout != null && spout.isEnabled()) {
			spoutEnabled = true;
			MiscUtil.log(Level.INFO, "Loaded Spout v" + spout.getDescription().getVersion());
		}
	}

	private void setupVault(PluginManager pm) {
		Plugin vault =  pm.getPlugin("Vault");
		if (vault != null && vault instanceof net.milkbowl.vault.Vault) {
			MiscUtil.log(Level.INFO, "Loaded Vault v" + vault.getDescription().getVersion());
			if (!setupEconomy()) {
				MiscUtil.log(Level.WARNING, "No economy plugin detected - economy command costs not available");
			}
			if (!setupPermission()) {
				MiscUtil.log(Level.WARNING, "No permissions plugin detected - no permission elevation support");
			}
		} else {
			MiscUtil.log(Level.WARNING, "Vault not loaded: no economy support & no permission elevation support");
		}
	}

	private boolean setupEconomy() {
		RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
		if (economyProvider != null) {
			economy = economyProvider.getProvider();
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
		cmds.registerCommand(new GiveMapCommand());
		cmds.registerCommand(new ItemUseCommand());
		cmds.registerCommand(new ListMacroCommand());
		cmds.registerCommand(new ListMenusCommand());
		cmds.registerCommand(new MenuTitleCommand());
		cmds.registerCommand(new PageCommand());
		cmds.registerCommand(new ReloadCommand());
		cmds.registerCommand(new RemoveItemCommand());
		cmds.registerCommand(new RemoveMacroCommand());
		cmds.registerCommand(new RemoveViewCommand());
		cmds.registerCommand(new SaveCommand());
		cmds.registerCommand(new SetConfigCommand());
		cmds.registerCommand(new ShowMenuCommand());
		cmds.registerCommand(new SortMenuCommand());
		cmds.registerCommand(new VarCommand());
		cmds.registerCommand(new ViewCommand());
	}

	private void loadPersistedData() {
		SMSPersistence.loadMacros();
		SMSPersistence.loadMenusAndViews();
	}

	private static boolean validateVersions(String pVer, String cbVer) {
		Pattern pat = Pattern.compile("b([0-9]+)jnks");
		Matcher m = pat.matcher(cbVer);
		int pluginBuild = getRelease(pVer);
		int bukkitBuild;
		if (m.find()) {
			bukkitBuild = Integer.parseInt(m.group(1));
		} else {
			MiscUtil.log(Level.WARNING, "Can't determine build number for CraftBukkit from " + cbVer);
			return true;
		}
		if (pluginBuild < 7000 && bukkitBuild >= 1093) {
			notCompatible(pVer, bukkitBuild, "0.7 or later");
			return false;
		} else if (pluginBuild >= 7000 && bukkitBuild < 1093) {
			notCompatible(pVer, bukkitBuild, "0.6.x earlier");
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Get the internal version number for the given string version, which is
	 * <major> * 1,000,000 + <minor> * 1,000 + <release>.  This assumes minor and
	 * release each won't go above 999, hopefully a safe assumption!
	 * 
	 * @param oldVersion
	 * @return
	 */
	private static int getRelease(String ver) {
		String[] a = ver.split("\\.");
		try {
			int major = Integer.parseInt(a[0]);
			int minor;
			int rel;
			if (a.length < 2) {
				minor = 0;
			} else {
				minor = Integer.parseInt(a[1]);
			}
			if (a.length < 3) {
				rel = 0;
			} else {
				rel = Integer.parseInt(a[2]);
			}
			return major * 1000000 + minor * 1000 + rel;
		} catch (NumberFormatException e) {
			MiscUtil.log(Level.WARNING, "Version string [" + ver + "] doesn't look right!");
			return 0;
		}
	}

	private static void notCompatible(String pVer, int bukkitBuild, String needed) {
		MiscUtil.log(Level.SEVERE, "ScrollingMenuSign v" + pVer + " is not compatible with CraftBukkit " + bukkitBuild + " - plugin disabled");
		MiscUtil.log(Level.SEVERE, "You need to use ScrollingMenuSign v" + needed);
	}
	
	public static URL makeImageURL(String path) throws MalformedURLException {
		if (path == null || path.isEmpty()) {
			throw new MalformedURLException("file must be non-null and not an empty string");
		}
		
		return makeImageURL(SMSConfig.getConfig().getString("sms.resource_base_url"), path);
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
}
