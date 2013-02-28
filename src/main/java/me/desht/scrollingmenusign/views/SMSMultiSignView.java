package me.desht.scrollingmenusign.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;

import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PersistableLocation;
import me.desht.dhutils.Str;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSMenuItem;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.scrollingmenusign.enums.ViewJustification;
import me.desht.dhutils.LogUtils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.material.Sign;

public class SMSMultiSignView extends SMSGlobalScrollableView {

	private BlockFace facing;
	private PersistableLocation topLeft;
	private PersistableLocation bottomRight;
	private int height; // in blocks
	private int width;  // in blocks

	private final Map<Location, String[]> updates = new HashMap<Location, String[]>();

	/**
	 * Create a new multi-sign view object with no registered location.  A location
	 * which contains a sign must be added with @see #addLocation(Location) before
	 * this view is useful.
	 * 
	 * @param name	Unique name for this view.
	 * @param menu	The SMSMenu object to attach this view to.
	 */
	public SMSMultiSignView(String name, SMSMenu menu) {
		super(name, menu);

		this.setMaxLocations(100);  // arbitrary maximum
	}

	/**
	 * Create a new multi-sign view at loc.  The wall signs around loc will be scanned to work out just
	 * what signs comprise this view.
	 * 
	 * @param name
	 * @param menu
	 * @param loc
	 * @throws SMSException
	 */
	public SMSMultiSignView(String name, SMSMenu menu, Location loc) throws SMSException {
		this(name, menu);

		scanForSigns(loc);
		for (Block b : getBlocks()) {
			addLocation(b.getLocation());	
		}
	}

	/**
	 * Create a new sign view object.  Equivalent to calling SMSSignView(null, menu, loc).  The
	 * view's name will be automatically generated, based on the menu name.
	 * 
	 * @param menu	The SMSMenu object to attach this view to.
	 * @param loc	The location of this view's sign
	 * @throws SMSException	if the given location is not suitable for this view
	 */
	public SMSMultiSignView(SMSMenu menu, Location loc) throws SMSException {
		this(null, menu, loc);
	}


	/* (non-Javadoc)
	 * @see me.desht.scrollingmenusign.views.SMSGlobalScrollableView#thaw(org.bukkit.configuration.ConfigurationSection)
	 */
	@Override
	public void thaw(ConfigurationSection node) throws SMSException {
		super.thaw(node);

		scanForSigns(getLocationsArray()[0]);
	}

	/* (non-Javadoc)
	 * @see me.desht.scrollingmenusign.views.SMSScrollableView#update(java.util.Observable, java.lang.Object)
	 */
	@Override
	public void update(Observable menu, Object arg) {
		super.update(menu, arg);
		
		switch ((SMSMenuAction) arg) {
		case REPAINT: case SCROLLED:
			repaintAll();
			break;
		case DELETE_PERM:
			erase();
			break;
		default:
			break;
		}
	}

