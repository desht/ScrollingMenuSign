package me.desht.scrollingmenusign;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * @author desht
 *
 */
public class SMSVariables implements SMSPersistable {
	private static final Map<String,SMSVariables> allVariables = new HashMap<String, SMSVariables>();
	
	private final String playerName;
	private final Configuration variables;
	
	/**
	 * Private constructor.
	 * 
	 * @param playerName	The player who owns these variables
	 */
	private SMSVariables(String playerName) {
		this.playerName = playerName;
		variables = new MemoryConfiguration();
	}
	
	private void autosave() {
		SMSPersistence.save(this);
	}

	/**
	 * Set the given variable to the given value
	 * 
	 * @param varName	the variable name
	 * @param value		the value
	 */
	public void set(String varName, String value) {
		variables.set(varName, value);
		autosave();
	}
	
	/**
	 * Get the value of the given variable
	 * 
	 * @param varName	the variable name
	 * @return	the value, or null if the variable does not exist
	 */
	public String get(String varName) {
		return variables.getString(varName);
	}
	
	/**
	 * Checks if the given variable exists.
	 * 
	 * @param varName	the variable name
	 * @return	true if it exists, false otherwise
	 */
	public boolean isSet(String varName) {
		return variables.contains(varName);
	}
	
	/**
	 * Get a list of all variables fo
	 * @return
	 */
	public Set<String> getVariables() {
		return variables.getKeys(false);
	}

	private void deleteCommon() {
		allVariables.remove(playerName);
	}

	void deletePermanent() {
		deleteCommon();
		SMSPersistence.unPersist(this);
	}

	void deleteTemporary() {
		deleteCommon();
	}
	
	/* (non-Javadoc)
	 * @see me.desht.scrollingmenusign.SMSPersistable#getName()
	 */
	@Override
	public String getName() {
		return playerName;
	}

	/* (non-Javadoc)
	 * @see me.desht.scrollingmenusign.SMSPersistable#getSaveFolder()
	 */
	@Override
	public File getSaveFolder() {
		return DirectoryStructure.getVarsFolder();
	}

	/* (non-Javadoc)
	 * @see me.desht.scrollingmenusign.SMSPersistable#freeze()
	 */
	@Override
	public Map<String, Object> freeze() {
		Map<String,Object> map = new HashMap<String, Object>();
		for (String key : getVariables()) {
			map.put(key, get(key));
		}
		return map;
	}

	/**
	 * Get the variable collection for the given player.  If the player has no variables,
	 * a new empty SMSVariables collection will be created.
	 * 
	 * @param playerName	the player's name
	 * @return	an SMSVariables collection of variables for the player
	 */
	public static SMSVariables getVariables(String playerName) {
		if (!allVariables.containsKey(playerName)) {
			allVariables.put(playerName, new SMSVariables(playerName));
		}
		return allVariables.get(playerName);
	}

	/**
	 * Get a list of all the known SMSVariables collections
	 * 
	 * @return	a list of all the known SMSVariables collections
	 */
	public static Collection<SMSVariables> listVariables() {
		return allVariables.values();
	}

	static void load(File f) {
		YamlConfiguration conf = YamlConfiguration.loadConfiguration(f);
		String playerName = FilenameUtils.removeExtension(f.getName());
		SMSVariables vars = getVariables(playerName);
		
		for (String key : conf.getKeys(false)) {
			vars.set(key, conf.getString(key));
		}
	}
}
