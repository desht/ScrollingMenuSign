package me.desht.scrollingmenusign.views;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import me.desht.dhutils.ConfigurationManager;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.ScrollingMenuSign;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public abstract class SMSScrollableView extends SMSView {
	public static final String MAX_TITLE_LINES = "max_title_lines";

	private boolean wrap;
	private final Map<String,Integer> playerScrollPos = new HashMap<String, Integer>();
	private final ScrollPosStack storedScrollPos = new ScrollPosStack();

	public SMSScrollableView(SMSMenu menu) {
		this(null, menu);
	}

	public SMSScrollableView(String name, SMSMenu menu) {
		super(name, menu);
		wrap = true;

		registerAttribute(MAX_TITLE_LINES, 0, "Max lines to use for menu title");
	}

	@Override
	public Map<String, Object> freeze() {
		Map<String, Object> map = super.freeze();

		return map;
	}

	@Override
	public void pushMenu(String playerName, SMSMenu newActive) {
		storedScrollPos.pushScrollPos(playerName, getScrollPos(playerName));
		setScrollPos(playerName, 1);
		super.pushMenu(playerName, newActive);
	}

	@Override
	public SMSMenu popMenu(String playerName) {		
		setScrollPos(playerName, storedScrollPos.popScrollPos(playerName));
		return super.popMenu(playerName);
	}

	public boolean isWrap() {
		return wrap;
	}

	public void setWrap(boolean wrap) {
		this.wrap = wrap;
	}

	protected void thaw(ConfigurationSection node) throws SMSException {
		super.thaw(node);
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
		playerName = getPlayerContext(playerName);

		if (!playerScrollPos.containsKey(playerName) || playerScrollPos.get(playerName) < 1) {
			setScrollPos(playerName, 1);
		} else if (playerScrollPos.get(playerName) > getActiveMenuItemCount(playerName))
			setScrollPos(playerName, getActiveMenuItemCount(playerName));

		return playerScrollPos.get(playerName);
	}

	/**
	 * Sets the scroll position for the given player on this view.
	 * 
	 * @param playerName	The player's name
	 * @param scrollPos		The scroll position
	 */
	public void setScrollPos(String playerName, int scrollPos) {
		playerName = getPlayerContext(playerName);
		playerScrollPos.put(playerName, scrollPos);
		setDirty(playerName, true);
	}

	/**
	 * Sets the current selected item for the given player to the next item.
	 * 
	 * @param playerName	The player to scroll the view for
	 */
	public void scrollDown(String playerName) {
		int pos = getScrollPos(playerName) + 1;
		if (pos > getActiveMenuItemCount(playerName)) {
			pos = wrap ? 1 : getActiveMenuItemCount(playerName);
		}
		setScrollPos(playerName, pos);
	}

	/**
	 * Sets the current selected item for the given player to the previous item.
	 * 
	 * @param playerName	The player to scroll the view for
	 */
	public void scrollUp(String playerName) {
		if (getActiveMenuItemCount(playerName) == 0)
			return;

		int pos = getScrollPos(playerName) - 1;
		if (pos <= 0) {
			pos = wrap ? getActiveMenuItemCount(playerName) : 1;
		}
		setScrollPos(playerName, pos);
	}

	/* (non-Javadoc)
	 * @see me.desht.scrollingmenusign.views.SMSView#clearPlayerForView(org.bukkit.entity.Player)
	 */
	@Override
	public void clearPlayerForView(Player player) {
		super.clearPlayerForView(player);
		playerScrollPos.remove(player.getName());
	}

	/**
	 * Get the suggested line length in characters.  Default is 0 - subclasses should override this
	 * as appropriate.  Line length of 0 will disable any splitting.
	 * 
	 * @return
	 */
	protected int getLineLength() {
		return 0;
	}


	/**
	 * Get the desired maximum number of title lines for this view.
	 * @return
	 */
	protected int getMaxTitleLines() {
		int max = (Integer) getAttribute(MAX_TITLE_LINES);
		return max > 0 ? max :  ScrollingMenuSign.getInstance().getConfig().getInt("sms.max_title_lines", 1);
	}

	/**
	 * Get the hard maximum on the number of title lines this view supports.  Override in subclasses.
	 * @return
	 */
	protected int getHardMaxTitleLines() {
		return 1;
	}

	/**
	 * Split the menu's title in the view's maximum line count, based on the view's suggested line length.
	 * 
	 * @return
	 */
	public List<String> splitTitle(String playerName) {
		String title = variableSubs(getActiveMenuTitle(playerName));
		int lineLength = getLineLength();
		List<String> result = new ArrayList<String>();
		int maxLines = Math.min(getMaxTitleLines(), getHardMaxTitleLines());

		if (lineLength == 0 || maxLines == 1) {
			result.add(title);
			return result;
		}

		Scanner s = new Scanner(title);
		StringBuilder sb = new StringBuilder(title.length());
		MarkupTracker markup = new MarkupTracker();
		while (s.hasNext()) {
			String word = s.next();
			markup.update(MarkupTracker.findMarkup(word));
			//			LogUtils.finer(getName() + ": buflen = " + sb.length() + " wordlen = " + word.length() + " line length = " + lineLength);
			if (sb.length() + word.length() + 1 <= lineLength || result.size() >= maxLines - 1) {
				// continue appending
				if (sb.length() > 0) sb.append(" ");
				sb.append(word);
			} else {
				// start a new line
				result.add(sb.toString()); 	
				sb = new StringBuilder(markup + word);
				lineLength = getLineLength() - markup.toString().length();
			}
		}
		result.add(sb.toString());

		return result;
	}

	/* (non-Javadoc)
	 * @see me.desht.scrollingmenusign.views.SMSView#onConfigurationChanged(me.desht.dhutils.ConfigurationManager, java.lang.String, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void onConfigurationChanged(ConfigurationManager configurationManager, String key, Object oldVal, Object newVal) {
		super.onConfigurationChanged(configurationManager, key, oldVal, newVal);

		if (key.equals(MAX_TITLE_LINES)) {
			setDirty(true);
		}
	}

	@Override
	public void onConfigurationValidate(ConfigurationManager configurationManager, String key, Object oldVal, Object newVal) {
		super.onConfigurationValidate(configurationManager, key, oldVal, newVal);

		if (key.equals(MAX_TITLE_LINES)) {
			if ((Integer)newVal > getHardMaxTitleLines() || (Integer)newVal < 0) {
				throw new SMSException("Valid " + MAX_TITLE_LINES + " range for this view is 0-" + getHardMaxTitleLines() + ".");
			}
		}
	}

	private static class MarkupTracker {
		// null indicates value never set, 0 indicates a reset (&R)
		private Character colour = null;
		private Character text = null;

		public void update(MarkupTracker other) {
			if (other.colour != null) this.colour = other.colour;
			if (other.text != null) this.text = other.text;
		}

		@Override
		public String toString() {
			String s = "";
			if (colour != null && colour != 0) s += "\u00a7" + colour;
			if (text != null && text != 0) s += "\u00a7" + text;
			return s;
		}

		public static MarkupTracker findMarkup(String s) {
			MarkupTracker m = new MarkupTracker();
			for (int i = 0; i < s.length() - 1; i++	) {
				if (s.charAt(i) == 0x00a7) {
					char c = Character.toUpperCase(s.charAt(i + 1));
					if (c >= '0' && c <= '9' || c >= 'A' && c <= 'F') {
						m.colour = c;
					} else if (c == 'R') {
						m.text = m.colour = 0;
					} else {
						m.text = c;
					}
				}
			}
			return m;
		}
	}

	private class ScrollPosStack {
		private Map<String,Deque<Integer>> stacks = new HashMap<String, Deque<Integer>>();

		private void verify(String playerName) {
			if (!stacks.containsKey(playerName)) {
				stacks.put(playerName, new ArrayDeque<Integer>());
			}
		}
		public void pushScrollPos(String playerName, int pos) {
			verify(playerName);
			stacks.get(playerName).push(pos);
		}

		public int popScrollPos(String playerName) {
			verify(playerName);
			Deque<Integer> stack = stacks.get(playerName);
			return stack.isEmpty() ? 1 : stack.pop();
		}
	}
}
