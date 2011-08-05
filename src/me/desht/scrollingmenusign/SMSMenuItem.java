package me.desht.scrollingmenusign;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Player;

public class SMSMenuItem implements Comparable<SMSMenuItem> {
	private final String label;
	private final String command;
	private final String message;
	private final SMSRemainingUses uses;
	private final SMSMenu menu;
	
	SMSMenuItem(SMSMenu menu, String l, String c, String m) {
		if (l == null || c == null || m == null)
			throw new NullPointerException();
		this.menu = menu;
		this.label = l;
		this.command = c;
		this.message = m;
		this.uses = new SMSRemainingUses(this);
	}
	
	@SuppressWarnings("unchecked")
	SMSMenuItem(SMSMenu menu, Map<String, Object> map) {
		this.menu = menu;
		this.label = SMSUtils.parseColourSpec(null, (String) map.get("label"));
		this.command = (String) map.get("command");
		this.message = SMSUtils.parseColourSpec(null, (String) map.get("message"));
		this.uses = map.containsKey("usesRemaining") ?
				new SMSRemainingUses(this, (Map<String, Integer>) map.get("usesRemaining")) :
					new SMSRemainingUses(this);
	}
	
	/**
	 * Get the label for this menu item
	 * 
	 * @return	The label
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Get the command for this menu item
	 * 
	 * @return	The command
	 */
	public String getCommand() {
		return command;
	}

	/**
	 * Get the feedback message for this menu item
	 * 
	 * @return	The feedback message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Set the maximum number of uses for this menu, globally (i.e. for all users).
	 * This clears any per-player use counts for the item.
	 * 
	 * @param nUses	maximum use count
	 */
	public void setGlobalUses(int nUses) {
		uses.clearUses();
		uses.setGlobalUses(nUses);
	}
	
	/**
	 * Set the maximum number of uses for this menu, per player.
	 * This clears any global use count for the item.
	 * 
	 * @param nUses	maximum use count
	 */
	public void setUses(int nUses) {
		uses.setUses(nUses);
	}
	
	/**
	 * Get the remaining number of uses of this menu item for the given player
	 * 
	 * @param player	Player to check for
	 * @return			Number of uses remaining
	 */
	public int getRemainingUses(Player player) {
		return uses.getRemainingUses(player.getName());
	}
	
	/**
	 * Clear (reset) the number of uses for the given player
	 * 
	 * @param player	Player to reset
	 */
	public void clearUses(Player player) {
		uses.clearUses(player.getName());
		if (menu != null)
			menu.autosave();
	}
	
	/**
	 * Clears all usage limits for this menu item
	 */
	public void clearUses() {
		uses.clearUses();
		if (menu != null)
			menu.autosave();
	}
	
	/**
	 * Executes the command for this item
	 * 
	 * @param player		Player to execute the command for
	 * @throws SMSException	if the usage limit for this player is exhausted
	 */
	public void execute(Player player) throws SMSException {
		String name = player.getName();
		if (uses.hasLimitedUses(name)) {
			if (uses.getRemainingUses(name) == 0) {
				throw new SMSException("You can't use that menu item anymore.");
			}
			uses.use(name);
			if (menu != null)
				menu.autosave();
			SMSUtils.statusMessage(player, "&6[Uses remaining for this menu item: &e" + uses.getRemainingUses(name) + "&6]");
		}
		SMSMacro.executeCommand(getCommand(), player);
	}
	
	/**
	 * Displays the feedback message for this menu item
	 * 
	 * @param player	Player to show the message to
	 */
	public void feedbackMessage(Player player) {
		SMSMacro.sendFeedback(player, getMessage());
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "SMSMenuItem [label=" + label + ", command=" + command + ", message=" + message + "]";
	}
	
	/**
	 * Returns a printable representation of the number of uses remaining for this item.
	 * 
	 * @return	Formatted usage information
	 */
	String formatUses() {
		return uses.toString();
	}
	
	/**
	 * Returns a printable representation of the number of uses remaining for this item, for the given player.
	 * 
	 * @param player	Player to retrieve the usage information for
	 * @return			Formatted usage information
	 */
	String formatUses(Player player) {
		if (player == null) {
			return formatUses();
		} else {
			return uses.toString(player.getName());
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((command == null) ? 0 : command.hashCode());
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result + ((message == null) ? 0 : message.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SMSMenuItem other = (SMSMenuItem) obj;
		if (command == null) {
			if (other.command != null)
				return false;
		} else if (!command.equals(other.command))
			return false;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		if (message == null) {
			if (other.message != null)
				return false;
		} else if (!message.equals(other.message))
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(SMSMenuItem other) {
		return SMSUtils.deColourise(label).compareTo(SMSUtils.deColourise(other.label));
	}
	
	Map<String, Object> freeze() {
		Map<String, Object> map = new HashMap<String, Object>();
		
		map.put("label", SMSUtils.unParseColourSpec(label));
		map.put("command", command);
		map.put("message", SMSUtils.unParseColourSpec(message));
		map.put("usesRemaining", uses.freeze());
		
		return map;
	}

	void autosave() {
		if (menu != null)
			menu.autosave();
	}
}
