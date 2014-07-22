package me.desht.scrollingmenusign.views;

import com.dsh105.holoapi.HoloAPI;
import com.dsh105.holoapi.api.Hologram;
import com.dsh105.holoapi.api.HologramFactory;
import com.dsh105.holoapi.api.touch.Action;
import com.dsh105.holoapi.api.touch.TouchAction;
import me.desht.dhutils.ConfigurationManager;
import me.desht.dhutils.Debugger;
import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.enums.SMSUserAction;
import me.desht.scrollingmenusign.views.hologram.HoloUtil;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPhysicsEvent;

import java.util.LinkedHashMap;
import java.util.Observable;

public class SMSPublicHoloView extends SMSGlobalScrollableView {
    private static final String LINES = "lines";
    private static final String DIRECTION = "direction";
    private static final String OFFSET = "offset";
    private static final String REDSTONE = "redstone";
    private Hologram hologram;
    private boolean powered;

    public SMSPublicHoloView(String name, SMSMenu menu) {
        super(name, menu);

        registerAttributes();
    }

    public SMSPublicHoloView(String name, SMSMenu menu, Location loc) {
        super(name, menu);

        if (!ScrollingMenuSign.getInstance().isHoloAPIEnabled()) {
            throw new SMSException("Public hologram view cannot be created - server does not have HoloAPI enabled");
        }

        registerAttributes();

        addLocation(loc);
    }

    @Override
    public void addLocation(Location loc) {
        super.addLocation(loc);

        powered = loc.getBlock().isBlockIndirectlyPowered();
    }

    private void registerAttributes() {
        registerAttribute(LINES, 4, "Number of lines visible in the hologram (including title)");
        registerAttribute(DIRECTION, BlockFace.UP, "Positioning of hologram relative to anchor block");
        registerAttribute(OFFSET, 1.0, "Offset distance from anchor block");
        registerAttribute(REDSTONE, RedstoneBehaviour.IGNORE, "Whether to require a redstone signal to show the hologram");
    }

    private Hologram buildHologram(String[] text) {
        if (hologram != null) {
            HoloAPI.getManager().stopTracking(hologram);
        }
        Debugger.getInstance().debug("creating new public hologram for " + getName());
        Hologram h = new HologramFactory(ScrollingMenuSign.getInstance())
                .withLocation(getHologramPosition())
                .withText(text)
                .withSimplicity(true)
                .build();

        h.addTouchAction(new SMSHoloTouchAction(this));
        h.setTouchEnabled(true);

        return h;
    }

    private Location getHologramPosition() {
        Location loc0 = getLocationsArray()[0].clone();
        BlockFace face = (BlockFace) getAttribute(DIRECTION);
        double offset = (Double) getAttribute(OFFSET);
        loc0.add(0.5 + face.getModX() * offset, 0.5 + face.getModY() * offset, 0.5 + face.getModZ() * offset);
        return loc0;
    }

    @Override
    public String getType() {
        return "public-holo";
    }

    @Override
    public void update(Observable menu, Object arg1) {
        super.update(menu, arg1);

        repaintAll();
    }

    @Override
    public void onDeleted(boolean permanent) {
        super.onDeleted(permanent);
        if (permanent) {
            if (hologram != null) {
                popdown();
            }
        }
    }

    private void repaintAll() {
        if (isActive()) {
            String[] text = HoloUtil.buildText(this, null, (Integer) getAttribute(LINES));
            if (hologram != null && hologram.getLines().length != text.length) {
                popdown(); // force a new hologram to be created with the right size
            }
            if (hologram == null) {
                hologram = buildHologram(text);
                HoloAPI.getManager().track(hologram, ScrollingMenuSign.getInstance());
            } else {
                HoloAPI.getManager().setLineContent(hologram, text);
//                hologram.updateLines(text);
            }
        }
    }

    @Override
    public String toString() {
        Location[] locs = getLocationsArray();
        return "public-holo @ " + (locs.length == 0 ? "NONE" : MiscUtil.formatLocation(getLocationsArray()[0]));
    }

    @Override
    protected int getLineLength() {
        return 50;  // estimate
    }

    @Override
    protected int getHardMaxTitleLines() {
        return 2;
    }

