package me.desht.scrollingmenusign.variables;

import me.desht.scrollingmenusign.DirectoryStructure;
import me.desht.scrollingmenusign.SMSPersistable;
import me.desht.scrollingmenusign.SMSPersistence;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.MemoryConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * @author desht
 */
public class SMSVariables implements SMSPersistable {
    private final UUID playerId;
    private final Configuration variables;

    /**
     * Package-protected constructor.
     *
     * @param playerId ID of the player who owns these variables
     */
    SMSVariables(UUID playerId) {
        this.playerId = playerId;
        variables = new MemoryConfiguration();
    }

    public UUID getPlayerId() {
        return playerId;
    }

    void autosave() {
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
}
