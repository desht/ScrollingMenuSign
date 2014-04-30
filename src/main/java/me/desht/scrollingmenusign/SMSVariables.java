package me.desht.scrollingmenusign;

import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PermissionUtils;
import me.desht.dhutils.UUIDFetcher;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.*;

/**
 * @author desht
 */
public class SMSVariables implements SMSPersistable {
	private static final UUID CONSOLE_SPECIAL_UUID = UUID.fromString("b5ccc760-cc85-11e3-9c1a-0800200c9a66");
	private static final UUID GLOBAL_UUID = new UUID(0, 0);

	private static final Map<String, YamlConfiguration> toMigrate = new HashMap<String, YamlConfiguration>();

	private static final Map<UUID, SMSVariables> allVariables = new HashMap<UUID, SMSVariables>();

	private static final String DEFAULT_MARKER = "*";

	private final UUID playerId;
	private final Configuration variables;

	/**
	 * Private constructor.
	 *
	 * @param playerId ID of the player who owns these variables
	 */
	private SMSVariables(UUID playerId) {
		this.playerId = playerId;
		variables = new MemoryConfiguration();
	}

	public UUID getPlayerId() {
		return playerId;
	}

	private void autosave() {
		SMSPersistence.save(this);
	}

	/**
	 * Set the given variable to the given value
	 *
	 * @param varName the variable name
	 * @param value   the value
	 */
	public void set(String varName, String value) {
		variables.set(varName, value);
		autosave();
	}

	/**
	 * Get the value of the given variable
	 *
	 * @param varName the variable name
	 * @return the value, or null if the variable does not exist
	 */
	public String get(String varName) {
		return variables.getString(varName);
	}

	/**
	 * Get the value of the given variable
	 *
	 * @param varName the variable name
	 * @param def     default value
	 * @return the value, or null if the variable does not exist
	 */
	public String get(String varName, String def) {
		return variables.getString(varName, def);
	}

	/**
	 * Checks if the given variable exists.
	 *
	 * @param varName the variable name
	 * @return true if it exists, false otherwise
	 */
	public boolean isSet(String varName) {
		return variables.contains(varName);
	}

	/**
	 * Get a all variable names in this variable collection.
	 *
	 * @return a set of the variable names in this collection
	 */
	public Set<String> getVariables() {
		return variables.getKeys(false);
	}

