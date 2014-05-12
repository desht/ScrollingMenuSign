package me.desht.scrollingmenusign.views.redout;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PersistableLocation;
import me.desht.dhutils.block.BlockUtil;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSInteractableBlock;
import me.desht.scrollingmenusign.SMSValidate;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.views.SMSGlobalScrollableView;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.material.Lever;
import org.bukkit.material.Redstone;

public class Switch implements Comparable<Switch>, SMSInteractableBlock {
    private static final Map<String, Set<ConfigurationSection>> deferred = new HashMap<String, Set<ConfigurationSection>>();

    private final SMSGlobalScrollableView view;
    private final PersistableLocation location;
    private final String trigger;
    private final String name;

    public Switch(SMSGlobalScrollableView view, String trigger, Location loc) {
        this.view = view;
        this.location = new PersistableLocation(loc);
        this.trigger = trigger;
        this.name = makeUniqueName(loc);

        ScrollingMenuSign.getInstance().getLocationManager().registerLocation(loc, this);
    }

    public Switch(SMSGlobalScrollableView view, ConfigurationSection conf) throws SMSException {
        String worldName = conf.getString("world");
        World w = Bukkit.getWorld(worldName);
        SMSValidate.notNull(w, "World not available");

        this.view = view;
        Location loc = new Location(w, conf.getInt("x"), conf.getInt("y"), conf.getInt("z"));
        this.location = new PersistableLocation(loc);
        this.trigger = MiscUtil.parseColourSpec(conf.getString("trigger"));
        this.name = makeUniqueName(loc);

        ScrollingMenuSign.getInstance().getLocationManager().registerLocation(loc, this);
    }

    /**
     * Get the view this output switch belongs to.
     *
     * @return The owning view
     */
    public SMSGlobalScrollableView getView() {
        return view;
    }

    /**
     * Get the name of this output switch.
     *
     * @return The switch's name
     */
    public String getName() {
        return name;
    }

    /**
     * Get this output switch's location.
     *
     * @return The switch's location
     */
    public Location getLocation() {
        return location.getLocation();
    }

    /**
     * Get the trigger string for this switch.
     *
     * @return The switch's trigger string
     */
    public String getTrigger() {
        return trigger;
    }

    /**
     * Remove this switch from its owning view.
     */
    public void delete() {
        view.removeSwitch(this);
        ScrollingMenuSign.getInstance().getLocationManager().unregisterLocation(getLocation());
    }

    /**
     * Get the Material for this switch.  (Right now, only Lever is supported).
     *
     * @return The switch's material
     */
    private Material getSwitchType() {
        return getLocation().getBlock().getType();
    }

    /**
     * Check if this switch is currently powered.
     *
     * @return true if powered, false otherwise
     */
    public boolean getPowered() {
        Block b = getLocation().getBlock();
        if (getSwitchType() == Material.LEVER) {
            return ((Redstone) b.getState().getData()).isPowered();
        } else {
            LogUtils.warning("Found " + getSwitchType() + " at " + location + " - expecting LEVER!");
            return false;
        }
    }

    /**
     * Set the powered status of this switch.
     *
     * @param powered true to switch on, false to switch off
     */
    public void setPowered(boolean powered) {
        if (getSwitchType() == Material.LEVER) {
            setLeverPowered(getLocation().getBlock(), powered);
        } else {
            LogUtils.warning("Found " + getSwitchType() + " at " + location + " - expecting LEVER!");
        }
    }

    private void setLeverPowered(Block b, boolean powered) {
        BlockState bs = b.getState();
        Lever lever = (Lever) bs.getData();
        lever.setPowered(powered);
        bs.setData(lever);
        bs.update();
    }

    public Map<String, Object> freeze() {
        Map<String, Object> map = new HashMap<String, Object>();

        map.put("trigger", MiscUtil.unParseColourSpec(trigger));
        map.put("world", location.getWorldName());
        map.put("x", location.getX());
        map.put("y", location.getY());
        map.put("z", location.getZ());

        return map;
    }

    private String makeUniqueName(Location loc) {
        return String.format("%s-%d-%d-%d", loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public static void deferLoading(SMSGlobalScrollableView view, ConfigurationSection conf) {
        conf.set("viewName", view.getName());
        String world = conf.getString("world");
        Set<ConfigurationSection> set = deferred.get(world);
        if (set == null) {
            set = new HashSet<ConfigurationSection>();
            deferred.put(conf.getString("world"), set);
        }
        set.add(conf);
    }

    public static void loadDeferred(World world) {
        Set<ConfigurationSection> set = deferred.get(world.getName());
        if (set == null) {
            return;
        }

        for (ConfigurationSection conf : set) {
            String viewName = conf.getString("viewName");
            try {
                SMSGlobalScrollableView view = (SMSGlobalScrollableView) ScrollingMenuSign.getInstance().getViewManager().getView(viewName);
                view.addSwitch(new Switch(view, conf));
            } catch (SMSException e) {
                LogUtils.warning("Can't load  deferred switch for view " + viewName + ": " + e.getMessage());
            }
        }

        deferred.remove(world.getName());
    }

    @Override
    public int compareTo(Switch other) {
        return name.compareTo(other.getName());
    }

    @Override
    public void processEvent(ScrollingMenuSign plugin, BlockDamageEvent event) {
        // ignore
    }

    @Override
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void processEvent(ScrollingMenuSign plugin, BlockBreakEvent event) {
        MiscUtil.statusMessage(event.getPlayer(),
                String.format("Output switch @ &f%s&- was removed from view &e%s / %s.",
                        MiscUtil.formatLocation(event.getBlock().getLocation()),
                        getView().getName(), getTrigger()));
        delete();
    }

    @Override
    public void processEvent(ScrollingMenuSign plugin, BlockPhysicsEvent event) {
        if (plugin.getConfig().getBoolean("sms.no_physics")) {
            event.setCancelled(true);
        } else if (BlockUtil.isAttachableDetached(event.getBlock())) {
            LogUtils.info("Redstone output switch @ " + location + " (for " + getView().getName() + ") has become detached: deleting");
            delete();
        }
    }

    @Override
    public void processEvent(ScrollingMenuSign plugin, BlockRedstoneEvent event) {
        // ignore
    }
}
