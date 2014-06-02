package me.desht.scrollingmenusign.views;

import me.desht.dhutils.Debugger;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PersistableLocation;
import me.desht.dhutils.Str;
import me.desht.dhutils.block.BlockUtil;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.enums.ViewJustification;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.material.Sign;

import java.util.*;
import java.util.Map.Entry;

/**
 * This view draws menus on a rectangular array of signs.
 */
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
     * @param name Unique name for this view.
     * @param menu The SMSMenu object to attach this view to.
     */
    public SMSMultiSignView(String name, SMSMenu menu) {
        super(name, menu);

        this.setMaxLocations(100);  // arbitrary maximum
    }

    /**
     * Create a new multi-sign view at loc.  The wall signs around loc will be scanned to work out just
     * what signs comprise this view.
     *
     * @param name name of the new view
     * @param menu menu for the new view
     * @param loc  location of the new view
     * @throws SMSException if there was any problem creating the view
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
     * @param menu The SMSMenu object to attach this view to.
     * @param loc  The location of this view's sign
     * @throws SMSException if the given location is not suitable for this view
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
    public void update(Observable menu, Object arg1) {
        super.update(menu, arg1);

        repaintAll();
    }

    @Override
    public void onDeleted(boolean permanent) {
        super.onDeleted(permanent);
        if (permanent) {
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    org.bukkit.block.Sign s = getSign(x, y);
                    if (s != null) {
                        for (int i = 0; i < SIGN_LINES; i++) {
                            s.setLine(i, "");
                        }
                        s.update();
                    }
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
        for (int i = nTitleLines; i < height * SIGN_LINES; i++) {
            drawText(i, "");
        }

        int scrollPos = getScrollPos();
        int menuSize = getActiveMenuItemCount(null);
        int pageSize = height * SIGN_LINES - nTitleLines;
        switch (getScrollType()) {
            case SCROLL:
                for (int j = 0, pos = scrollPos; j < pageSize && j < menuSize; j++) {
                    String pre = j == 0 ? prefixSel : prefixNotSel;
                    String lineText = getActiveItemLabel(null, pos);
                    drawText(j + nTitleLines, formatItem(pre, lineText));
                    if (++pos > menuSize) {
                        pos = 1;
                    }
                }
                break;
            case PAGE:
                int pageNum = (scrollPos - 1) / pageSize;
                for (int j = 0, pos = (pageNum * pageSize) + 1; j < pageSize && pos <= menuSize; j++, pos++) {
                    String pre = pos == scrollPos ? prefixSel : prefixNotSel;
                    String lineText = getActiveItemLabel(null, pos);
                    drawText(j + nTitleLines, formatItem(pre, lineText));
                }
                break;
        }

        applyUpdates();
    }

    /**
     * Draw a line of text at the given line, which will potentially span multiple signs.
     * Colour/markup codes are preserved across signs, which may lead to unexpectedly few
     * printable characters appearing on each sign if a lot of markup is used!
     *
     * @param line The line number on which to draw the text
     * @param text The text to draw
     */
    public void drawText(int line, String text) {
        int y = line / SIGN_LINES;

        Debugger.getInstance().debug(2, "drawText: view=" + getName() + ", line=" + line + ", text=[" + text + "]");
        int begin = 0;
        if (width == 1) {
            // optimised case; avoid line-splitting calculations
            pendingUpdate(getSignLocation(0, y), line % SIGN_LINES, text);
        } else {
            // multiple horizontal signs; we have some line-splitting to do...
            String ctrlColour = "", ctrlOther = "";
            for (int x = 0; x < width; x++) {
                String ctrl = ctrlColour + ctrlOther;
                int end = Math.min(begin + (SIGN_WIDTH - ctrl.length()), text.length());
                String sub = ctrl + text.substring(begin, end);
                if (sub.endsWith("\u00a7")) {
                    // we can't have a control char split over 2 signs
                    sub = StringUtils.chop(sub);
                }
                ctrlColour = ctrlOther = "";
                for (int i = 0; i < sub.length() - 1; i++) {
                    char c = sub.charAt(i), c1 = Character.toLowerCase(sub.charAt(i + 1));
                    if (c == '\u00a7') {
                        if (c1 == 'r') {
                            ctrlColour = ctrlOther = "";
                        } else if (isHexDigit(c1)) {
                            ctrlColour = "\u00a7" + c1;
                            ctrlOther = ""; // colour code disables any previous formatting code
                        } else {
                            ctrlOther += "\u00a7" + c1;
                        }
                    }
                }
                Location loc = getSignLocation(x, y);
                Debugger.getInstance().debug(3, "drawText: substr = [" + sub + "] @" + x + "," + y + loc + " line=" + line % SIGN_LINES);
                pendingUpdate(loc, line % SIGN_LINES, sub);
                begin += sub.length() - ctrl.length();
            }
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
     * @param x X co-ordinate
     * @param y Y co-ordinate
     * @return the Sign block retrieved
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
     * @param x the X position, increasing rightwards
     * @param y the Y position, increasing downwards
     * @return location of the sign at the given position
     */
    public Location getSignLocation(int x, int y) {
        Location tl = topLeft.getLocation();

        BlockFace toLeft = BlockUtil.getLeft(facing);

        int x1 = tl.getBlockX() + toLeft.getModX() * x;
        int y1 = tl.getBlockY() - y;
        int z1 = tl.getBlockZ() + toLeft.getModZ() * x;

        return new Location(tl.getWorld(), x1, y1, z1);
    }

    /**
     * Mark one line on a given sign as requiring an update.
     *
     * @param loc  location of the sign needing an update
     * @param line line number on the sign, in the range 0 .. 3
     * @param text the text to be updated
     */
    private void pendingUpdate(Location loc, int line, String text) {
        if (!updates.containsKey(loc)) {
            updates.put(loc, new String[SIGN_LINES]);
        }
        updates.get(loc)[line] = text;
    }

    /**
     * Apply all the updates that have been marked as pending.  Doing them all at once means
     * we only need to send world updates for each sign once.
     */
    private void applyUpdates() {
        for (Entry<Location, String[]> e : updates.entrySet()) {
            Block b = e.getKey().getBlock();
            if (b.getType() != Material.WALL_SIGN) {
                continue;
            }
            org.bukkit.block.Sign s = (org.bukkit.block.Sign) b.getState();
            for (int i = 0; i < SIGN_LINES; i++) {
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
                scan(b, BlockFace.EAST);
                break;
            case EAST:
                scan(b, BlockFace.SOUTH);
                break;
            case SOUTH:
                scan(b, BlockFace.WEST);
                break;
            case WEST:
                scan(b, BlockFace.NORTH);
                break;
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
            case NORTH:
            case SOUTH:
                width = Math.abs(tl.getBlockZ() - br.getBlockZ()) + 1;
                break;
            case EAST:
            case WEST:
                width = Math.abs(tl.getBlockX() - br.getBlockX()) + 1;
                break;
            default:
                break;
        }
        Debugger.getInstance().debug(2, "multisign: topleft=" + topLeft + ", bottomright=" + bottomRight);
        Debugger.getInstance().debug(2, "multisign: height=" + height + ", width=" + width);
    }

    private Location scan(Block b, BlockFace horizontal, BlockFace vertical) {
        Debugger.getInstance().debug(2, "scan: " + b + " h=" + horizontal + " v=" + vertical);

        Block b1 = scanOneDir(b, horizontal);
        b1 = scanOneDir(b1, vertical);

        return b1.getLocation();
    }

    private Block scanOneDir(Block b, BlockFace dir) {
        while (b.getType() == Material.WALL_SIGN && ScrollingMenuSign.getInstance().getViewManager().getViewForLocation(b.getLocation()) == null) {
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
        int l = SIGN_WIDTH * width - prefix.length();
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
        return formatLine(prefix, doVariableSubstitutions(null, text), getItemJustification());
    }

    private boolean isHexDigit(char c) {
        return c >= '0' && c <= '9' || c >= 'a' && c <= 'f';
    }

    @Override
    protected int getLineLength() {
        return SIGN_WIDTH * width;
    }

    @Override
    protected int getHardMaxTitleLines() {
        return 4;
    }
}
