package me.desht.scrollingmenusign.views;

import java.util.Observable;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.scrollingmenusign.enums.ViewJustification;
import me.desht.dhutils.MiscUtil;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;

/**
 * @author des
 *
 * This view draws menus on signs.
 */
public class SMSSignView extends SMSGlobalScrollableView {
	
	/**
	 * Create a new sign view object.
	 * 
	 * @param name	Unique name for this view.
	 * @param menu	The SMSMenu object to attach this view to.
	 * @param loc	The location of this view's sign
	 * @throws SMSException	if the given location is not suitable for this view
	 */
	public SMSSignView(String name, SMSMenu menu, Location loc) throws SMSException {
		super(name, menu);
		addLocation(loc);
	}

	/**
	 * Create a new sign view object with no registered location.  A location
	 * which contains a sign must be added with @see #addLocation(Location) before
	 * this view is useful.
	 * 
	 * @param name	Unique name for this view.
	 * @param menu	The SMSMenu object to attach this view to.
	 */
	public SMSSignView(String name, SMSMenu menu) {
		super(name, menu);
	}
	
	/**
	 * Create a new sign view object.  Equivalent to calling SMSSignView(null, menu, loc).  The
	 * view's name will be automatically generated, based on the menu name.
	 * 
	 * @param menu	The SMSMenu object to attach this view to.
	 * @param loc	The location of this view's sign
	 * @throws SMSException	if the given location is not suitable for this view
	 */
	public SMSSignView(SMSMenu menu, Location loc) throws SMSException {
		this(null, menu, loc);
	}

	/* (non-Javadoc)
	 * @see me.desht.scrollingmenusign.views.SMSView#addLocation(org.bukkit.Location)
	 */
	@Override
	public void addLocation(Location loc) throws SMSException {
		Block b = loc.getBlock();
		if (b.getType() != Material.SIGN_POST && b.getType() != Material.WALL_SIGN) {
			throw new SMSException("Location " + MiscUtil.formatLocation(loc) + " does not contain a sign.");
		}
		
		super.addLocation(loc);
	}

	/* (non-Javadoc)
	 * @see me.desht.scrollingmenusign.views.SMSScrollableView#update(java.util.Observable, java.lang.Object)
	 */
	@Override
	public void update(Observable menu, Object arg1) {
		Sign sign = getSign();
		if (sign == null)
			return;
		if (!(menu instanceof SMSMenu))
			return;

		String[] lines = buildSignText(getLastScrollPos());
		for (int i = 0; i < lines.length; i++) {
			sign.setLine(i, lines[i]);
		}
		
		sign.update();
		setDirty(false);
	}

	private String[] buildSignText(int scrollPos) {
		String[] res = new String[4];

		// first line of the sign is the menu title
		res[0] = String.format(makePrefix("", getTitleJustification()), getMenu().getTitle());
		
		// line 2-4 are the menu items around the current menu position
		// line 3 is the current position
		String prefix1 = ScrollingMenuSign.getInstance().getConfig().getString("sms.item_prefix.not_selected", "  ").replace("%", "%%"); 
		String prefix2 = ScrollingMenuSign.getInstance().getConfig().getString("sms.item_prefix.selected", "> ").replace("%", "%%");
		
		ViewJustification ij = getItemJustification();
		res[1] = String.format(makePrefix(prefix1, ij), getLine2Item(scrollPos));
		res[2] = String.format(makePrefix(prefix2, ij), getLine3Item(scrollPos));
		res[3] = String.format(makePrefix(prefix1, ij), getLine4Item(scrollPos));
		
		return res;
	}

	private String getLine2Item(int pos) {
		if (getMenu().getItemCount() < 3)
			return "";
			
		int prevPos = pos - 1;
		if (prevPos < 1) {
			prevPos = getMenu().getItemCount();
		}
		return getMenu().getItemAt(prevPos).getLabel();
	}
	
	private String getLine3Item(int pos) {
		if (getMenu().getItemCount() < 1) {
			return "";
		}
		return getMenu().getItemAt(pos).getLabel();
	}

	private String getLine4Item(int pos) {
		if (getMenu().getItemCount() < 2) 
			return "";
			
		int nextPos = pos + 1;
		if (nextPos > getMenu().getItemCount()) {
			nextPos = 1;
		}
		return getMenu().getItemAt(nextPos).getLabel();
	}
	
	private String makePrefix(String prefix, ViewJustification just) {
		int l = 15 - prefix.length();
		String s = "";
		switch (just) {
		case LEFT:
			s =  prefix + "%1$-" + l + "s"; break;
		case CENTER:
			s = prefix + "%1$s"; break;
		case RIGHT:
			s = prefix + "%1$" + l + "s"; break;
		}
		return MiscUtil.parseColourSpec(s);
	}
	
	/**
	 * Get the actual Bukkit Sign object for this view.
	 * 
	 * @return	The Sign object
	 */
	private Sign getSign() {
		if (getLocations().isEmpty())
			return null;
		
		Location loc = getLocationsArray()[0];
		Block b = loc.getBlock();
		if (b.getType() != Material.SIGN_POST && b.getType() != Material.WALL_SIGN) {
			return null;
		}
		return (Sign) b.getState();
	}

	/**
	 * Erase the text for this view's sign, leaving it blank.
	 */
	public void blankSign() {
		Sign sign = getSign();
		if (sign == null)
			return;
		for (int i = 0; i < 4; i++) {
			sign.setLine(i, "");
		}
		sign.update();		
	}

	/**
	 * Destroy the sign for this view, replacing it with air.
	 */
	public void destroySign() {
		Location loc = getLocationsArray()[0];
		loc.getBlock().setTypeId(0);
	}

	/**
	 * Convenience routine.  Create and register a new SMSSignView object, and attach it to
	 * the given menu.  A sign must already exist at the given location, and it must not be
	 * an already-existing view.
	 * 
	 * @param menu	The menu to attach the new view to
	 * @param loc	Location of the new view
	 * @return		The newly-created view
	 * @throws SMSException	if the given location is not a suitable location for a new view
	 */
	public static SMSView addSignToMenu(SMSMenu menu, Location loc) throws SMSException {
		SMSView view = new SMSSignView(menu, loc);
		view.register();
		view.update(menu, SMSMenuAction.REPAINT);
		return view;
	}
	
	/* (non-Javadoc)
	 * @see me.desht.scrollingmenusign.views.SMSView#deletePermanent()
	 */
	@Override
	public void deletePermanent() {
		blankSign();
		super.deletePermanent();
	}
	
	public String toString() {
		Location[] locs = getLocationsArray();
		return "sign @ " + (locs.length == 0 ? "NONE" : MiscUtil.formatLocation(getLocationsArray()[0]));
	}

	@Override
	public String getType() {
		return "sign";
	}
}
