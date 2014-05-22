package me.desht.scrollingmenusign.views;

import java.util.*;

import me.desht.dhutils.ConfigurationManager;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PersistableLocation;
import me.desht.scrollingmenusign.RedstoneControlSign;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSMenuItem;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.TooltipSign;
import me.desht.scrollingmenusign.enums.RedstoneOutputMode;
import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.scrollingmenusign.enums.SMSUserAction;
import me.desht.scrollingmenusign.views.redout.Switch;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import com.google.common.base.Joiner;

/**
 * This is just like a {@link SMSScrollableView} but maintains only a single player context
 * for scroll position tracking.  Generally used as the base class for views implemented by
 * blocks in the world, such as sign views.
 * <p/>
 * It also maintains a set of output switches which are powered/unpowered depending on
 * the selected item in this view, and tracks the location of a possible tooltip sign.
 */
public abstract class SMSGlobalScrollableView extends SMSScrollableView {
    protected static final int SIGN_WIDTH = 15;
    protected static final int SIGN_LINES = 4;

    public static final String RS_OUTPUT_MODE = "rsoutputmode";
    public static final String PULSE_TICKS = "pulseticks";

    private final Set<Switch> switches = new HashSet<Switch>();
    private final Set<RedstoneControlSign> controlSigns = new HashSet<RedstoneControlSign>();

    private PersistableLocation tooltipSign;
    private BukkitTask pulseResetTask;

    public SMSGlobalScrollableView(SMSMenu menu) {
        this(null, menu);
    }

    public SMSGlobalScrollableView(String name, SMSMenu menu) {
        super(name, menu);
        Configuration config = ScrollingMenuSign.getInstance().getConfig();
        registerAttribute(RS_OUTPUT_MODE, RedstoneOutputMode.SELECTED, "Redstone output mode when menu is scrolled/clicked");
        registerAttribute(PULSE_TICKS, config.getLong("sms.redstoneoutput.pulseticks"), "Pulse duration for " + RS_OUTPUT_MODE + "=pulse");
        pulseResetTask = null;
        tooltipSign = null;
    }

    @Override
    public void pushMenu(Player player, SMSMenu newActive) {
        super.pushMenu(player, newActive);
        updateTooltipSign();
    }

    @Override
    public SMSMenu popMenu(Player player) {
        SMSMenu menu = super.popMenu(player);

        updateTooltipSign();

        return menu;
    }

    @Override
    public void update(Observable menu, Object arg) {
        super.update(menu, arg);

        ViewUpdateAction vu = ViewUpdateAction.getAction(arg);
        if (vu.getAction() == SMSMenuAction.REPAINT) {
            updateTooltipSign();
        }
    }

    public void addSwitch(Switch sw) {
        switches.add(sw);
        autosave();
    }

    public void removeSwitch(Switch sw) {
        switches.remove(sw);
        autosave();
    }

    public void addControlSign(RedstoneControlSign sign) {
        controlSigns.add(sign);
        autosave();
    }

    public void removeControlSign(RedstoneControlSign sign) {
        controlSigns.remove(sign);
        autosave();
    }

    @Override
    protected UUID getPlayerContext(Player player) {
        return GLOBAL_PLAYER_UUID;
    }

    /**
     * Get the last scroll position (currently-selected item) for this view.  If the scroll position
     * is out of range (possibly because an item was deleted from the menu), it will be automatically
     * adjusted to be in range before being returned.
     *
     * @return The scroll position
     */
    public int getScrollPos() {
        return super.getScrollPos(null);
    }

    public void updateSwitchPower() {
        SMSMenuItem item = getActiveMenuItemAt(null, getScrollPos());
        if (item == null) {
            return;
        }
        for (Switch sw : switches) {
            sw.setPowered(sw.getTrigger().equals(item.getLabel()));
        }
    }