    @Override
    public void onConfigurationChanged(ConfigurationManager configurationManager, String attribute, Object oldVal, Object newVal) {
        super.onConfigurationChanged(configurationManager, attribute, oldVal, newVal);

        if ((attribute.equals(OFFSET) || attribute.equals(DIRECTION)) && hologram != null) {
            hologram.move(getHologramPosition());
        } else if (attribute.equals(REDSTONE)) {
            checkVisibility(powered, powered, (RedstoneBehaviour) oldVal, (RedstoneBehaviour) newVal);
        }
    }

    @Override
    public boolean isClickable() {
        // the anchor block isn't used for scrolling or executing - click the hologram itself for that
        return false;
    }

    /**
     * Check if this view is active; if the hologram for the view is being displayed.
     * This is dependent on the anchor block's current redstone power level and the
     * value of the REDSTONE attribute.
     *
     * @return true if the view is active, false otherwise
     */
    public boolean isActive() {
        return isActive(powered, getRedstoneBehaviour());
    }

    /**
     * Get the current redstone behaviour.
     *
     * @return the current redstone behaviour
     */
    public RedstoneBehaviour getRedstoneBehaviour() {
        return (RedstoneBehaviour) getAttribute(REDSTONE);
    }

    @Override
    public void processEvent(ScrollingMenuSign plugin, BlockPhysicsEvent event) {
        super.processEvent(plugin, event);

        boolean newPower = getLocationsArray()[0].getBlock().isBlockIndirectlyPowered();
        if (newPower != powered) {
            Debugger.getInstance().debug("holo anchor for " + getName() + " got power change! " + powered + " => " + newPower);
            checkVisibility(powered, newPower, getRedstoneBehaviour(), getRedstoneBehaviour());
            powered = newPower;
        }
    }

    private void checkVisibility(boolean oldPower, boolean newPower, RedstoneBehaviour oldRedstone, RedstoneBehaviour newRedstone) {
        if (!isActive(oldPower, oldRedstone) && isActive(newPower, newRedstone)) {
            if (hologram == null) {
                hologram = buildHologram(HoloUtil.buildText(this, null, (Integer) getAttribute(LINES)));
                HoloAPI.getManager().track(hologram, ScrollingMenuSign.getInstance());
            }
        } else if (isActive(oldPower, oldRedstone) && !isActive(newPower, newRedstone) && hologram != null) {
            popdown();
        }
    }

    private boolean isActive(boolean level, RedstoneBehaviour rsb) {
        switch (rsb) {
            case IGNORE: return true;
            case HIGH: return level;
            case LOW: return !level;
            default: throw new IllegalArgumentException("invalid value: " + rsb);
        }
    }

    private void popdown() {
        HoloAPI.getManager().stopTracking(hologram);
        hologram = null;
    }

    public enum RedstoneBehaviour {
        IGNORE, HIGH, LOW
    }

    private class SMSHoloTouchAction implements TouchAction {
        private final SMSPublicHoloView view;

        private SMSHoloTouchAction(SMSPublicHoloView view) {
            this.view = view;
        }

        @Override
        public void onTouch(Player player, Action action) {
            Debugger.getInstance().debug("Hologram action: player=" + player.getName() + " action=" + action + " view = " + view.getName());
            SMSUserAction ua = getAction(player, action);
            if (ua != null) {
                ua.execute(player, view);
            }
        }

        @Override
        public String getSaveKey() {
            return null;
        }

        @Override
        public LinkedHashMap<String, Object> getDataToSave() {
            return null;
        }

        private SMSUserAction getAction(Player player, Action action) {
            StringBuilder key = new StringBuilder();
            switch (action) {
                case RIGHT_CLICK:
                    key = new StringBuilder("sms.actions.rightclick.");
                    break;
                case LEFT_CLICK:
                    key = new StringBuilder("sms.actions.leftclick.");
                    break;
            }
            key.append(player.isSneaking() ? "sneak" : "normal");

            String s = ScrollingMenuSign.getInstance().getConfig().getString(key.toString(), "none");
            return SMSUserAction.valueOf(s.toUpperCase());
        }
    }
}
