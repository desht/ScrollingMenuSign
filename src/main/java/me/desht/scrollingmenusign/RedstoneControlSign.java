package me.desht.scrollingmenusign;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import me.desht.dhutils.BlockFaceUtil;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PersistableLocation;
import me.desht.scrollingmenusign.enums.SMSUserAction;
import me.desht.scrollingmenusign.views.SMSGlobalScrollableView;
import me.desht.scrollingmenusign.views.SMSView;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.util.Vector;

public class RedstoneControlSign {
	private static final Map<PersistableLocation, RedstoneControlSign> allSigns = new HashMap<PersistableLocation, RedstoneControlSign>();

	private static final Map<String, Set<Vector>> deferred = new HashMap<String, Set<Vector>>();

	private final PersistableLocation location;
	private final SMSGlobalScrollableView view;
	private final List<RedstoneControlSign.Action> actions = new ArrayList<RedstoneControlSign.Action>();

	private int lastPowerLevel;

	/**
	 * Construct a new RedstoneControlSign for the given Sign and view.  Private constructor - use getControlSign().
	 * 
	 * @param sign	the Sign
	 * @param view	the globally-scrollable view
	 * @throws SMSException if the sign text is any way invalid
	 */
	private RedstoneControlSign(Sign sign, SMSGlobalScrollableView view) {
		this.location = new PersistableLocation(sign.getLocation());

		SMSValidate.isTrue(sign.getLine(0).equals(ChatColor.RED + "[smsred]"),
		                   "Sign @ " + MiscUtil.formatLocation(sign.getBlock().getLocation()) + " is not a SMS redstone control sign");

		if (view == null) {
			SMSView baseView = SMSView.getView(sign.getLine(1));
			SMSValidate.isTrue(baseView instanceof SMSGlobalScrollableView, "view " + sign.getLine(1) + " is not a globally-scrollable view");
			this.view = (SMSGlobalScrollableView) baseView;
		} else {
			this.view = view;
		}

		String line23 = sign.getLine(2) + " " + sign.getLine(3);
		for (String action : line23.split("\\s+")) {
			parseAction(action);
		}

		lastPowerLevel = sign.getBlock().getBlockPower();

		this.view.addControlSign(this);
		this.view.autosave();
	}

	/**
	 * Get a new RedstoneControlSign for the given location.  The block must contain a sign, of which
	 * the first line must read "[smsred]" in red text, and the second line must contain the name of
	 * a globally-scrollable view.
	 * 
	 * @param loc	The location to check for
	 * @return	The RedstoneControlSign at that location.
	 * @throws SMSException if there is no sign at this block or the sign is not valid
	 */
	public static RedstoneControlSign getControlSign(Location loc) {
		return getControlSign(loc, null);
	}

	/**
	 * Get a new RedstoneControlSign for the given block and view object.  This is called when restoring
	 * RedstoneControlSign from disk.
	 * 
	 * @param block	the block to check
	 * @param view	the view this is associated with
	 * @return the RedstoneControlSign at this block
	 * @throws SMSException if there is no sign at this block or the sign is not valid
	 */
	public static RedstoneControlSign getControlSign(Location loc, SMSGlobalScrollableView view) {
		Block block = loc.getBlock();
		SMSValidate.isTrue(block.getType() == Material.WALL_SIGN || block.getType() == Material.SIGN_POST,
				"Block @ " + MiscUtil.formatLocation(block.getLocation()) + " does not contain a sign");
		PersistableLocation pLoc = new PersistableLocation(loc);
		if (!allSigns.containsKey(pLoc)) {
			allSigns.put(pLoc, new RedstoneControlSign((Sign) block.getState(), view));
		}
		return allSigns.get(pLoc);
	}

	/**
	 * Get the last-recorded power level for this control sign.
	 * 
	 * @return	the last-recorded power level
	 */
	public int getLastPowerLevel() {
		return lastPowerLevel;
	}

	/**
	 * Set the last-recorded power level for this control sign.
	 * 
	 * @param lastPowerLevel the power level to record
	 */
	public void setLastPowerLevel(int lastPowerLevel) {
		this.lastPowerLevel = lastPowerLevel;
	}

	/**
	 * Get the location of this control sign.
	 * 
	 * @return	the control sign location
	 */
	public Location getlocation() {
		return location.getLocation();
	}

	/**
	 * Get the view that this control sign is attached to.
	 * 
	 * @return	the view object
	 */
	public SMSGlobalScrollableView getView() {
		return view;
	}

	/**
	 * Delete this control sign, detaching it from its view.
	 */
	public void delete() {
		allSigns.remove(location);
		view.removeControlSign(this);
		view.autosave();
	}

	public boolean isAttached() {
		Block b = getlocation().getBlock();
		BlockState bs = b.getState();

		if (bs instanceof Sign) {
			org.bukkit.material.Sign s = (org.bukkit.material.Sign) bs.getData();
			Block attached = b.getRelative(s.getAttachedFace());
			return attached.getType() != Material.AIR;
		}
		return true;
	}

