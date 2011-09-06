package me.desht.scrollingmenusign;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.logging.Level;

import me.desht.scrollingmenusign.commands.AddItemCommand;
import me.desht.scrollingmenusign.commands.AddMacroCommand;
import me.desht.scrollingmenusign.commands.AddViewCommand;
import me.desht.scrollingmenusign.commands.CommandManager;
import me.desht.scrollingmenusign.commands.CreateMenuCommand;
import me.desht.scrollingmenusign.commands.DebugCommand;
import me.desht.scrollingmenusign.commands.DefaultCmdCommand;
import me.desht.scrollingmenusign.commands.DeleteMenuCommand;
import me.desht.scrollingmenusign.commands.GetConfigCommand;
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
import me.desht.scrollingmenusign.listeners.SMSBlockListener;
import me.desht.scrollingmenusign.listeners.SMSEntityListener;
import me.desht.scrollingmenusign.listeners.SMSPlayerListener;
import me.desht.scrollingmenusign.listeners.SMSServerListener;
import me.desht.util.Debugger;
import me.desht.util.MessagePager;
import me.desht.util.MiscUtil;
import me.desht.util.PermissionsUtils;

import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import com.LRFLEW.register.payment.Method;

public class ScrollingMenuSign extends JavaPlugin {
	private static PluginDescriptionFile description;

	private final SMSPlayerListener signListener = new SMSPlayerListener(this);
	private final SMSBlockListener blockListener = new SMSBlockListener(this);
	private final SMSEntityListener entityListener = new SMSEntityListener(this);
	private final SMSHandlerImpl handler = new SMSHandlerImpl();
	private SMSServerListener serverListener;
	private final CommandManager cmds = new CommandManager(this);
	
	private static Method economy = null;
	
	private final Debugger debugger = new Debugger();
	
	@Override
	public void onEnable() {
		description = this.getDescription();
		
		SMSPersistence.init();
		SMSConfig.init(this);

		if (!loadRegister())
			return;

		PermissionsUtils.setup();
		SMSCommandSigns.setup();
		
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Event.Type.PLAYER_INTERACT, signListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_ITEM_HELD, signListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_DAMAGE, blockListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_BREAK, blockListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_PHYSICS, blockListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.ENTITY_EXPLODE, entityListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.PLUGIN_ENABLE, serverListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.PLUGIN_DISABLE, serverListener, Event.Priority.Normal, this);
		
		registerCommands();
		
		loadMacros();
		
		MessagePager.setPageCmd("/sms page [#|n|p]");
		
		// delayed loading of saved menu files to ensure all worlds are loaded first
		if (getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
			@Override
			public void run() {
				loadMenusAndViews();
			}
		})==-1) {
			MiscUtil.log(Level.WARNING, "Couldn't schedule menu loading - multiworld support might not work.");
			loadMenusAndViews();
		}

		MiscUtil.log(Level.INFO, description.getName() + " version " + description.getVersion() + " is enabled!" );
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
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Player player = null;
		if (sender instanceof Player) {
			player = (Player) sender;
		}
		try {
			return cmds.dispatch(player, label, args);
		} catch (SMSException e) {
			MiscUtil.errorMessage(player, e.getMessage());
			return true;
		}
	}
	
	@Override
	public void onDisable() {
		saveMenus();
		saveMacros();
		MiscUtil.log(Level.INFO, description.getName() + " version " + description.getVersion() + " is disabled!" );
	}

	public void loadMenusAndViews() {
		SMSPersistence.loadAll();
	}
	
	public void loadMacros() {
		SMSMacro.loadCommands();
	}
	
	public void saveMenus() {
		SMSPersistence.saveAll();
	}
	
	public void saveMacros() {
		SMSMacro.saveCommands();
	}
	
	public void debug(String message) {
		getDebugger().debug(message);
	}
	
	public SMSHandler getHandler() {
		return handler;
	}

	public Debugger getDebugger() {
		return debugger;
	}

	public static void setEconomy(Method economy) {
		ScrollingMenuSign.economy = economy;
	}

	public static Method getEconomy() {
		return economy;
	}

	private boolean loadRegister() {
		try {
            Class.forName("com.LRFLEW.register.payment.Methods");
            serverListener = new SMSServerListener(this);
            return true;
        } catch (ClassNotFoundException e) {
            try {
                MiscUtil.log(Level.INFO, "[ScrollingMenuSign] Register library not found! Downloading...");
                if (!new File("lib").isDirectory())
                    if (!new File("lib").mkdir())
                        MiscUtil.log(Level.SEVERE, "[ScrollingMenuSign] Error creating lib directory. Please make sure Craftbukkit has permissions to write to the Minecraft directory and there is no file named \"lib\" in that location.");
                URL Register = new URL("https://github.com/iConomy/Register/raw/master/dist/_Register.jar");
                ReadableByteChannel rbc = Channels.newChannel(Register.openStream());
                FileOutputStream fos = new FileOutputStream("lib/Register.jar");
                fos.getChannel().transferFrom(rbc, 0, 1 << 24);
                MiscUtil.log(Level.INFO, "[ScrollingMenuSign] Register library downloaded. Server reboot required to load.");
            } catch (MalformedURLException ex) {
                MiscUtil.log(Level.WARNING, "[ScrollingMenuSign] Error accessing Register lib URL: " + ex);
            } catch (FileNotFoundException ex) {
                MiscUtil.log(Level.WARNING, "[ScrollingMenuSign] Error accessing Register lib URL: " + ex);
            } catch (IOException ex) {
                MiscUtil.log(Level.WARNING, "[ScrollingMenuSign] Error downloading Register lib: " + ex);
            } finally {
                getServer().getPluginManager().disablePlugin(this);
            }
            return false;
        }
    }
}
