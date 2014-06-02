package me.desht.scrollingmenusign.views;

import me.desht.dhutils.ConfigurationManager;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Represents the abstract base class for all scrollable views.  Provides per-player scroll positioning.
 */
public abstract class SMSScrollableView extends SMSView {
    public enum ScrollType {
        DEFAULT, SCROLL, PAGE
    }

    public static final String MAX_TITLE_LINES = "max_title_lines";
    public static final String SCROLL_TYPE = "scrolltype";

    private static ScrollType defaultScrollType;

    private boolean wrap;
    private final Map<UUID, Integer> playerScrollPos = new HashMap<UUID, Integer>();
    private final ScrollPosStack storedScrollPos = new ScrollPosStack();

    public SMSScrollableView(SMSMenu menu) {
        this(null, menu);
    }

    public SMSScrollableView(String name, SMSMenu menu) {
        super(name, menu);
        wrap = true;

        registerAttribute(MAX_TITLE_LINES, 0, "Max lines to use for menu title");
        registerAttribute(SCROLL_TYPE, ScrollType.DEFAULT, "View scrolling method (scroll or page)");
    }

    @Override
    public void pushMenu(Player player, SMSMenu newActive) {
        storedScrollPos.pushScrollPos(player, getScrollPos(player));
        setScrollPos(player, 1);
        super.pushMenu(player, newActive);
    }

    @Override
    public SMSMenu popMenu(Player player) {
        setScrollPos(player, storedScrollPos.popScrollPos(player));
        return super.popMenu(player);
    }

    public static void setDefaultScrollType(ScrollType scrollType) {
        defaultScrollType = scrollType;
    }

    public static ScrollType getDefaultScrollType() {
        return defaultScrollType;
    }

    public boolean isWrap() {
        return wrap;
    }

    public void setWrap(boolean wrap) {
        this.wrap = wrap;
    }

    /**
     * Get the scroll type for this view.
     *
     * @return the view's scroll type
     */
    public ScrollType getScrollType() {
        ScrollType t = (ScrollType) getAttribute(SCROLL_TYPE);
        return t == ScrollType.DEFAULT ? getDefaultScrollType() : t;
    }

    /**
     * Get the given player's scroll position (currently-selected item) for this view.  If the scroll position
     * is out of range (possibly because an item was deleted from the menu), it will be automatically
     * adjusted to be in range before being returned.
     *
     * @param player The player to check
     * @return The scroll position
     */
    public int getScrollPos(Player player) {
        UUID key = getPlayerContext(player);
        Integer pos = playerScrollPos.get(getPlayerContext(player));
        if (pos == null || pos < 1) {
            setScrollPos(player, 1);
        } else if (pos > getActiveMenuItemCount(player)) {
            setScrollPos(player, getActiveMenuItemCount(player));
        }
        return playerScrollPos.get(key);
    }

    /**
     * Sets the scroll position for the given player on this view.
     *
     * @param player    The player's name
     * @param scrollPos The new scroll position
     */
    public void setScrollPos(Player player, int scrollPos) {
        playerScrollPos.put(getPlayerContext(player), scrollPos);
        setDirty(player, true);
    }

    /**
     * Sets the currently-selected item for the given player to the next item.
     *
     * @param player The player to scroll the view for
     */
    public void scrollDown(Player player) {
        int pos = getScrollPos(player) + 1;
        if (pos > getActiveMenuItemCount(player)) {
            pos = wrap ? 1 : getActiveMenuItemCount(player);
        }
        setScrollPos(player, pos);
    }

    /**
     * Sets the current selected item for the given player to the previous item.
     *
     * @param player The player to scroll the view for
     */
    public void scrollUp(Player player) {
        if (getActiveMenuItemCount(player) == 0)
            return;

        int pos = getScrollPos(player) - 1;
        if (pos <= 0) {
            pos = wrap ? getActiveMenuItemCount(player) : 1;
        }
        setScrollPos(player, pos);
    }

    /* (non-Javadoc)
     * @see me.desht.scrollingmenusign.views.SMSView#clearPlayerForView(org.bukkit.entity.Player)
     */
    @Override
    public void clearPlayerForView(Player player) {
        super.clearPlayerForView(player);
        playerScrollPos.remove(getPlayerContext(player));
    }

    /**
     * Get the suggested line length in characters.  Default is 0 - subclasses should override this
     * as appropriate.  Line length of 0 will disable any splitting.
     *
     * @return the suggested line length
     */
    protected int getLineLength() {
        return 0;
    }


    /**
     * Get the desired maximum number of title lines for this view.
     *
     * @return the maximum number of title lines to draw
     */
    protected int getMaxTitleLines() {
        int max = (Integer) getAttribute(MAX_TITLE_LINES);
        return max > 0 ? max : ScrollingMenuSign.getInstance().getConfig().getInt("sms.max_title_lines", 1);
    }

    /**
     * Get the hard maximum on the number of title lines this view supports.  Override in subclasses.
     *
     * @return the hard limit for the maximum number of title lines
     */
    protected int getHardMaxTitleLines() {
        return 1;
    }

    /**
     * Split the menu's title in the view's maximum line count, based on the view's suggested line length.
     *
     * @return a String list containing the split title
     */
    public List<String> splitTitle(Player player) {
        String title = doVariableSubstitutions(player, getActiveMenuTitle(player));
        int lineLength = getLineLength();
        List<String> result = new ArrayList<String>();
        int maxLines = Math.min(getMaxTitleLines(), getHardMaxTitleLines());

        if (lineLength == 0 || maxLines == 1) {
            result.add(title);
            return result;
        }

        Scanner scanner = new Scanner(title);
        StringBuilder sb = new StringBuilder(title.length());
        MarkupTracker markup = new MarkupTracker();
        while (scanner.hasNext()) {
            String word = scanner.next();
            markup.update(MarkupTracker.findMarkup(word));
            //			Debugger.getInstance().debug(2, getName() + ": buflen = " + sb.length() + " wordlen = " + word.length() + " line length = " + lineLength);
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
        scanner.close();
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
    public Object onConfigurationValidate(ConfigurationManager configurationManager, String key, Object oldVal, Object newVal) {
        newVal = super.onConfigurationValidate(configurationManager, key, oldVal, newVal);

        if (key.equals(MAX_TITLE_LINES)) {
            if ((Integer) newVal > getHardMaxTitleLines() || (Integer) newVal < 0) {
                throw new SMSException("Valid " + MAX_TITLE_LINES + " range for this view is 0-" + getHardMaxTitleLines() + ".");
            }
        }

        return newVal;
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
            for (int i = 0; i < s.length() - 1; i++) {
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
        private final Map<UUID, Deque<Integer>> stacks = new HashMap<UUID, Deque<Integer>>();

        private void verify(Player player) {
            if (!stacks.containsKey(player.getUniqueId())) {
                stacks.put(player.getUniqueId(), new ArrayDeque<Integer>());
            }
        }

        public void pushScrollPos(Player player, int pos) {
            verify(player);
            stacks.get(player.getUniqueId()).push(pos);
        }

        public int popScrollPos(Player player) {
            verify(player);
            Deque<Integer> stack = stacks.get(player.getUniqueId());
            return stack.isEmpty() ? 1 : stack.pop();
        }
    }
}