	/**
	 * Process the actions for this control sign.  Iterate through each defined adjacent block, and when one is 
	 * found that is powered, execute the associated action on the control sign's view.  Stop processing as soon as
	 * a powered block is found.
	 */
	public void processActions() {
		for (Action a : actions) {
			LogUtils.fine("processActions: check " + a.block + " power = " + a.block.isBlockPowered() + "/" + a.block.isBlockIndirectlyPowered());
			if (a.block.isBlockPowered() || a.block.isBlockIndirectlyPowered()) {
				String k = "sms.redstonecontrol." + a.action.toString().toLowerCase();
				if (ScrollingMenuSign.getInstance().getConfig().getBoolean(k)) {
					LogUtils.fine("processActions: view=" + view.getName() + " action=" + a.action);
					a.action.execute(null, view);
					break;
				}
			}
		}
	}

	/**
	 * Retrieve the org.bukkit.block.Sign object from this control sign.
	 * 
	 * @return	the Sign object
	 */
	private Sign getSignBlock() {
		Block b = location.getLocation().getBlock();
		SMSValidate.isTrue(b.getType() == Material.WALL_SIGN || b.getType() == Material.SIGN_POST, "block " + b + " is not a Sign!");
		return (Sign)b.getState();
	}

	/**
	 * Parse the action definitions in the given string.  This would be taken from lines 3 & 4 of
	 * the sign.
	 * 
	 * @param action	The action string, containing a whitespace-separate list of location/action pairs.
	 */
	private void parseAction(String action) {
		Sign sign = getSignBlock();
		org.bukkit.material.Sign signData = (org.bukkit.material.Sign) sign.getData();

		SMSValidate.isTrue(action.length() == 2,
				"Invalid redstone control spec. '" + action +  "' for sign @ " +  MiscUtil.formatLocation(sign.getBlock().getLocation()));

		BlockFace face;
		switch (Character.toLowerCase(action.charAt(0))) {
		case 'o':	// over
			face = BlockFace.UP;
			break;
		case 'u':	// under
			face = BlockFace.DOWN;
			break;
		case 'f':	// front
			face = signData.getFacing();
			break;
		case 'b':	// back
			face = signData.getFacing().getOppositeFace();
			break;
		case 'l':	// left
			face = BlockFaceUtil.getLeft(signData.getFacing().getOppositeFace());
			break;
		case 'r':	// right
			face = BlockFaceUtil.getLeft(signData.getFacing());
			break;
		default:
			throw new SMSException("Invalid redstone control direction '" + action.charAt(0) + "'");	
		}

		SMSUserAction userAction;
		switch (Character.toLowerCase(action.charAt(1))) {
		case 'x':	// execute
			userAction = SMSUserAction.EXECUTE; break;
		case 'u':	// execute
			userAction = SMSUserAction.SCROLLUP; break;
		case 'd':	// execute
			userAction = SMSUserAction.SCROLLDOWN; break;
		default:
			throw new SMSException("Invalid redstone control action '" + action.charAt(1) + "'");
		}

		actions.add(new Action(face, sign.getBlock().getRelative(face), userAction));
	}

	/**
	 * Check if there is a RedstoneControlSign at the given location.
	 * 
	 * @param loc the location to check for
	 * @return	true if there is a control sign there, false otherwise
	 */
	public static boolean checkForSign(Location loc) {
		return allSigns.containsKey(new PersistableLocation(loc));
	}

	/**
	 * Get the existing control sign at the given loc, if any.
	 * 
	 * @param loc	The location to check for
	 * @return	The control sign object, or null if no control is at the given location.
	 */
	public static RedstoneControlSign getSignAt(Location loc) {
		return allSigns.get(new PersistableLocation(loc));
	}

	/**
	 * Mark a control sign as deferred - do this if the world isn't loaded at this point.
	 * 
	 * @param worldName	The name of the world
	 * @param pos	a Vector representing the position of the sign
	 */
	public static void deferLoading(String worldName, Vector pos) {
		if (!deferred.containsKey(worldName)) {
			deferred.put(worldName, new HashSet<Vector>());
		}
		deferred.get(worldName).add(pos);
	}

	/**
	 * Load any deferred control signs for the given world.  Called from the
	 * WorldLoadEvent handler.
	 * 
	 * @param world	The world that's been loaded
	 */
	public static void loadDeferred(World world) {
		String worldName = world.getName();
		if (!deferred.containsKey(worldName)) {
			return;
		}
		for (Vector pos : deferred.get(world.getName())) {
			Location loc = new Location(world, pos.getX(), pos.getY(), pos.getZ());
			getControlSign(loc);
		}
		deferred.remove(world.getName());
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(MiscUtil.formatLocation(location.getLocation())).append(": ");
		for (Action a : actions) {
			sb.append(a.toString()).append(" ");
		}
		return sb.toString();
	}

	private class Action {
		BlockFace face;
		Block block;
		SMSUserAction action;

		Action (BlockFace face, Block block, SMSUserAction action) {
			this.face = face;
			this.block = block;
			this.action = action;
			LogUtils.fine("redstone control: create power-on action: " + block + " = " + action);
		}

		@Override
		public String toString() {
			return face + "=" + action.getShortDesc();
		}
	}
}
