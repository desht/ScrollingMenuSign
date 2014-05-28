package me.desht.scrollingmenusign;

import me.desht.dhutils.LogUtils;
import me.desht.dhutils.UUIDFetcher;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Represents the usage-limitation on SMS menus and menu items.
 */
public class SMSRemainingUses {
    private static final String GLOBAL_MAX = "&GLOBAL";
    private static final String GLOBAL_REMAINING = "&GLOBALREMAINING";
    private static final String PER_PLAYER_MAX = "&PERPLAYER";

    private int globalMax = 0;
    private int globalRemaining = 0;
    private int perPlayerMax = 0;

    private final Map<UUID, Integer> uses = new HashMap<UUID, Integer>();
    private final Map<String, Integer> oldUses = new HashMap<String, Integer>();

    private final SMSUseLimitable parentObject;

    SMSRemainingUses(SMSUseLimitable lim) {
        this.parentObject = lim;
    }

    SMSRemainingUses(SMSUseLimitable lim, ConfigurationSection node) {
        this.parentObject = lim;
        if (node == null)
            return;
        if (!node.getKeys(false).isEmpty() && !node.contains("limits")) {
            migrateOldFormatData(node);
        } else {
            globalMax = node.getInt("limits.globalMax", 0);
            globalRemaining = node.getInt("limits.globalRemaining", 0);
            perPlayerMax = node.getInt("limits.perPlayerMax", 0);

            ConfigurationSection cs = node.getConfigurationSection("players");
            if (cs != null) {
                for (String id : cs.getKeys(false)) {
                    uses.put(UUID.fromString(id), cs.getInt(id));
                }
            }
        }
    }

    /**
     * Check if this item has usage limits (either global or per-player) in place.
     *
     * @return true if there are limitations; false otherwise
     */
    public boolean hasLimitedUses() {
        return globalMax > 0 || perPlayerMax > 0;
    }

    /**
     * Get the remaining uses for the player.
     *
     * @param playerName the player's name
     * @return the remaining uses
     * @deprecated use {@link #getRemainingUses(org.bukkit.OfflinePlayer)}
     */
    @Deprecated
    public int getRemainingUses(String playerName) {
        @SuppressWarnings("deprecation") Player player = Bukkit.getPlayer(playerName);
        return player == null ? 0 : getRemainingUses(player);
    }

    /**
     * Get the remaining uses for the given player.
     *
     * @param player the player to check for
     * @return the number of uses remaining, or Integer.MAX_VALUE if there is no limit
     */
    public int getRemainingUses(OfflinePlayer player) {
        if (globalMax > 0) {
            return globalRemaining;
        } else if (perPlayerMax > 0) {
            Integer uses = this.uses.get(player.getUniqueId());
            return uses == null ? perPlayerMax : uses;
        } else {
            return Integer.MAX_VALUE;
        }
    }

    /**
     * Clear all usage limits for this item, for all players.
     */
    public void clearUses() {
        uses.clear();
        perPlayerMax = globalMax = globalRemaining = 0;

        autosave();
    }

