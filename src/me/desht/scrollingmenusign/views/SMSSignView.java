package me.desht.scrollingmenusign.views;

import java.util.Observable;

import me.desht.scrollingmenusign.SMSConfig;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.util.MiscUtil;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;

/**
 * @author des
 *
 */
public class SMSSignView extends SMSScrollableView {

	public SMSSignView(String name, SMSMenu menu) {
		super(name, menu);
	}
	
	/**
	 * Create a new sign view object.  Equivalent to calling SMSSignView(null, menu, loc)
	 * 
	 * @param menu	The SMSMenu object to attach this view to.
	 * @param loc	The location of this view's sign
	 * @throws SMSException	if the given location is not suitable for this view
	 */
	public SMSSignView(SMSMenu menu, Location loc) throws SMSException {
		this(null, menu, loc);
	}
	
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

	/* (non-Javadoc)
	 * @see me.desht.scrollingmenusign.views.SMSView#addLocation(org.bukkit.Location)
	 */
	@Override
	public void addLocation(Location loc) throws SMSException {
		// a sign view has only one location: the sign
		if (getLocations().size() > 0)
			return;

		Block b = loc.getBlock();
		if (b.getType() != Material.SIGN_POST && b.getType() != Material.WALL_SIGN) {
			throw new SMSException("Location " + MiscUtil.formatLocation(loc) + " does not contain a sign.");
		}

		SMSView v = SMSView.getViewForLocation(loc);
		if (v != null) {
			throw new SMSException("Location " + MiscUtil.formatLocation(loc) + " already has a menu: " + v.getMenu().getName());
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

		int pos = getScrollPos();
		if (pos < 1 || pos > getMenu().getItemCount())
			setScrollPos(1);
		String[] lines = buildSignText(getScrollPos());
		for (int i = 0; i < lines.length; i++) {
			sign.setLine(i, lines[i]);
		}
		
		sign.update();
		setDirty(false);
	}


	private String[] buildSignText(int scrollPos) {
		String[] res = new String[4];

		// first line of the sign in the menu title
		res[0] = getMenu().getTitle();
		
		// line 2-4 are the menu items around the current menu position
		// line 3 is the current position
		String prefix1 = SMSConfig.getConfiguration().getString("sms.item_prefix.not_selected", "  ");
		String prefix2 = SMSConfig.getConfiguration().getString("sms.item_prefix.selected", "> ");
		
		res[1] = String.format(makePrefix(prefix1), getLine2Item(scrollPos));
		res[2] = String.format(makePrefix(prefix2), getLine3Item(scrollPos));
		res[3] = String.format(makePrefix(prefix1), getLine4Item(scrollPos));
		
		return res;
	}

	private String getLine2Item(int pos) {
		if (getMenu().getItemCount() < 3)
			return "";
			
		int prevPos = pos - 1;
		if (prevPos < 1) {
			prevPos = getMenu().getItemCount();
		}
		return getMenu().getItem(prevPos).getLabel();
	}
	
	private String getLine3Item(int pos) {
		if (getMenu().getItemCount() < 1) {
			return "";
		}
		return getMenu().getItem(pos).getLabel();
	}

	private String getLine4Item(int pos) {
		if (getMenu().getItemCount() < 2) 
			return "";
			
		int nextPos = pos + 1;
		if (nextPos > getMenu().getItemCount()) {
			nextPos = 1;
		}
		return getMenu().getItem(nextPos).getLabel();
	}
	
	private String makePrefix(String prefix) {
		String just = SMSConfig.getConfiguration().getString("sms.item_justify", "left");
		int l = 15 - prefix.length();
		String s = "";
		if (just.equals("left"))
			s =  prefix + "%1$-" + l + "s";
		else if (just.equals("right"))
			s = prefix + "%1$" + l + "s";
		else
			s = prefix + "%1$s";
		return MiscUtil.parseColourSpec(null, s);
	}
	
	/**
	 * Get the actual Bukkit Sign object for this view.
	 * 
	 * @return	The Sign object
	 */
	private Sign getSign() {
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
		view.update(menu, SMSMenuAction.REPAINT);
		return view;
	}
	
	@Override
	public void deletePermanent() {
		blankSign();
		super.deletePermanent();
	}
	
	public String toString() {
		return "sign @ " + MiscUtil.formatLocation(getLocationsArray()[0]);
	}
}
