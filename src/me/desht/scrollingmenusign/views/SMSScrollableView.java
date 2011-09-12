package me.desht.scrollingmenusign.views;

import java.util.Map;
import java.util.Observable;

import me.desht.scrollingmenusign.SMSMenu;

import org.bukkit.util.config.ConfigurationNode;

public abstract class SMSScrollableView extends SMSView {

	private int scrollPos;

	public SMSScrollableView(SMSMenu menu) {
		this(null, menu);
	}
	
	public SMSScrollableView(String name, SMSMenu menu) {
		super(name, menu);
		scrollPos = 1;
	}

	@Override
	public Map<String, Object> freeze() {
		Map<String, Object> map = super.freeze();

		map.put("scrollPos", scrollPos);
		
		return map;
	}
	
	protected void thaw(ConfigurationNode node) {
		scrollPos = node.getInt("scrollPos", 1);
		if (scrollPos < 1 || scrollPos > getMenu().getItemCount())
			scrollPos = 1;
	}

	/**
	 * Get the scroll position (currently-selected item) for this view
	 * 
	 * @return	The scroll position
	 */
	public int getScrollPos() {
		return scrollPos;
	}

	/**
	 * Set the scroll position (currently-selected item) for this view
	 * 
	 * @param scrollPos	The scroll position
	 */
	public void setScrollPos(int scrollPos) {
		this.scrollPos = scrollPos;
		setDirty(true);
	}

	/**
	 * Set the currently selected item for this sign to the next item.
	 * 
	 * @param l	Location of the sign
	 */
	public void scrollDown() {
		scrollPos++;
		if (scrollPos > getMenu().getItemCount())
			scrollPos = 1;
		setDirty(true);
	}

	/**
	 * Set the currently selected item for this sign to the previous item.
	 * 
	 * @param l	Location of the sign
	 */
	public void scrollUp() {
		if (getMenu().getItemCount() == 0)
			return;
		
		scrollPos--;
		if (scrollPos <= 0)
			scrollPos = getMenu().getItemCount();
		setDirty(true);
	}
	
	@Override
	public abstract void update(Observable menu, Object arg1);
	
}