    /**
     * Toggle the switch status for the currently selected menu item
     */
    public void toggleSwitchPower() {
        SMSMenuItem item = getActiveMenuItemAt(null, getScrollPos());
        if (item == null) {
            return;
        }
        for (Switch sw : switches) {
            if (sw.getTrigger().equals(item.getLabel())) {
                sw.setPowered(!sw.getPowered());
            }
        }
    }

    /**
     * Set the switch status for the currently selected menu item on, and all others off.
     */
    public void radioSwitchPower() {
        SMSMenuItem item = getActiveMenuItemAt(null, getScrollPos());
        if (item == null) {
            return;
        }
        for (Switch sw : switches) {
            sw.setPowered(sw.getTrigger().equals(item.getLabel()));
        }
    }

    /**
     * Set the switch status for the selected menu item on for a given time, then off again.
     *
     * @param pulseAll if true, pulse the switch status for <em>all</em> switches
     */
    public void pulseSwitchPower(boolean pulseAll) {
        SMSMenuItem item = getActiveMenuItemAt(null, getScrollPos());
        if (item == null) {
            return;
        }
        final List<Switch> affected = new ArrayList<Switch>();
        for (Switch sw : switches) {
            if (pulseAll || sw.getTrigger().equals(item.getLabel())) {
                sw.setPowered(true);
                affected.add(sw);
            }
        }

        if (!affected.isEmpty()) {
            long delay = (Long) getAttribute(PULSE_TICKS);
            pulseResetTask = Bukkit.getScheduler().runTaskLater(ScrollingMenuSign.getInstance(), new Runnable() {
                @Override
                public void run() {
                    for (Switch sw : affected) {
                        sw.setPowered(false);
                    }
                    pulseResetTask = null;
                }
            }, delay);
        }
    }

    public Set<Switch> getSwitches() {
        return switches;
    }

    public Set<RedstoneControlSign> getControlSigns() {
        return controlSigns;
    }