	@Override
	public void erase() {
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				org.bukkit.block.Sign s = getSign(x, y);
				if (s != null) {
					for (int i = 0; i < 4; i++) {
						s.setLine(i, "");
					}
					s.update();
				}
			}
		}
	}

	private void repaintAll() {
		String prefixNotSel = ScrollingMenuSign.getInstance().getConfig().getString("sms.item_prefix.not_selected", "  ").replace("%", "%%"); 
		String prefixSel = ScrollingMenuSign.getInstance().getConfig().getString("sms.item_prefix.selected", "> ").replace("%", "%%");

		List<String> titleLines = formatTitle();
		int nTitleLines = titleLines.size();
		for (int i = 0; i < nTitleLines; i++) {
			drawText(i, titleLines.get(i));
		}

		int current = getScrollPos();
		int nItems = getActiveMenuItemCount(null);
		int nDisplayable = height * 4 - nTitleLines;
		if (nItems > 0) {
			for (int n = 0; n < nDisplayable; n++) {
				SMSMenuItem item = getActiveMenuItemAt(null, current);
				String lineText;
				if (n < nItems) {
					lineText = item == null ? "???" : item.getLabel();
				} else {
					// no more menu items - blank lines to the bottom of the view
					lineText = "";
				}
				LogUtils.finer("SMSMultiSignView: update: current=" + current + " line=" + n + " text=[" + lineText + "]");
				drawText(n + nTitleLines, formatItem(n == 0 ? prefixSel : prefixNotSel, lineText));
				current++;
				if (current > nItems)
					current = 1;
			}
		}

		applyUpdates();
	}

	/**
	 * Draw a line of text at the given line, which will potentially span multiple signs.
	 * Colour/markup codes are preserved across signs, which may lead to unexpectedly few
	 * printable characters appearing on each sign if a lot of markup is used!
	 * 
	 * @param line	The line number on which to draw the text
	 * @param text	The text to draw
	 */
	public void drawText(int line, String text) {
		int y = line / 4;

		LogUtils.finest("drawLine: line=" + line + "  text=[" + text + "]");
		int begin = 0;
		int x = 0;
		String ctrlColour = "";
		String ctrlOther = "";
		while (x < width) {
			String ctrl = ctrlColour + ctrlOther;
			int end = Math.min(begin + (15 - ctrl.length()), text.length()); 
			String sub = ctrl + text.substring(begin, end);
			if (sub.endsWith("\u00a7")) {
				// we can't have a control char split over 2 signs
				sub = sub.replaceAll("\u00a7$", "");
			}
			ctrlColour = ctrlOther = "";
			for (int i = 0; i < sub.length() - 1; i++) {
				char c = sub.charAt(i), c1 = Character.toLowerCase(sub.charAt(i + 1));
				if (c == '\u00a7') {
					if (c1 == 'r') {
						ctrlColour = ctrlOther = "";
					} else if (isHexDigit(c1)) {
						ctrlColour = "\u00a7" + c1;
					} else {
						ctrlOther += "\u00a7" + c1;
					}
				}
			}
			Location loc = getSignLocation(x, y);
			LogUtils.finest("drawLine: sub = [" + sub + "] @" + x + "," + y + loc + " line=" + line % 4);
			pendingUpdate(loc, line % 4, sub);
			begin += sub.length() - ctrl.length();
			x++;
		}
	}

	/* (non-Javadoc)
	 * @see me.desht.scrollingmenusign.views.SMSView#getType()
	 */
	@Override
	public String getType() {
		return "multisign";
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "multisign @ " + MiscUtil.formatLocation(topLeft.getLocation()) + " (" + width + "x" + height + ")";
	}

	/* (non-Javadoc)
	 * @see me.desht.scrollingmenusign.views.SMSView#addLocation(org.bukkit.Location)
	 */
	@Override
	public void addLocation(Location loc) throws SMSException {
		Block b = loc.getBlock();
		if (b.getType() != Material.WALL_SIGN) {
			throw new SMSException("Location " + MiscUtil.formatLocation(loc) + " does not contain a wall sign.");
		}

		super.addLocation(loc);
	}

	/**
	 * Get the Sign at position (x,y) in the view.  (x, y) = (0, 0) is the top left sign.
	 * x increases to the right, y increases downward.  This works regardless of sign orientation.
	 * 
	 * @param x		X co-ordinate
	 * @param y		Y co-ordinate
	 * @return	the Sign block retrieved
	 */
	public org.bukkit.block.Sign getSign(int x, int y) {
		Block b = getSignLocation(x, y).getBlock();
		if (b.getType() == Material.WALL_SIGN) {
			return (org.bukkit.block.Sign) b.getState();
		} else {
			return null;
		}
	}
	
	/**
	 * Get the location that position (x,y) in the view maps to.  (x, y) = (0, 0) is the top left sign.
	 * x increases to the right, y increases downward.  This works regardless of sign orientation.
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public Location getSignLocation(int x, int y) {
		Location tl = topLeft.getLocation();
		
		BlockFace toLeft = getLeft(facing);
		
		int x1 = tl.getBlockX() + toLeft.getModX() * x;
		int y1 = tl.getBlockY() - y;
		int z1 = tl.getBlockZ() + toLeft.getModZ() * x;
		
		return new Location(tl.getWorld(), x1, y1, z1);
	}

	public BlockFace getLeft(BlockFace face) {
		switch (face) {
		case NORTH: return BlockFace.WEST;
		case EAST: return BlockFace.NORTH;
		case SOUTH: return BlockFace.EAST;
		case WEST: return BlockFace.SOUTH;
		default: throw new IllegalArgumentException("unsupported face");
		}
	}
	
	/**
	 * Mark one line on a given sign as requiring an update.
	 * 
	 * @param s
	 * @param line
	 * @param text
	 */
	private void pendingUpdate(Location loc, int line, String text) {
		if (!updates.containsKey(loc)) {
			updates.put(loc, new String[4]);
		}
		String[] lines = updates.get(loc);
		lines[line] = text;
	}

	/**
	 * Apply all the updates that have been marked as pending.  Doing them all at once means
	 * we only need to send world updates for each sign once.
	 */
	private void applyUpdates() {
		for (Entry<Location,String[]> e : updates.entrySet()) {
			Block b = e.getKey().getBlock();
			if (b.getType() != Material.WALL_SIGN) {
				continue;
			}
			org.bukkit.block.Sign s = (org.bukkit.block.Sign) b.getState();
			for (int i = 0; i < 4; i++) {
				String line = e.getValue()[i];
				if (line != null) {
					s.setLine(i, line);
				}
			}
			s.update();
		}
		updates.clear();
	}

	private void scanForSigns(Location startLoc) throws SMSException {
		Block b = startLoc.getBlock();
		if (b.getType() != Material.WALL_SIGN) {
			throw new SMSException("Location " + MiscUtil.formatLocation(b.getLocation()) + " does not contain a sign.");
		}

		Sign s = (Sign) b.getState().getData();
		facing = s.getFacing();

		switch (facing) {
		case NORTH:
			scan(b, BlockFace.EAST); break;
		case EAST:
			scan(b, BlockFace.SOUTH); break;
		case SOUTH:
			scan(b, BlockFace.WEST); break;
		case WEST:
			scan(b, BlockFace.NORTH); break;
		default:
			throw new SMSException("Unexpected sign direction " + facing);
		}
	}

	private void scan(Block b, BlockFace horizontal) throws SMSException {
		Location tl = scan(b, horizontal, BlockFace.UP);
		Location br = scan(b, horizontal.getOppositeFace(), BlockFace.DOWN);
		
		topLeft = new PersistableLocation(tl);
		bottomRight = new PersistableLocation(br);

		validateSignArray();

		height = (tl.getBlockY() - br.getBlockY()) + 1;
		switch (horizontal) {
		case NORTH: case SOUTH:
			width = Math.abs(tl.getBlockZ() - br.getBlockZ()) + 1;
			break;
		case EAST: case WEST:
			width = Math.abs(tl.getBlockX() - br.getBlockX()) + 1;
			break;
		default:
			break;
		}
		LogUtils.finer("multisign: topleft=" + topLeft + ", bottomright=" + bottomRight);
		LogUtils.finer("multisign: height=" + height + ", width=" + width);
	}

	private Location scan(Block b, BlockFace horizontal, BlockFace vertical) {
		Block b1 = b;

		LogUtils.finer("scan: " + b + " h=" + horizontal + " v=" + vertical);

		b1 = scanOneDir(b, horizontal);
		b1 = scanOneDir(b1, vertical);

		return b1.getLocation();
	}

	private Block scanOneDir(Block b, BlockFace dir) {
		while (b.getType() == Material.WALL_SIGN && SMSView.getViewForLocation(b.getLocation()) == null) {
			Sign s = (Sign) b.getState().getData();
			if (s.getFacing() != facing) {
				break;
			}
			b = b.getRelative(dir);
		}
		return b.getRelative(dir.getOppositeFace());
	}

	private List<Block> getBlocks() {
		List<Block> res = new ArrayList<Block>();
		
		Block tlb = topLeft.getLocation().getBlock();
		Block brb = bottomRight.getLocation().getBlock();

		int x1 = Math.min(tlb.getX(), brb.getX());
		int x2 = Math.max(tlb.getX(), brb.getX());
		int z1 = Math.min(tlb.getZ(), brb.getZ());
		int z2 = Math.max(tlb.getZ(), brb.getZ());
		int y1 = brb.getY();
		int y2 = tlb.getY();

		World w = tlb.getWorld();
		for (int x = x1; x <= x2; x++) {
			for (int y = y1; y <= y2; y++) {
				for (int z = z1; z <= z2; z++) {
					res.add(w.getBlockAt(x, y, z));
				}
			}
		}

		return res;
	}

	private void validateSignArray() throws SMSException {
		for (Block b : getBlocks()) {
			if (b.getType() != Material.WALL_SIGN) {
				throw new SMSException("Sign array is not rectangular!");
			}
		}
	}

	private String formatLine(String prefix, String text, ViewJustification just) {
		int l = 15 * width - prefix.length();
		String s = "";
		//		this regexp sadly doesn't work
		//		String reset = text.matches("\u00a7[mn]") ? "\u00a7r" : "";
		String reset = "";
		String textL = text.toLowerCase();
		if (textL.contains("\u00a7m") || textL.contains("\u00a7n")) {
			reset = "\u00a7r";
		}
		switch (just) {
		case LEFT:
			s = prefix + Str.padRight(text + reset, l);
			break;
		case CENTER:
			s = prefix + Str.padCenter(text + reset, l);
			break;
		case RIGHT:
			s = prefix + Str.padLeft(text + reset, l);
			break;
		default:
			break;
		}
		return MiscUtil.parseColourSpec(s);
	}

	private List<String> formatTitle() {
		List<String> lines = splitTitle(null);
		for (int i = 0; i < lines.size(); i++) {
			lines.set(i, formatLine("", lines.get(i), getTitleJustification()));
		}
		return lines;
	}

	private String formatItem(String prefix, String text) {
		return formatLine(prefix, variableSubs(text), getItemJustification());
	}

