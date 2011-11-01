package me.desht.scrollingmenusign.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.logging.Level;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSMenuItem;
import me.desht.util.MiscUtil;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.material.Attachable;

public class SMSRedstoneView extends SMSView {

	private static final String POWERTOGGLE = "powertoggle";
	private static final String POWEROFF = "poweroff";
	private static final String POWERON = "poweron";
	private static final String PLAYERRADIUS = "playerradius";
	private static final String AFFECTONLYNEAREST = "affectonlynearest";

	private final Map<Location,Boolean> powered = new HashMap<Location, Boolean>();

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
	protected void thaw(ConfigurationSection node) {
		// No extra work to do here
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
		//		System.out.println("handle power change " + getName() + ": " + curPower + " -> " + newPower);

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
			SMSMenuItem item = getMenu().getItem(label);
			List<Player> players = getAffectedPlayers(loc);
			if (item != null) {
				if (players != null) {
					// run the command for each affected player
					for (Player p : players) {
						item.execute(p);
					}
				} else {
					// no affected players - run this as a console command
					item.execute(null);
				}
			} else {
				MiscUtil.log(Level.WARNING, "No such menu item '" + label + "' in menu " + getMenu().getName());
			}
		} catch (SMSException e) {
			MiscUtil.log(Level.WARNING, e.getMessage());
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

		double minDist = Double.MAX_VALUE;
		List<Player> res = new ArrayList<Player>();

		if ((Boolean) getAttribute(AFFECTONLYNEAREST)) {
			// get a list containing only the closest player (who must also be within PLAYERRADIUS)
			Player closest = null;
			for (Player p : loc.getWorld().getPlayers()) {
				double dist = p.getLocation().distance(loc);
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
				double dist = p.getLocation().distance(loc);
				if (dist < radius) {
					res.add(p);
				}
			}
		}
		
		return res;
	}

	@Override
	public void addLocation(Location loc) throws SMSException {
		powered.put(loc, loc.getBlock().isBlockPowered() || loc.getBlock().isBlockIndirectlyPowered());

		super.addLocation(loc);
	}

	@Override
	public void removeLocation(Location loc) {
		powered.remove(loc);

		super.removeLocation(loc);
	}

	public boolean isPowered(Location loc) {
		return powered.get(loc);
	}

	public void setPowered(Location loc, Boolean power) {
		powered.put(loc, power);
	}

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
	public static SMSView addRedstoneViewToMenu(SMSMenu menu, Location loc) throws SMSException {
		SMSView view = new SMSRedstoneView(menu, loc);
		view.register();
		return view;
	}

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
		}
	}

	private static void checkNeighbour(BlockRedstoneEvent event, BlockFace face) {
		Block block = event.getBlock();
		Block neighbour = block.getRelative(face);
		SMSRedstoneView rv = getRedstoneViewForLocation(neighbour.getLocation());

		if (rv != null && rv.hasPowerChanged(neighbour.getLocation(), event.getNewCurrent())) {
			rv.handlePowerChange(neighbour.getLocation(), event.getNewCurrent());
		}
	}
}
