package me.desht.scrollingmenusign;

import java.util.logging.Level;

import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Event;

public class ScrollingMenuSign extends JavaPlugin {
	private static PluginDescriptionFile description;

	private final SMSPlayerListener signListener = new SMSPlayerListener(this);
	private final SMSBlockListener blockListener = new SMSBlockListener(this);
	private final SMSCommandExecutor commandExecutor = new SMSCommandExecutor(this);
	private final SMSPersistence persistence = new SMSPersistence(this);
	private final SMSEntityListener entityListener = new SMSEntityListener(this);
	private final SMSHandlerImpl handler = new SMSHandlerImpl(this);
	
	final SMSDebugger debugger = new SMSDebugger(this);
	
	@Override
	public void onEnable() {
		description = this.getDescription();

		SMSConfig.init(this);

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
		
		loadMacros();
		
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

		SMSUtils.log(Level.INFO, description.getName() + " version " + description.getVersion() + " is enabled!" );
	}

	@Override
	public void onDisable() {
		saveMenus();
		saveMacros();
		SMSUtils.log(Level.INFO, description.getName() + " version " + description.getVersion() + " is disabled!" );
	}

	void loadMenus() {
		persistence.loadAll();
	}
	
	void loadMacros() {
		SMSMacro.loadCommands();
	}
	
	void saveMenus() {
		persistence.saveAll();
	}
	
	void saveMacros() {
		SMSMacro.saveCommands();
	}
	
	void debug(String message) {
		debugger.debug(message);
	}
	
	SMSPersistence getPersistence() {
		return persistence;
	}
	
	public SMSHandler getHandler() {
		return handler;
	}
}
