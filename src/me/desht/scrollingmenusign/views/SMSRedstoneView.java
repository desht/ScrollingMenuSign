package me.desht.scrollingmenusign.views;

import java.util.HashMap;
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
	private static final String USENEAREST = "usenearestplayer";

	private final Map<Location,Boolean> powered = new HashMap<Location, Boolean>();

	public SMSRedstoneView(String name, SMSMenu menu) {
		super(name, menu);

		registerAttribute(POWERON, "");
		registerAttribute(POWEROFF, "");
		registerAttribute(POWERTOGGLE, "");
		registerAttribute(USENEAREST, false);
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
		// A redstone view doesn't really have any visual appearance to redraw
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

	public void handlePowerChange(Location loc, int newCurrent) {
		boolean curPower = isPowered(loc);
		boolean newPower = newCurrent > 0;
//		System.out.println("handle power change " + getName() + ": " + curPower + " -> " + newPower);

		if (newPower && !curPower) {
			execute(POWERON);
		} else if (curPower && !newPower) {
			execute(POWEROFF);
		}

		if (curPower != newPower) {
			execute(POWERTOGGLE);
		}

		setPowered(loc, newPower);
	}

	private void execute(String attr) {
		try {
			String label = getAttributeAsString(attr);
			if (label == null || label.isEmpty())
				return;
			SMSMenuItem item = getMenu().getItem(label);
			Player player = findNearestPlayer();
			if (item != null) {
				item.execute(player);
			} else {
				MiscUtil.log(Level.WARNING, "No such menu item '" + label + "' in menu " + getMenu().getName());
			}
		} catch (SMSException e) {
			MiscUtil.log(Level.WARNING, e.getMessage());
		}
	}

	private Player findNearestPlayer() {
		Boolean f = (Boolean) getAttribute(USENEAREST);
		if (f == null || !f	) {
			return null;
		}
		if (getLocations().isEmpty()) {
			return null;
		}
		
		Player closest = null;
		double minDist = Double.MAX_VALUE;
		
		Location viewLoc = getLocationsArray()[0];
		for (Player p : viewLoc.getWorld().getPlayers()) {
			if (p.getLocation().distance(viewLoc) < minDist) {
				closest = p;
			}
		}
		
//		for (int x = viewLoc.getBlockX() - 16; x <= viewLoc.getBlockX() + 16; x += 16) {
//			for (int z = viewLoc.getBlockZ() - 16; z <= viewLoc.getBlockZ() + 16; z += 16) {
//				Chunk chunk = w.getChunkAt(x, z);
//				System.out.println("chunk " + chunk);
//				for (Entity ent : chunk.getEntities()) {
//					System.out.println("got entity " + ent.getClass() + " @ " + ent.getLocation());
//					if (!(ent instanceof Player))
//						continue;
//					if (ent.getLocation().distance(viewLoc) < minDist) {
//						closest = (Player) ent;
//					}
//				}
//			}
//		}
		
//		System.out.println("found nearest player: " + closest);
		return closest;
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