    /**
     * Clear usage limits for the given player.
     *
     * @param playerName The player name to remove usage limits for
     * @deprecated use {@link #clearUses(org.bukkit.OfflinePlayer)}
     */
    @Deprecated
    public void clearUses(String playerName) {
        @SuppressWarnings("deprecation") Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            clearUses(player);
        }
    }

    /**
     * Reset the usage count for the given player.  This does not remove the
     * usage limitation, so subsequent uses of the item will decrement the usage
     * count again.
     *
     * @param player the player
     */
    public void clearUses(OfflinePlayer player) {
        uses.remove(player.getUniqueId());
        autosave();
    }

    /**
     * Set the usage limits per player.  This is the total number of times an
     * item or menu can be used by each player.
     *
     * @param useCount The usage limit
     */
    public void setUses(int useCount) {
        uses.clear();
        globalMax = globalRemaining = 0;
        perPlayerMax = useCount;
        autosave();
    }

    /**
     * Set the global usage limit.  This is the total number of times an item/menu can be
     * used by any player.
     *
     * @param useCount the usage count
     */
    public void setGlobalUses(int useCount) {
        uses.clear();
        globalMax = globalRemaining = useCount;
        perPlayerMax = 0;
        autosave();
    }

    /**
     * Record a usage event against this item.
     *
     * @param playerName name of the player who used the menu/item
     * @throws SMSException if there are not enough uses remaining
     * @deprecated use {@link #use(org.bukkit.OfflinePlayer)}
     */
    @Deprecated
    public void use(String playerName) {
        @SuppressWarnings("deprecation") Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            use(player);
        }
    }

    /**
     * Record a usage event against this item.
     *
     * @param player The player who used the menu/item
     * @throws SMSException if there are not enough uses remaining
     */
    public void use(OfflinePlayer player) {
        if (globalMax > 0) {
            SMSValidate.isTrue(globalRemaining > 0, "Not enough uses remaining");
            globalRemaining--;
        } else {
            Integer uses = this.uses.get(player.getUniqueId());
            SMSValidate.isTrue(uses == null || uses > 0, "Not enough uses remaining");
            this.uses.put(player.getUniqueId(), uses == null ? perPlayerMax - 1 : uses - 1);
        }
        autosave();
    }

    private void autosave() {
        parentObject.autosave();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        if (globalMax > 0) {
            return String.format("%d/%d (global)", globalRemaining, globalMax);
        } else if (perPlayerMax > 0) {
            return String.format("%d (per-player)", perPlayerMax);
        } else {
            return "";
        }
    }

    /**
     * Return a formatted description of the total and remaining usage for the given player.
     *
     * @param playerName the player name
     * @return a formatted string
     * @deprecated use {@link #toString(org.bukkit.OfflinePlayer)}
     */
    @Deprecated
    public String toString(String playerName) {
        @SuppressWarnings("deprecation") Player player = Bukkit.getPlayer(playerName);
        return player == null ? "" : toString(player);
    }

    /**
     * Return a formatted description of the total and remaining usage for the given player.
     *
     * @param player the player
     * @return Formatted string
     */
    public String toString(OfflinePlayer player) {
        if (globalMax > 0) {
            return String.format("%d/%d (global)", globalRemaining, globalMax);
        } else if (perPlayerMax > 0) {
            return String.format("%d/%d (for %s)", getRemainingUses(player), perPlayerMax, player.getName());
        } else {
            return "";
        }
    }

    Map<String,Map<String,Integer>> freeze() {
        Map<String,Map<String,Integer>> res = new HashMap<String, Map<String, Integer>>();

        if (hasLimitedUses()) {
            Map<String,Integer> l = new HashMap<String, Integer>();
            if (globalMax > 0) {
                l.put("globalMax", globalMax);
                l.put("globalRemaining", globalRemaining);
            } else if (perPlayerMax > 0) {
                l.put("perPlayerMax", perPlayerMax);
            }
            res.put("limits", l);

            Map<String,Integer> p = new HashMap<String, Integer>();
            for (Map.Entry<UUID, Integer> e : uses.entrySet()) {
                p.put(e.getKey().toString(), e.getValue());
            }
            res.put("players", p);
        }

        return res;
    }

    String getDescription() {
        return parentObject.getDescription();
    }

    private void migrateOldFormatData(ConfigurationSection node) {
        LogUtils.info("migrating [" + parentObject.getLimitableName() + "] usage-limit data to new format...");
        globalMax = node.getInt(GLOBAL_MAX);
        globalRemaining = node.getInt(GLOBAL_REMAINING);
        perPlayerMax = node.getInt(PER_PLAYER_MAX);
        for (String key : node.getKeys(false)) {
            if (!key.startsWith("&")) {
                oldUses.put(key, node.getInt(key));
            }
        }
        if (!oldUses.isEmpty()) {
            Bukkit.getScheduler().runTaskAsynchronously(
                    ScrollingMenuSign.getInstance(),
                    new AsyncMigrationTask(new ArrayList<String>(oldUses.keySet()))
            );
        }
    }

    private class AsyncMigrationTask implements Runnable {
        private final List<String> toMigrate;

        public AsyncMigrationTask(List<String> toMigrate) {
            this.toMigrate = toMigrate;
        }

        @Override
        public void run() {
            UUIDFetcher uf = new UUIDFetcher(toMigrate);
            try {
                Map<String,UUID> res = uf.call();
                Bukkit.getScheduler().runTask(ScrollingMenuSign.getInstance(), new SyncMigrationTask(res));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class SyncMigrationTask implements Runnable {
        private final Map<String, UUID> data;

        public SyncMigrationTask(Map<String, UUID> res) {
            this.data = res;
        }

        @Override
        public void run() {
            for (Map.Entry<String, UUID> e : data.entrySet()) {
                Integer n = oldUses.get(e.getKey());
                if (n != null) {
                    uses.put(e.getValue(), n);
                } else {
                    LogUtils.warning("can't find usage-limit data for " + e.getKey() + " in " + parentObject.getLimitableName());
                }
            }
            oldUses.clear();
            LogUtils.info(data.size() + " usage-limit player names for " + parentObject.getLimitableName() + " migrated to UUIDs");
            autosave();
        }
    }
}
