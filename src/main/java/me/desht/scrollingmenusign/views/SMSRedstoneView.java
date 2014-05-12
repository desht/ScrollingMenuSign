package me.desht.scrollingmenusign.views;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

import me.desht.dhutils.*;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSMenuItem;
import me.desht.scrollingmenusign.ScrollingMenuSign;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockRedstoneEvent;

/**
 * A view that tracks the redstone powered state of one or more blocks in the world and executes commands in
 * response to changes in the state.
 */
public class SMSRedstoneView extends SMSView {
    // attributes
    public static final String POWERTOGGLE = "powertoggle";
    public static final String POWEROFF = "poweroff";
    public static final String POWERON = "poweron";
    public static final String PLAYERRADIUS = "playerradius";
    public static final String AFFECTONLYNEAREST = "affectonlynearest";

    public SMSRedstoneView(String name, SMSMenu menu) {
        super(name, menu);

        registerAttribute(POWERON, "", "Item to run when redstone power goes on");
        registerAttribute(POWEROFF, "", "Item to run when redstone power goes off");
        registerAttribute(POWERTOGGLE, "", "Item to run when redstone power changes");
        registerAttribute(PLAYERRADIUS, 0.0, "Command will be run on players within this radius");
        registerAttribute(AFFECTONLYNEAREST, true, "If true, command will only be run on nearest player");
    }

    public SMSRedstoneView(SMSMenu menu) {
        this(null, menu);
    }

    public SMSRedstoneView(SMSMenu menu, Location loc) throws SMSException {
        this(menu);

        addLocation(loc);
    }

    @Override
    public void update(Observable menu, Object arg1) {
        // A redstone view doesn't have any visual appearance to redraw
    }

    @Override
    public String getType() {
        return "redstone";
    }

    @Override
    public String toString() {
        Location[] locs = getLocationsArray();
        return "redstone @ " + (locs.length == 0 ? "NONE" : MiscUtil.formatLocation(locs[0]));
    }

    private void execute(Location loc, String attr) {
        try {
            String label = getAttributeAsString(attr);
            if (label == null || label.isEmpty())
                return;
            SMSMenuItem item = getNativeMenu().getItem(label);
            List<Player> players = getAffectedPlayers(loc);
            if (item != null) {
                if (players != null) {
                    // run the command for each affected player
                    for (Player p : players) {
                        if (PermissionUtils.isAllowedTo(p, "scrollingmenusign.use.redstone")) {
                            item.executeCommand(p, this);
                            item.feedbackMessage(p);
                        }
                    }
                } else {
                    // no affected players - run this as a console command
                    item.executeCommand(Bukkit.getConsoleSender(), this);
                }
            } else {
                LogUtils.warning("No such menu item '" + label + "' in menu " + getNativeMenu().getName());
            }
        } catch (SMSException e) {
            LogUtils.warning(e.getMessage());
        }
    }

    /**
     * Get a list of the players affected by this view during an execution event.  Returns null
     * if this view doesn't affect players (PLAYERRADIUS <= 0), or a list of players (which may
     * be empty) otherwise.  If AFFECTONLYNEAREST is true, then the list will contain one element
     * only - the closest player to the view.
     *
     * @param loc The view's location - where the event occurred
     * @return A list of affected players
     */
    private List<Player> getAffectedPlayers(Location loc) {
        double radius = (Double) getAttribute(PLAYERRADIUS);
        if (radius <= 0) {
            return null;
        }
        double radius2 = radius * radius;

        double minDist = Double.MAX_VALUE;
        List<Player> res = new ArrayList<Player>();

        if ((Boolean) getAttribute(AFFECTONLYNEAREST)) {
            // get a list containing only the closest player (who must also be within PLAYERRADIUS)
            Player closest = null;
            for (Player p : loc.getWorld().getPlayers()) {
                double dist = p.getLocation().distanceSquared(loc);
                if (dist < radius2 && dist < minDist) {
                    closest = p;
                    minDist = dist;
                }
            }
            if (closest != null) {
                res.add(closest);
            }
        } else {
            // get a list of all players within PLAYERRADIUS
            for (Player p : loc.getWorld().getPlayers()) {
                double dist = p.getLocation().distanceSquared(loc);
                if (dist < radius2) {
                    res.add(p);
                }
            }
        }

        return res;
    }

    /**
     * Check if the power level for the given location has changed
     *
     * @param loc        The location to check
     * @param newCurrent The new current at the given location
     * @return true if the new current represents a power level different from the block's current powered status
     */
    public boolean hasPowerChanged(Location loc, int newCurrent) {
        boolean curPower = loc.getBlock().isBlockPowered() || loc.getBlock().isBlockIndirectlyPowered();

        boolean newPower = newCurrent > 0;
        return curPower != newPower;
    }

    @Override
    public void processEvent(ScrollingMenuSign plugin, BlockRedstoneEvent event) {
        Block b = event.getBlock();

        Debugger.getInstance().debug("block redstone event @ " + b.getLocation() + ", view = "
                + getName() + ", menu = " + getNativeMenu().getName()
                + ", current = " + event.getOldCurrent() + "->" + event.getNewCurrent());

        if (event.getNewCurrent() > event.getOldCurrent()) {
            execute(b.getLocation(), POWERON);
        } else if (event.getOldCurrent() > event.getNewCurrent()) {
            execute(b.getLocation(), POWEROFF);
        }
        if (event.getOldCurrent() != event.getNewCurrent()) {
            execute(b.getLocation(), POWERTOGGLE);
        }
    }

    /* (non-Javadoc)
     * @see me.desht.scrollingmenusign.views.SMSView#onConfigurationValidate(me.desht.dhutils.ConfigurationManager, java.lang.String, java.lang.String)
     */
    @Override
    public void onConfigurationValidate(ConfigurationManager configurationManager, String attribute, Object oldVal, Object newVal) {
        super.onConfigurationValidate(configurationManager, attribute, oldVal, newVal);

        if (attribute.equals(POWERON) || attribute.equals(POWEROFF) || attribute.equals(POWERTOGGLE)) {
            String label = newVal.toString();
            if (!label.isEmpty()) {
                if (getNativeMenu().indexOfItem(label) == -1) {
                    throw new SMSException("Menu " + getNativeMenu().getName() + " does not contain the item '" + newVal + "'");
                }
            }
        }
    }
}