	private void deleteCommon() {
		allVariables.remove(playerId);
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
		return playerId.toString();
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
		Map<String, Object> map = new HashMap<String, Object>();
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
	 * @param sender the command sender
	 * @param autoCreate if true, a variables object will be auto-created if it doesn't exist
	 * @throws SMSException if autoCreate is false and the variables object does not exist
	 * @return an SMSVariables collection of variables for the player
	 */
	public static SMSVariables getVariables(CommandSender sender, boolean autoCreate) {
		UUID id = sender instanceof ConsoleCommandSender ? CONSOLE_SPECIAL_UUID : ((Player) sender).getUniqueId();
		return getVariables(id, autoCreate);
	}

	/**
	 * Get the variable collection for the given player.  If the player has no variables,
	 * a new empty SMSVariables collection will be created iff autoCreate is true, otherwise
	 * an exception will be thrown.
	 *
	 * @param playerName the player's name
	 * @param autoCreate if true, a variables object will be auto-created if it doesn't exist
	 * @throws SMSException if autoCreate is false and the variables object does not exist
	 * @return an SMSVariables collection of variables for the player
	 */
	public static SMSVariables getVariables(UUID playerName, boolean autoCreate) {
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
	 * @param playerId the name of the player to check for
	 * @return true if variables are defined for the player, false otherwise
	 */
	public static boolean hasVariables(UUID playerId) {
		return allVariables.containsKey(playerId);
	}

	/**
	 * Get a list of all the known SMSVariables collections
	 *
	 * @return a list of all the known SMSVariables collections
	 */
	public static Collection<SMSVariables> listVariables() {
		return listVariables(false);
	}

	/**
	 * Get a (possibly sorted) list of all the known SMSVariables collections
	 *
	 * @param isSorted true if the result should be sorted by variable name
	 * @return a list of all the known SMSVariables collections
	 */
	private static Collection<SMSVariables> listVariables(boolean isSorted) {
		if (isSorted) {
			SortedSet<UUID> sorted = new TreeSet<UUID>(allVariables.keySet());
			List<SMSVariables> res = new ArrayList<SMSVariables>();
			for (UUID name : sorted) {
				res.add(allVariables.get(name));
			}
			return res;
		} else {
			return new ArrayList<SMSVariables>(allVariables.values());
		}
	}

	/**
	 * Get the value of the given variable spec.  The spec may be a simple variable name or
	 * a player name followed by a period, followed by the variable name.
	 *
	 * @param sender  the command sender who is retrieving the variable
	 * @param varSpec the variable specification
	 * @return the variable value, or null if not set
	 */
	public static String get(CommandSender sender, String varSpec) {
		return get(sender, varSpec, null);
	}

	/**
	 * Get the value of the given variable spec.  The spec may be a simple variable name or
	 * a player name followed by a period, followed by the variable name.
	 *
	 * @param sender   the command sender who is retrieving the variable
	 * @param varSpec  the variable specification
	 * @param defValue default value to use if the variable is not set
	 * @return the variable value, or the default value if not set
	 */
	public static String get(CommandSender sender, String varSpec, String defValue) {
		VarSpec vs = new VarSpec(sender, varSpec);

		if (hasVariables(vs.playerId) && getVariables(vs.playerId, false).isSet(vs.varName)) {
			return getVariables(vs.playerId, false).get(vs.varName);
		} else {
			if (hasVariables(GLOBAL_UUID)) {
				return getVariables(GLOBAL_UUID, false).get(vs.varName, defValue);
			} else {
				return defValue;
			}
		}
	}

	/**
	 * Set the given variable spec. to the given value.
	 *
	 * @param sender  the command sender who is retrieving the variable
	 * @param varSpec the variable specification
	 * @param value   new value for the variable
	 */
	public static void set(CommandSender sender, String varSpec, String value) {
		VarSpec vs = new VarSpec(sender, varSpec);
		getVariables(vs.playerId, true).set(vs.varName, value);
	}

	/**
	 * Check if the given variable specification exists.
	 * <p/>
	 * A variable specification is either "<varname>" or "<playername>.<varname>"
	 *
	 * @param sender  the command sender to check
	 * @param varSpec the variable specification
	 * @return true if the variable exists, false otherwise
	 */
	public static boolean isSet(CommandSender sender, String varSpec) {
		VarSpec vs = new VarSpec(sender, varSpec);
		return hasVariables(vs.playerId) && getVariables(vs.playerId, false).isSet(vs.varName);
	}

	static void load(File f) {
		YamlConfiguration conf = YamlConfiguration.loadConfiguration(f);
		String playerName = f.getName().replaceAll("\\.yml$", "");
		if (MiscUtil.looksLikeUUID(playerName)) {
			SMSVariables vars = getVariables(UUID.fromString(playerName), true);

			for (String key : conf.getKeys(false)) {
				vars.set(key, conf.getString(key));
			}
		} else {
			toMigrate.put(playerName, conf);
		}
	}

	static void migrateUUIDs() {
		final UUIDFetcher uf = new UUIDFetcher(new ArrayList<String>(toMigrate.keySet()), true);
		Bukkit.getScheduler().runTaskAsynchronously(ScrollingMenuSign.getInstance(), new Runnable() {
			@Override
			public void run() {
				try {
					new SyncUUIDTask(uf.call()).runTask(ScrollingMenuSign.getInstance());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	private static class VarSpec {
		private final UUID playerId;
		private final String varName;

		private VarSpec(CommandSender sender, String spec) {
			String[] parts = spec.split("\\.", 2);

			if (parts.length == 1) {
				// unqualified variable - <var>
				playerId = sender instanceof Player ? ((Player) sender).getUniqueId() : CONSOLE_SPECIAL_UUID;
				varName = parts[0];
			} else {
				// qualified variable - <player>.<var>
				if (parts[0].startsWith(DEFAULT_MARKER)) {
					playerId = GLOBAL_UUID;
				} else if (parts[0].equalsIgnoreCase("console")) {
					playerId = CONSOLE_SPECIAL_UUID;
				} else if (MiscUtil.looksLikeUUID(parts[0])) {
					playerId = UUID.fromString(parts[0]);
				} else {
					throw new SMSException("Varspec [" + spec + "]: player ID should be '*', 'console' or a valid UUID");
				}
				if (sender instanceof Player && !((Player) sender).getUniqueId().equals(playerId)) {
					PermissionUtils.requirePerms(sender, "scrollingmenusign.vars.other");
				}
				varName = parts[1];
			}
			SMSValidate.isTrue(varName.matches("[a-zA-Z0-9_]+"), "Invalid variable name: " + spec + " (must be all alphanumeric)");
		}
	}

	private static class SyncUUIDTask extends BukkitRunnable {
		private final Map<String,UUID> map;
		public SyncUUIDTask(Map<String, UUID> map) {
			this.map = map;
		}

		@Override
		public void run() {
			for (String playerName : map.keySet()) {
				UUID id = map.get(playerName);
				if (id != null) {
					YamlConfiguration conf = toMigrate.get(playerName);
					SMSVariables vars = getVariables(id, true);
					for (String key : conf.getKeys(false)) {
						vars.set(key, conf.getString(key));
					}
				} else {
					LogUtils.warning("can't find UUID for player: " + playerName);
				}
			}
			toMigrate.clear();
			LogUtils.info("user variables migration complete");
		}
	}
}
