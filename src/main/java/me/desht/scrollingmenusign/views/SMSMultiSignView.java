package me.desht.scrollingmenusign.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;

import me.desht.scrollingmenusign.SMSConfig;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSMenuItem;
import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.scrollingmenusign.enums.ViewJustification;
import me.desht.scrollingmenusign.util.MiscUtil;
import me.desht.scrollingmenusign.util.SMSLogger;
import me.desht.scrollingmenusign.util.Str;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.material.Sign;

public class SMSMultiSignView extends SMSGlobalScrollableView {

	private BlockFace facing;
	private Block topLeft;
	private Block bottomRight;
	private int height; // in blocks
	private int width;  // in blocks
	
	private final Map<Location, String[]> updates = new HashMap<Location, String[]>();
	
	/**
	 * Create a new multi-sign view at loc.  The signs around loc will be scanned to work out just
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
		addBlocks();
	}

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
			
		this.setMaxLocations(100);
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
	
	@Override
	public void update(Observable menu, Object arg1) {
		if (!(menu instanceof SMSMenu))
			return;

		String prefix1 = SMSConfig.getConfig().getString("sms.item_prefix.not_selected", "  ");
		String prefix2 = SMSConfig.getConfig().getString("sms.item_prefix.selected", "> ");
		
		int current = getLastScrollPos();
		int nDisplayable = height * 4 - 1;
		int nItems = getMenu().getItemCount();
		
		drawLine(0, formatTitle());
		
		if (nItems > 0) {
			for (int n = 0; n < nDisplayable; n++) {
				SMSMenuItem item = getMenu().getItemAt(current);
				String lineText;
				if (n < nItems) {
					lineText = item == null ? "???" : item.getLabel();
				} else {
					// no more menu items - blank lines to the bottom of the view
					lineText = "";
				}
				SMSLogger.finer("SMSMultiSignView: update: current=" + current + " line=" + n + " text=[" + lineText + "]");
				drawLine(n + 1, formatItem(n == 0 ? prefix2 : prefix1, lineText));
				current++;
				if (current > nItems)
					current = 1;
			}
		}
		
		applyUpdates();
	}

	public void drawLine(int line, String text) {
		int y = line / 4;
		
		SMSLogger.finest("drawLine: line=" + line + "  text=[" + text + "]");
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
			SMSLogger.finest("drawLine: sub = [" + sub + "]");
			org.bukkit.block.Sign s = getSign(x, y);
			if (s != null) {
				SMSLogger.finest("drawLine: x=" + x + " y=" + y + " sign = " + s.getLocation() + " line = " + line % 4);
				pendingUpdate(s, line % 4, sub);
			}
			begin += sub.length() - ctrl.length();
			x++;
		}
	}
	
	private void pendingUpdate(org.bukkit.block.Sign s, int i, String sub) {
		Location l = s.getLocation();
		if (!updates.containsKey(l)) {
			updates.put(l, new String[4]);
		}
		String[] lines = updates.get(l);
		lines[i] = sub;
	}
	
	private void applyUpdates() {
		for (Entry<Location,String[]> e : updates.entrySet()) {
			Block b = e.getKey().getBlock();
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

	@Override
	public String getType() {
		return "multisign";
	}

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
	
	/* (non-Javadoc)
	 * @see me.desht.scrollingmenusign.views.SMSView#deletePermanent()
	 */
	@Override
	public void deletePermanent() {
		blankSigns();
		super.deletePermanent();
	}
	
	/**
	 * Return the Sign at position (x,y) in the view.  (x, y) = (0, 0) is the top left sign.
	 * x increases to the right, y increases downward.
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public org.bukkit.block.Sign getSign(int x, int y) {
		int y1 = topLeft.getY() - y;
		int x1, z1;
		switch (facing) {
		case NORTH:
			x1 = topLeft.getX();
			z1 = topLeft.getZ() + x;
			break;
		case SOUTH:
			x1 = topLeft.getX();
			z1 = topLeft.getZ() - x;
			break;
		case EAST:
			x1 = topLeft.getX() - x;
			z1 = topLeft.getZ();
			break;
		case WEST:
			x1 = topLeft.getX() + x;
			z1 = topLeft.getZ();
			break;
		default:
			throw new IllegalStateException("Unexpected facing " + facing + " for " + this);	
		}
		Block b = topLeft.getWorld().getBlockAt(x1, y1, z1);
		if (b.getType() == Material.WALL_SIGN) {
			return (org.bukkit.block.Sign) b.getState();
		} else {
			return null;
		}
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
		topLeft = scan(b, horizontal, BlockFace.UP);
		bottomRight = scan(b, horizontal.getOppositeFace(), BlockFace.DOWN);
		
		validateSignArray();
		
		height = (topLeft.getY() - bottomRight.getY()) + 1;
		switch (horizontal) {
		case NORTH: case SOUTH:
			width = Math.abs(topLeft.getX() - bottomRight.getX()) + 1;
			break;
		case EAST: case WEST:
			width = Math.abs(topLeft.getZ() - bottomRight.getZ()) + 1;
			break;
		}
		SMSLogger.finer("multisign: topleft=" + topLeft + ", bottomright=" + bottomRight);
		SMSLogger.finer("multisign: height=" + height + ", width=" + width);
	}

	private Block scan(Block b, BlockFace horizontal, BlockFace vertical) {
		Block b1 = b;
		
		SMSLogger.finer("scan: " + b + " h=" + horizontal + " v=" + vertical);
		
		b1 = scanOneDir(b, horizontal);
		b1 = scanOneDir(b1, vertical);
		
		return b1;
	}

	private Block scanOneDir(Block b, BlockFace dir) {
		while (b.getType() == Material.WALL_SIGN) {
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
		
		int x1 = Math.min(topLeft.getX(), bottomRight.getX());
		int x2 = Math.max(topLeft.getX(), bottomRight.getX());
		int z1 = Math.min(topLeft.getZ(), bottomRight.getZ());
		int z2 = Math.max(topLeft.getZ(), bottomRight.getZ());
		int y1 = bottomRight.getY();
		int y2 = topLeft.getY();
		
		World w = topLeft.getWorld();
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

	private void addBlocks() throws SMSException {
		for (Block b : getBlocks()) {
			addLocation(b.getLocation());	
		}
	}

	private String formatLine(String prefix, String text, ViewJustification just) {
		int l = 15 * width - prefix.length();
		String s = "";
		String reset = text.matches("\u00a7[mn]") ? "\u00a7r" : "";
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
		}
		return MiscUtil.parseColourSpec(null, s);
	}

	private String formatTitle() {
		return formatLine("", getMenu().getTitle(), getTitleJustification());
	}
	
	private String formatItem(String prefix, String text) {
		return formatLine(prefix, text, getItemJustification());
	}
	
	/**
	 * Erase all the signs for this view.
	 */
	private void blankSigns() {
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
	public static SMSView addSignToMenu(SMSMenu menu, Location location) throws SMSException {
		SMSView view = new SMSMultiSignView(menu, location);
		view.register();
		view.update(menu, SMSMenuAction.REPAINT);
		return view;
	}

}
