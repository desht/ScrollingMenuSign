package me.desht.scrollingmenusign;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

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

	public String getPlayerName() {
		return playerName;
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
	 * Get the value of the given variable
	 * 
	 * @param varName	the variable name
	 * @param def	default value
	 * @return	the value, or null if the variable does not exist
	 */
	public String get(String varName, String def) {
		return variables.getString(varName, def);
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
	 * a new empty SMSVariables collection will be created iff autoCreate is true, otherwise
	 * an exception will be thrown.
	 * 
	 * @param playerName	the player's name
	 * @return	an SMSVariables collection of variables for the player
	 * @throws SMSException if autoCreate is false and the variables object does not exist
	 */
	public static SMSVariables getVariables(String playerName, boolean autoCreate) {
		if (!allVariables.containsKey(playerName)) {
			if (autoCreate) {
				allVariables.put(playerName, new SMSVariables(playerName));
			} else {
				throw new SMSException("No variables are defined for player " + playerName);
			}
		}
		return allVariables.get(playerName);
	}
	
	/**
	 * Check if any variables are defined for the given player.
	 * 
	 * @param playerName
	 * @return
	 */
	public static boolean hasVariables(String playerName) {
		return allVariables.containsKey(playerName);
	}

	/**
	 * Get a list of all the known SMSVariables collections
	 * 
	 * @return	a list of all the known SMSVariables collections
	 */
	public static Collection<SMSVariables> listVariables() {
		return listVariables(false);
	}

	/**
	 * Get the value of the given variable spec.  The spec may be a simple variable name or
	 * a player name followed by a period, followed by the variable name.
	 * 
	 * @param playerName	Player who is retrieving the variable
	 * @param varSpec		Variable specification
	 * @return				The variable value, or null if not set
	 */
	public static String getVar(String playerName, String varSpec) {
		return getVar(playerName, varSpec, null);
	}

	public static String getVar(String playerName, String varSpec, String def) {
		VarSpec vs = new VarSpec(playerName, varSpec);
		if (!hasVariables(vs.playerName)) return def;
		return getVariables(vs.playerName, false).get(vs.varName, def);
	}
	
	/**
	 * Set the given variable spec. to the given value.
	 * 
	 * @param playerName
	 * @param varSpec
	 * @param value
	 */
	public static void setVar(String playerName, String varSpec, String value) {
		VarSpec vs = new VarSpec(playerName, varSpec);
		getVariables(vs.playerName, true).set(vs.varName, value);
	}
	
	/**
	 * Check if the given variable spec. exists.
	 * 
	 * @param playerName
	 * @param varSpec
	 * @return
	 */
	public static boolean isVarSet(String playerName, String varSpec) {
		VarSpec vs = new VarSpec(playerName, varSpec);
		
		if (!hasVariables(vs.playerName)) return false;
		return getVariables(vs.playerName, false).isSet(vs.varName);
	}

	private static Collection<SMSVariables> listVariables(boolean isSorted) {
		if (isSorted) {
			SortedSet<String> sorted = new TreeSet<String>(allVariables.keySet());
			List<SMSVariables> res = new ArrayList<SMSVariables>();
			for (String name : sorted) {
				res.add(allVariables.get(name));
			}
			return res;
		} else {
			return new ArrayList<SMSVariables>(allVariables.values());
		}
	}

	static void load(File f) {
		YamlConfiguration conf = YamlConfiguration.loadConfiguration(f);
		String playerName = FilenameUtils.removeExtension(f.getName());
		SMSVariables vars = getVariables(playerName, true);

		for (String key : conf.getKeys(false)) {
			vars.set(key, conf.getString(key));
		}
	}
	
	private static class VarSpec {
		public final String playerName;
		public final String varName;

		public VarSpec(String playerName, String spec) {
			String[] parts = spec.split("\\.", 2);
			if (parts.length == 1) {
				this.playerName = playerName;
				this.varName = parts[0];
			} else {
				this.playerName = parts[0];
				this.varName = parts[1];
			}
		}
	}
}
