package me.desht.scrollingmenusign.variables;

import com.google.common.collect.Lists;
import me.desht.dhutils.*;
import me.desht.scrollingmenusign.*;
import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.scrollingmenusign.util.Substitutions;
import me.desht.scrollingmenusign.views.ViewUpdateAction;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;

public class VariablesManager implements Observer {
    public static final UUID GLOBAL_UUID = new UUID(0, 0);
    private static final String DEFAULT_MARKER = "*";

    private final Map<String, YamlConfiguration> toMigrate = new HashMap<String, YamlConfiguration>();
    private final Map<UUID, SMSVariables> allVariables = new HashMap<UUID, SMSVariables>();
    private final Map<String, Set<String>> menuUsage = new HashMap<String, Set<String>>();

    private final ScrollingMenuSign plugin;

    public VariablesManager(ScrollingMenuSign plugin) {
        this.plugin = plugin;
    }

    public void clear() {
        allVariables.clear();
    }

    public void load(File f) {
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

    public void checkForUUIDMigration() {
        if (toMigrate.isEmpty()) {
            return;
        }
        LogUtils.info("Migrating user variables for " + toMigrate.size() + " user(s)");
        final UUIDFetcher uf = new UUIDFetcher(new ArrayList<String>(toMigrate.keySet()), true);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    new SyncUUIDTask(uf.call()).runTask(plugin);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Get the variable collection for the given player.  If the player has no variables,
     * a new empty SMSVariables collection will be created iff autoCreate is true, otherwise
     * an exception will be thrown.
     *
     * @param sender     the command sender
     * @param autoCreate if true, a variables object will be auto-created if it doesn't exist
     * @return an SMSVariables collection of variables for the player
     * @throws SMSException if autoCreate is false and the variables object does not exist
     */
    public SMSVariables getVariables(CommandSender sender, boolean autoCreate) {
        UUID id = sender instanceof ConsoleCommandSender ? GLOBAL_UUID : ((Player) sender).getUniqueId();
        return getVariables(id, autoCreate);
    }

    /**
     * Get the variable collection for the given player.  If the player has no variables,
     * a new empty SMSVariables collection will be created iff autoCreate is true, otherwise
     * an exception will be thrown.
     *
     * @param playerId   the player's UUID
     * @param autoCreate if true, a variables object will be auto-created if it doesn't exist
     * @return an SMSVariables collection of variables for the player
     * @throws SMSException if autoCreate is false and the variables object does not exist
     */
    public SMSVariables getVariables(UUID playerId, boolean autoCreate) {
        if (!hasVariables(playerId)) {
            SMSValidate.isTrue(autoCreate, "No variables are defined for player ID " + playerId);
            allVariables.put(playerId, new SMSVariables(playerId));
        }
        return allVariables.get(playerId);
    }

    /**
     * Check if any variables are defined for the given player.
     *
     * @param playerId the name of the player to check for
     * @return true if variables are defined for the player, false otherwise
     */
    public boolean hasVariables(UUID playerId) {
        return allVariables.containsKey(playerId);
    }

    /**
     * Get a list of all the known SMSVariables collections
     *
     * @return a list of all the known SMSVariables collections
     */
    public Collection<SMSVariables> listVariables() {
        return listVariables(false);
    }

    /**
     * Get a (possibly sorted) list of all the known SMSVariables collections
     *
     * @param isSorted true if the result should be sorted by variable name
     * @return a list of all the known SMSVariables collections
     */
    private Collection<SMSVariables> listVariables(boolean isSorted) {
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
     * a player UUID, or 'console', followed by a period, followed by the variable name.
     *
     * @param sender  the command sender who is retrieving the variable
     * @param varSpec the variable specification
     * @return the variable value, or null if not set
     * @throws SMSException is varSpec is not a well-formed variable spec
     */
    public String get(CommandSender sender, String varSpec) {
        return get(sender, varSpec, null);
    }

    /**
     * Get the value of the given variable spec.  The spec may be a simple variable name or
     * a player UUID, or 'console', followed by a period, followed by the variable name.
     *
     * @param sender   the command sender who is retrieving the variable
     * @param varSpec  the variable specification
     * @param defValue default value to use if the variable is not set
     * @return the variable value, or the default value if not set
     * @throws SMSException is varSpec is not a well-formed variable spec
     */
    public String get(CommandSender sender, String varSpec, String defValue) {
        VarSpec vs = new VarSpec(sender, varSpec);

        if (hasVariables(vs.getPlayerId()) && getVariables(vs.getPlayerId(), false).isSet(vs.getVarName())) {
            return getVariables(vs.getPlayerId(), false).get(vs.getVarName());
        } else {
            if (hasVariables(GLOBAL_UUID)) {
                return getVariables(GLOBAL_UUID, false).get(vs.getVarName(), defValue);
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
     * @throws SMSException is varSpec is not a well-formed variable spec
     */
    public void set(CommandSender sender, String varSpec, String value) {
        VarSpec vs = new VarSpec(sender, varSpec);
        getVariables(vs.getPlayerId(), value != null).set(vs.getVarName(), value);
        for (SMSMenu menu : getMenusUsingVariable(vs.getVarName())) {
            menu.notifyObservers(new ViewUpdateAction(SMSMenuAction.REPAINT));
        }
    }

    /**
     * Check if the given variable spec exists.  The spec may be a simple variable name or
     * a player UUID, or 'console', followed by a period, followed by the variable name.
     *
     * @param sender  the command sender to check
     * @param varSpec the variable specification
     * @return true if the variable exists, false otherwise
     * @throws SMSException is varSpec is not a well-formed variable spec
     */
    public boolean isSet(CommandSender sender, String varSpec) {
        VarSpec vs = new VarSpec(sender, varSpec);
        return hasVariables(vs.getPlayerId()) && getVariables(vs.getPlayerId(), false).isSet(vs.getVarName());
    }

    /**
     * Given a string, attempt to extract a UUID from it.  The string must be either
     * a valid UUID string, or be equals to "console" or start with a '*', in which case
     * the special global UUID will be returned.
     *
     * @param s the string to extract
     * @return a UUID from the string
     * @throws SMSException if the string can't be extracted
     */
    public UUID getIDFromString(String s) {
        if (MiscUtil.looksLikeUUID(s)) {
            return UUID.fromString(s);
        } else if (s.startsWith(DEFAULT_MARKER) || s.equalsIgnoreCase("console")) {
            return GLOBAL_UUID;
        } else {
            throw new SMSException("Player ID should be '*', 'console' or a valid UUID");
        }
    }

    public List<SMSMenu> getMenusUsingVariable(String varName) {
        Set<String> menuNames = menuUsage.get(varName);
        if (menuNames == null) {
            return Collections.emptyList();
        } else {
            SMSHandler h = ScrollingMenuSign.getInstance().getHandler();
            List<SMSMenu> res = Lists.newArrayList();
            for (String menuName : menuNames) {
                if (h.checkMenu(menuName)) {
                    res.add(h.getMenu(menuName));
                }
            }
            return res;
        }
    }

    public void updateVariableUsage(SMSMenu menu) {
        for (Set<String> s : menuUsage.values()) {
            s.remove(menu.getName());
        }
        updateVarRefs(menu.getTitle(), menu.getName());
        for (SMSMenuItem item : menu.getItems()) {
            updateVarRefs(item.getLabel(), menu.getName());
            for (String l : item.getLore()) {
                updateVarRefs(l, menu.getName());
            }
        }
    }

    private void updateVarRefs(String str, String menuName) {
        Matcher m = Substitutions.userVarSubPat.matcher(str);
        while (m.find()) {
            VarSpec vs = new VarSpec(null, m.group(1));
            if (!menuUsage.containsKey(vs.getVarName())) {
                menuUsage.put(vs.getVarName(), new HashSet<String>());
            }
            menuUsage.get(vs.getVarName()).add(menuName);
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        if (o instanceof SMSMenu) {
            Debugger.getInstance().debug("variables manager : menu updated: " + ((SMSMenu) o).getName());
            updateVariableUsage((SMSMenu) o);
        }
    }

    private class VarSpec {
        private final UUID playerId;
        private final String varName;

        private VarSpec(CommandSender sender, String spec) {
            String[] parts = spec.split("\\.", 2);

            if (parts.length == 1) {
                // unqualified variable - <var>
                playerId = sender instanceof Player ? ((Player) sender).getUniqueId() : GLOBAL_UUID;
                varName = parts[0];
            } else {
                // qualified variable - <player>.<var>
                playerId = getIDFromString(parts[0]);
                if (sender instanceof Player && !((Player) sender).getUniqueId().equals(playerId)) {
                    PermissionUtils.requirePerms(sender, "scrollingmenusign.vars.other");
                }
                varName = parts[1];
            }
            SMSValidate.isTrue(varName.matches("[a-zA-Z0-9_]+"), "Invalid variable name: " + spec + " (must be all alphanumeric)");
        }

        private UUID getPlayerId() {
            return playerId;
        }

        private String getVarName() {
            return varName;
        }
    }

    private class SyncUUIDTask extends BukkitRunnable {
        private final Map<String, UUID> map;

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
                    vars.autosave();
                    File f = new File(DirectoryStructure.getVarsFolder(), playerName + ".yml");
                    if (!f.delete()) {
                        LogUtils.warning("failed to delete old variables file: " + f);
                    }
                } else {
                    LogUtils.warning("can't find UUID for player: " + playerName);
                }
            }
            toMigrate.clear();
            LogUtils.info("User variables migration complete");
        }
    }
}