//	/**
//	 * Erase all the signs for this view.
//	 */
//	private void blankSigns() {
//		for (int x = 0; x < width; x++) {
//			for (int y = 0; y < height; y++) {
//				org.bukkit.block.Sign s = getSign(x, y);
//				if (s != null) {
//					for (int i = 0; i < 4; i++) {
//						s.setLine(i, "");
//					}
//					s.update();
//				}
//			}
//		}
//	}

	private boolean isHexDigit(char c) {
		return c >= '0' && c <= '9' || c >= 'a' && c <= 'f'	;
	}

	/**
	 * Convenience method.  Create a new multi-sign view at the given location.
	 * 
	 * @param menu
	 * @param location
	 * @return
	 * @throws SMSException
	 */
	public static SMSView addSignToMenu(String viewName, SMSMenu menu, Location location) throws SMSException {
		SMSView view = new SMSMultiSignView(viewName, menu, location);
		view.register();
		view.update(menu, SMSMenuAction.REPAINT);
		return view;
	}
	public static SMSView addSignToMenu(SMSMenu menu, Location location) throws SMSException {
		return addSignToMenu(null, menu, location);
	}

	@Override
	protected int getLineLength() {
		return 15 * width;
	}
	
	@Override
	protected int getHardMaxTitleLines() {
		return 4;
	}
}