    @Override
    public Map<String, Object> freeze() {
        Map<String, Object> map = super.freeze();

        Map<String, Map<String, Object>> l = new HashMap<String, Map<String, Object>>();
        for (Switch sw : switches) {
            l.put(sw.getName(), sw.freeze());
        }
        map.put("switches", l);

        List<PersistableLocation> locs = new ArrayList<PersistableLocation>();
        for (RedstoneControlSign s : controlSigns) {
            PersistableLocation pl = new PersistableLocation(s.getlocation());
            pl.setSavePitchAndYaw(false);
            locs.add(pl);
        }
        map.put("controlSigns", locs);

        if (tooltipSign != null) {
            map.put("tooltip", tooltipSign);
        }

        return map;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void thaw(ConfigurationSection node) throws SMSException {
        super.thaw(node);

        ConfigurationSection sw = node.getConfigurationSection("switches");
        if (sw != null) {
            for (String k : sw.getKeys(false)) {
                ConfigurationSection conf = node.getConfigurationSection("switches." + k);
                try {
                    addSwitch(new Switch(this, conf));
                } catch (SMSException e) {
                    // world not loaded
                    Switch.deferLoading(this, conf);
                }
            }
            updateSwitchPower();
        }

        List<PersistableLocation> rcSignLocs = (List<PersistableLocation>) node.getList("controlSigns");
        if (rcSignLocs != null) {
            for (PersistableLocation pl : rcSignLocs) {
                try {
                    RedstoneControlSign.getControlSign(pl.getLocation(), this);
                } catch (IllegalStateException e) {
                    // world not loaded
                    RedstoneControlSign.deferLoading(pl.getWorldName(), new Vector(pl.getX(), pl.getY(), pl.getZ()));
                } catch (SMSException e) {
                    LogUtils.warning("can't load redstone control sign at " + MiscUtil.formatLocation(pl.getLocation()) + ": " + e.getMessage());
                }
            }
        }

        tooltipSign = (PersistableLocation) node.get("tooltip");
        if (tooltipSign != null) {
            Location loc = tooltipSign.getLocation();
            ScrollingMenuSign.getInstance().getLocationManager().registerLocation(loc, new TooltipSign(this));
        }
    }

    @Override
    public void onScrolled(Player player, SMSUserAction action) {
        super.onScrolled(player, action);

        RedstoneOutputMode mode = (RedstoneOutputMode) getAttribute(RS_OUTPUT_MODE);
        if (mode == RedstoneOutputMode.SELECTED) {
            updateSwitchPower();
        }

        updateTooltipSign();
    }

    @Override
    public void onExecuted(Player player) {
        super.onExecuted(player);

        RedstoneOutputMode mode = (RedstoneOutputMode) getAttribute(RS_OUTPUT_MODE);
        switch (mode) {
            case TOGGLE:
                toggleSwitchPower();
                break;
            case PULSE:
                pulseSwitchPower(false);
                break;
            case PULSEANY:
                pulseSwitchPower(true);
                break;
            case RADIO:
                radioSwitchPower();
            default:
                break;
        }
    }

    @Override
    public void onDeleted(boolean permanent) {
        super.onDeleted(permanent);
        if (permanent) {
            if (tooltipSign != null) {
                Block b = tooltipSign.getBlock();
                if (b.getType() == Material.SIGN_POST || b.getType() == Material.WALL_SIGN) {
                    Sign sign = (Sign) b.getState();
                    for (int i = 0; i < SIGN_LINES; i++) {
                        sign.setLine(i, "");
                    }
                    sign.update();
                }
                removeTooltipSign();
            }
        }
    }

    @Override
    public void onConfigurationChanged(ConfigurationManager configurationManager, String key, Object oldVal, Object newVal) {
        super.onConfigurationChanged(configurationManager, key, oldVal, newVal);

        if (key.equals(RS_OUTPUT_MODE)) {
            switch ((RedstoneOutputMode) newVal) {
                case SELECTED:
                    updateSwitchPower();
                    break;
                default:
                    for (Switch sw : getSwitches()) {
                        sw.setPowered(false);
                    }
            }
            if (pulseResetTask != null) {
                pulseResetTask.cancel();
                pulseResetTask = null;
            }
        }
    }

    public void addTooltipSign(Location loc) {
        tooltipSign = new PersistableLocation(loc);
        tooltipSign.setSavePitchAndYaw(false);
        ScrollingMenuSign.getInstance().getLocationManager().registerLocation(loc, new TooltipSign(this));
        autosave();
    }

    public Location getTooltipSign() {
        return tooltipSign == null ? null : tooltipSign.getLocation();
    }

    public void removeTooltipSign() {
        ScrollingMenuSign.getInstance().getLocationManager().unregisterLocation(tooltipSign.getLocation());
        tooltipSign = null;

        autosave();
    }

    public void updateTooltipSign() {
        if (tooltipSign == null) {
            return;
        }
        Block b = tooltipSign.getBlock();
        if (b.getType() != Material.WALL_SIGN && b.getType() != Material.SIGN_POST) {
            LogUtils.warning("Block " + b + " is not a sign.  Removing tooltip from view " + getName());
            removeTooltipSign();
        } else {
            Sign sign = (Sign) b.getState();
            String[] text = getTooltipText();
            for (int i = 0; i < SIGN_LINES; i++) {
                sign.setLine(i, text[i]);
            }
            sign.update();
        }
    }

    public String[] getTooltipText() {
        String[] lore = getActiveMenuItemAt(null, getScrollPos()).getLore();
        Scanner scanner = new Scanner(Joiner.on(" ").join(lore));
        StringBuilder sb = new StringBuilder();
        String[] text = new String[SIGN_LINES];
        int i = 0;
        while (scanner.hasNext() && i < text.length) {
            String word = scanner.next();
            if (sb.length() + word.length() >= SIGN_WIDTH) {
                text[i++] = sb.toString();
                sb.setLength(0);
            }
            if (sb.length() > 0) sb.append(" ");
            sb.append(word);
        }
        scanner.close();
        if (i < text.length) text[i] = sb.toString();

        return text;
    }

}