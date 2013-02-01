package me.desht.scrollingmenusign.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSMenuItem;
import me.desht.dhutils.ConfigurationManager;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PermissionUtils;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.PersistableLocation;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.material.Attachable;

public class SMSRedstoneView extends SMSView {

	// attributes
	public static final String POWERTOGGLE = "powertoggle";
	public static final String POWEROFF = "poweroff";
	public static final String POWERON = "poweron";
	public static final String PLAYERRADIUS = "playerradius";
	public static final String AFFECTONLYNEAREST = "affectonlynearest";

	private final Map<PersistableLocation,Boolean> powered = new HashMap<PersistableLocation, Boolean>();

	public SMSRedstoneView(String name, SMSMenu menu) {
		super(name, menu);

		registerAttribute(POWERON, "");
		registerAttribute(POWEROFF, "");
		registerAttribute(POWERTOGGLE, "");
		registerAttribute(PLAYERRADIUS, 0.0);
		registerAttribute(AFFECTONLYNEAREST, true);
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
	public void erase() {
		// A redstone view doesn't have any visual appearance to erase
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

	private void handlePowerChange(Location loc, int newCurrent) {
		boolean curPower = isPowered(loc);
		boolean newPower = newCurrent > 0;

		if (newPower && !curPower) {
			execute(loc, POWERON);
		} else if (curPower && !newPower) {
			execute(loc, POWEROFF);
		}
		if (curPower != newPower) {
			execute(loc, POWERTOGGLE);
		}

		setPowered(loc, newPower);
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
	 * @return	A list of affected players
	 */
	private List<Player> getAffectedPlayers(Location loc) {
		Double radius = (Double) getAttribute(PLAYERRADIUS);
		if (radius <= 0) {
			return null;
		}
		radius *= radius;
		
		double minDist = Double.MAX_VALUE;
		List<Player> res = new ArrayList<Player>();

		if ((Boolean) getAttribute(AFFECTONLYNEAREST)) {
			// get a list containing only the closest player (who must also be within PLAYERRADIUS)
			Player closest = null;
			for (Player p : loc.getWorld().getPlayers()) {
				double dist = p.getLocation().distanceSquared(loc);
				if (dist < radius && dist < minDist) {
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
				if (dist < radius) {
					res.add(p);
				}
			}
		}
		
		return res;
	}

	/* (non-Javadoc)
	 * @see me.desht.scrollingmenusign.views.SMSView#addLocation(org.bukkit.Location)
	 */
	@Override
	public void addLocation(Location loc) throws SMSException {
		powered.put(new PersistableLocation(loc), loc.getBlock().isBlockPowered() || loc.getBlock().isBlockIndirectlyPowered());

		super.addLocation(loc);
	}

	/* (non-Javadoc)
	 * @see me.desht.scrollingmenusign.views.SMSView#removeLocation(org.bukkit.Location)
	 */
	@Override
	public void removeLocation(Location loc) {
		powered.remove(new PersistableLocation(loc));

		super.removeLocation(loc);
	}

	/**
	 * Check if the view believe the given location is currently powered
	 * 
	 * @param loc	The location to check
	 * @return		true if the location is considered powered, false otherwise
	 */
	public boolean isPowered(Location loc) {
		return powered.get(new PersistableLocation(loc));
	}

	/**
	 * Record the current location as being powered or not
	 * 
	 * @param loc		The location to record
	 * @param power		The power level
	 */
	public void setPowered(Location loc, boolean power) {
		powered.put(new PersistableLocation(loc), power);
	}

	/**
	 * Check if the power level for the given location has changed
	 * 
	 * @param loc	The location to check
	 * @param newCurrent	The new current at the given location
	 * @return	true if the new current represents a power level different from the block's current powered status
	 */
	public boolean hasPowerChanged(Location loc, int newCurrent) {
		boolean curPower = loc.getBlock().isBlockPowered() || loc.getBlock().isBlockIndirectlyPowered();

		boolean newPower = newCurrent > 0;
		return curPower != newPower;
	}

	/**
	 * Convenience method.  Create a new redstone view at the given location and add it 
	 * to the given menu.
	 * 
	 * @param menu	The menu to add the view to.
	 * @param loc	The location for the view.
	 * @throws SMSException if the location is not suitable for this view
	 */
	public static SMSView addRedstoneViewToMenu(String viewName, SMSMenu menu, Location loc) throws SMSException {
		SMSView view = new SMSRedstoneView(viewName, menu);
		view.addLocation(loc);
		view.register();
		return view;
	}
	public static SMSView addRedstoneViewToMenu(SMSMenu menu, Location loc) throws SMSException {
		return addRedstoneViewToMenu(null, menu, loc);
	}

	/**
	 * Get the redstone view at the given location, if any.
	 * 
	 * @param loc	The location to check
	 * @return	The view if any exists, null otherwise
	 */
	public static SMSRedstoneView getRedstoneViewForLocation(Location loc) {
		SMSView v = SMSView.getViewForLocation(loc);
		if (v != null && v instanceof SMSRedstoneView) {
			return (SMSRedstoneView) v;
		} else {
			return null;
		}
	}

	/**
	 * Process a BlockRedstoneEvent that just occurred.  The event's block is assumed to have already
	 * been verified as belonging to a redstone view.
	 * 
	 * @param event		The event that just occurred
	 */
	public static void processRedstoneEvent(BlockRedstoneEvent event) {
		Block block = event.getBlock();

		// the block itself could be a view
		checkNeighbour(event, BlockFace.SELF);

		//		System.out.println("redstone event: " + block.getLocation() + " : "+ event.getOldCurrent() + " -> " + event.getNewCurrent());
		switch (block.getType()) {
		case REDSTONE_WIRE:
			// check the block below and the 4 blocks adjacent
			checkNeighbour(event, BlockFace.DOWN);
			checkNeighbour(event, BlockFace.NORTH);
			checkNeighbour(event, BlockFace.SOUTH);
			checkNeighbour(event, BlockFace.EAST);
			checkNeighbour(event, BlockFace.WEST);
			break;
		case REDSTONE_TORCH_OFF:
		case REDSTONE_TORCH_ON:
			// check the block above
			checkNeighbour(event, BlockFace.UP);
			break;
		case STONE_BUTTON:
		case LEVER:
			// check the attached block
			BlockFace face = ((Attachable) block.getState().getData()).getAttachedFace();
			checkNeighbour(event, face);
			break;
		case WOOD_PLATE:
		case STONE_PLATE:
			// check the block below
			checkNeighbour(event, BlockFace.DOWN);
		default:
			break;
		}
	}

	private static void checkNeighbour(BlockRedstoneEvent event, BlockFace face) {
		Block block = event.getBlock();
		Block neighbour = block.getRelative(face);
		SMSRedstoneView rv = getRedstoneViewForLocation(neighbour.getLocation());

		if (rv != null && rv.hasPowerChanged(neighbour.getLocation(), event.getNewCurrent())) {
			LogUtils.fine("block redstone event @ " + neighbour.getLocation() + ", view = " +
					rv.getName() + ", menu = " + rv.getNativeMenu().getName() + ", new current = " + event.getNewCurrent());
			rv.handlePowerChange(neighbour.getLocation(), event.getNewCurrent());
		}
	}
	
	/* (non-Javadoc)
	 * @see me.desht.scrollingmenusign.views.SMSView#onConfigurationValidate(me.desht.dhutils.ConfigurationManager, java.lang.String, java.lang.String)
	 */
	@Override
	public void onConfigurationValidate(ConfigurationManager configurationManager, String attribute, String newVal) {
		super.onConfigurationValidate(configurationManager, attribute, newVal);
		
		if (attribute.equals(POWERON) || attribute.equals(POWEROFF) || attribute.equals(POWERTOGGLE)) {
			if (!newVal.isEmpty()) {
				if (getNativeMenu().indexOfItem(newVal) == -1) {
					throw new SMSException("Menu " + getNativeMenu().getName() + " does not contain the item '" + newVal + "'");
				}
			}
		}
	}
}
