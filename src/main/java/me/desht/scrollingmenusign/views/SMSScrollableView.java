package me.desht.scrollingmenusign.views;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public abstract class SMSScrollableView extends SMSView {
	private boolean perPlayerScrolling;
	private boolean wrap;
	private int lastScrollPos;
	private final Map<String,Integer> playerScrollPos = new HashMap<String, Integer>();

	public SMSScrollableView(SMSMenu menu) {
		this(null, menu);
	}
	
	public SMSScrollableView(String name, SMSMenu menu) {
		super(name, menu);
		lastScrollPos = 1;
		wrap = true;
		perPlayerScrolling = true;
	}

	@Override
	public Map<String, Object> freeze() {
		Map<String, Object> map = super.freeze();
		
		return map;
	}
	
	public boolean isWrap() {
		return wrap;
	}

	public void setWrap(boolean wrap) {
		this.wrap = wrap;
	}
	
	public boolean isPerPlayerScrolling() {
		return perPlayerScrolling;
	}

	protected void setPerPlayerScrolling(boolean perPlayerScrolling) {
		this.perPlayerScrolling = perPlayerScrolling;
	}

	protected void thaw(ConfigurationSection node) throws SMSException {
		super.thaw(node);
		lastScrollPos = node.getInt("scrollPos", 1);
		if (lastScrollPos < 1 || lastScrollPos > getMenu().getItemCount())
			lastScrollPos = 1;
	}

	/**
	 * Get the last scroll position (currently-selected item) for this view.  If the scroll position
	 * is out of range (possibly because an item was deleted from the menu), it will be automatically
	 * adjusted to be in range before being returned.
	 * 
	 * @return	The scroll position
	 */
	public int getLastScrollPos() {
		if (lastScrollPos < 1)
			lastScrollPos = 1;
		else if (lastScrollPos > getMenu().getItemCount())
			lastScrollPos = getMenu().getItemCount();
		
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
		if (perPlayerScrolling) {
			if (!playerScrollPos.containsKey(playerName) || playerScrollPos.get(playerName) < 1) {
				setScrollPos(playerName, 1);
			} else if (playerScrollPos.get(playerName) > getMenu().getItemCount())
				setScrollPos(playerName, getMenu().getItemCount());

			return playerScrollPos.get(playerName);
		} else {
			return getLastScrollPos();
		}
	}

	/**
	 * Sets the scroll position for the given player on this view.
	 * 
	 * @param playerName	The player's name
	 * @param scrollPos		The scroll position
	 */
	public void setScrollPos(String playerName, int scrollPos) {
		if (perPlayerScrolling) {
			playerScrollPos.put(playerName, scrollPos);
			lastScrollPos = scrollPos;
			setDirty(playerName, true);
		} else {
			lastScrollPos = scrollPos;
			setDirty(true);
		}
	}
	
	/**
	 * Sets the current selected item for the given player to the previous item.
	 * 
	 * @param playerName	The player to scroll the view for
	 */
	public void scrollDown(String playerName) {
		int pos = getScrollPos(playerName) + 1;
		if (wrap && pos > getMenu().getItemCount())
			pos = 1;
		setScrollPos(playerName, pos);
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
		if (wrap && pos <= 0)
			pos = getMenu().getItemCount();
		setScrollPos(playerName, pos);
	}
	
	@Override
	public abstract void update(Observable menu, Object arg1);
	
	/* (non-Javadoc)
	 * @see me.desht.scrollingmenusign.views.SMSView#clearPlayerForView(org.bukkit.entity.Player)
	 */
	@Override
	public void clearPlayerForView(Player player) {
		super.clearPlayerForView(player);
		playerScrollPos.remove(player.getName());
	}
}
