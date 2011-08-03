package me.desht.scrollingmenusign;

import java.io.File;
import java.util.logging.Level;

import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.*;

//@SuppressWarnings("serial")
public class ScrollingMenuSign extends JavaPlugin {
	static enum MenuRemoveAction { DESTROY_SIGN, BLANK_SIGN, DO_NOTHING };
	private static PluginDescriptionFile description;

	private final SMSPlayerListener signListener = new SMSPlayerListener(this);
	private final SMSBlockListener blockListener = new SMSBlockListener(this);
	private final SMSCommandExecutor commandExecutor = new SMSCommandExecutor(this);
	private final SMSPersistence persistence = new SMSPersistence(this);
	private final SMSEntityListener entityListener = new SMSEntityListener(this);
	
	final SMSDebugger debugger = new SMSDebugger(this);
	final SMSMacro macroHandler = new SMSMacro(this);
	
	@Override
	public void onEnable() {
		description = this.getDescription();

		SMSConfig.configInitialise(this);

		SMSPermissions.setup();
		SMSCommandSigns.setup();
		
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Event.Type.PLAYER_INTERACT, signListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_ITEM_HELD, signListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_DAMAGE, blockListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_BREAK, blockListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_PHYSICS, blockListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.ENTITY_EXPLODE, entityListener, Event.Priority.Normal, this);

		getCommand("sms").setExecutor(commandExecutor);
		
		if (!getDataFolder().exists()) getDataFolder().mkdir();

		SMSUtils.log(Level.INFO, description.getName() + " version " + description.getVersion() + " is enabled!" );
		
		// delayed loading of saved menu files to ensure all worlds are loaded first
		if (getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
			@Override
			public void run() {
				loadMenus();
			}
		})==-1) {
			SMSUtils.log(Level.WARNING, "Couldn't schedule menu loading - multiworld support might not work.");
			loadMenus();
		}
		loadMacros();
	}

	@Override
	public void onDisable() {
		saveMenus();
		saveMacros();
		SMSUtils.log(Level.INFO, description.getName() + " version " + description.getVersion() + " is disabled!" );
	}

	void loadMenus() {
		persistence.load();
	}
	
	void loadMacros() {
		macroHandler.loadCommands();
	}
	
	void saveMenus() {
		persistence.save();
	}
	
	void saveMacros() {
		macroHandler.saveCommands();
	}
	
	void debug(String message) {
		debugger.debug(message);
	}
	
	void maybeSaveMenus() {
		if (getConfiguration().getBoolean("sms.autosave", false)) {
			saveMenus();
		}
	}
}
