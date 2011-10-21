package me.desht.scrollingmenusign.views;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;

import me.desht.scrollingmenusign.SMSMenu;

import org.bukkit.configuration.ConfigurationSection;

public abstract class SMSScrollableView extends SMSView {

	private int lastScrollPos;
	private final Map<String,Integer> playerScrollPos = new HashMap<String, Integer>();

	public SMSScrollableView(SMSMenu menu) {
		this(null, menu);
	}
	
	public SMSScrollableView(String name, SMSMenu menu) {
		super(name, menu);
		lastScrollPos = 1;
	}

	@Override
	public Map<String, Object> freeze() {
		Map<String, Object> map = super.freeze();
		
		return map;
	}
	
	protected void thaw(ConfigurationSection node) {
		lastScrollPos = node.getInt("scrollPos", 1);
		if (lastScrollPos < 1 || lastScrollPos > getMenu().getItemCount())
			lastScrollPos = 1;
	}

	/**
	 * Get the default scroll position (currently-selected item) for this view.  If the scroll position
	 * is out of range (possibly because an item was deleted from the menu), it will be automatically
	 * adjusted to be in range before being returned.
	 * 
	 * @return	The scroll position
	 * @deprecated Use getScrollPos(String playerName)
	 */
	@Deprecated
	public int getScrollPos() {
		if (lastScrollPos < 1)
			setScrollPos(1);
		else if (lastScrollPos > getMenu().getItemCount())
			setScrollPos(getMenu().getItemCount());
		
		return lastScrollPos;
	}
	
	/**
	 * Get the given player's scroll position (currently-selected item) for this view.  If the scroll position
	 * is out of range (possibly because an item was deleted from the menu), it will be automatically
	 * adjusted to be in range before being returned.
	 * 
	 * @param playerName	The player to check
	 * @return				The scroll position
	 */
	public int getScrollPos(String playerName) {
		if (!playerScrollPos.containsKey(playerName) || playerScrollPos.get(playerName) < 1) {
			setScrollPos(playerName, 1);
		} else if (playerScrollPos.get(playerName) > getMenu().getItemCount())
			setScrollPos(playerName, getMenu().getItemCount());
		
		return playerScrollPos.get(playerName);
	}

	/**
	 * Set the default scroll position (currently-selected item) for this view.
	 * 
	 * @param scrollPos	The scroll position
	 * @deprecated Use setScrollPos(String playerName, int scrollPos)
	 */
	@Deprecated
	public void setScrollPos(int scrollPos) {
		this.lastScrollPos = scrollPos;
		setDirty(true);
	}

	/**
	 * Sets the scroll position for the given player on this view.
	 * 
	 * @param playerName	The player's name
	 * @param scrollPos		The scroll position
	 */
	public void setScrollPos(String playerName, int scrollPos) {
		playerScrollPos.put(playerName, scrollPos);
		lastScrollPos = scrollPos;
		setDirty(true);
	}

	/**
	 * Set the currently selected item for this view to the next item.
	 * 
	 * @deprecated Use scrollDown(String playerName)
	 */
	@Deprecated
	public void scrollDown() {
		lastScrollPos++;
		if (lastScrollPos > getMenu().getItemCount())
			lastScrollPos = 1;
		setDirty(true);
	}
	
	/**
	 * Sets the current selected item for the given player to the previous item.
	 * 
	 * @param playerName	The player to scroll the view for
	 */
	public void scrollDown(String playerName) {
		int pos = getScrollPos(playerName) + 1;
		if (pos > getMenu().getItemCount())
			pos = 1;
		setScrollPos(playerName, pos);
	}

	/**
	 * Set the currently selected item for this view to the previous item.
	 * 
	 * @deprecated Use scrollUp(String playerName)
	 */
	@Deprecated
	public void scrollUp() {
		if (getMenu().getItemCount() == 0)
			return;
		
		lastScrollPos--;
		if (lastScrollPos <= 0)
			lastScrollPos = getMenu().getItemCount();
		setDirty(true);
	}
	
	/**
	 * Sets the current selected item for the given player to the previous item.
	 * 
	 * @param playerName	The player to scroll the view for
	 */
	public void scrollUp(String playerName) {
		if (getMenu().getItemCount() == 0)
			return;
		
		int pos = getScrollPos(playerName) - 1;
		if (pos <= 0)
			pos = getMenu().getItemCount();
		setScrollPos(playerName, pos);
	}
	
	@Override
	public abstract void update(Observable menu, Object arg1);
	
}
