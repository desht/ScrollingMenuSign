package me.desht.scrollingmenusign;

import java.util.HashMap;
import java.util.Map;

import me.desht.util.MiscUtil;

import org.bukkit.entity.Player;
import org.bukkit.util.config.ConfigurationNode;

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
	
	SMSMenuItem(SMSMenu menu, ConfigurationNode node) {
		this.menu = menu;
		this.label = MiscUtil.parseColourSpec(node.getString("label"));
		this.command = node.getString("command");
		this.message = MiscUtil.parseColourSpec(null, node.getString("message"));
		this.uses = new SMSRemainingUses(this, node.getNode("usesRemaining"));
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
	 * @deprecated	Use getUseLimits().setGlobalUses() instead
	 */
	@Deprecated
	public void setGlobalUses(int nUses) {
		uses.clearUses();
		uses.setGlobalUses(nUses);
	}
	
	/**
	 * Set the maximum number of uses for this menu, per player.
	 * This clears any global use count for the item.
	 * 
	 * @param nUses	maximum use count
	 * @deprecated	Use getUseLimits().setUses() instead
	 */
	@Deprecated
	public void setUses(int nUses) {
		uses.setUses(nUses);
	}
	
	/**
	 * Get the remaining number of uses of this menu item for the given player
	 * 
	 * @param player	Player to check for
	 * @return			Number of uses remaining
	 * @deprecated	Use getUseLimits().getRemainingUses() instead
	 */
	@Deprecated
	public int getRemainingUses(Player player) {
		return uses.getRemainingUses(player.getName());
	}
	
	/**
	 * Clear (reset) the number of uses for the given player
	 * 
	 * @param player	Player to reset
	 * @deprecated	Use getUseLimits().clearUses() instead
	 */
	@Deprecated
	public void clearUses(Player player) {
		uses.clearUses(player.getName());
		if (menu != null)
			menu.autosave();
	}
	
	/**
	 * Clears all usage limits for this menu item
	 * 
	 * @deprecated	Use getUseLimits().clearUses() instead
	 */
	@Deprecated
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
		checkRemainingUses(this.getUseLimits(), player);
		checkRemainingUses(menu.getUseLimits(), player);
		String cmd = getCommand();
		if ((cmd == null || cmd.isEmpty()) && !menu.getDefaultCommand().isEmpty() ) {
			cmd = menu.getDefaultCommand().replaceAll("<LABEL>", getLabel());
		}
		SMSMacro.executeCommand(cmd, player);
	}

	private void checkRemainingUses(SMSRemainingUses uses, Player player) throws SMSException {
		String name = player.getName();
		if (uses.hasLimitedUses(name)) {
			String what = uses.getOwningObject().toString();
			if (uses.getRemainingUses(name) == 0) {
				throw new SMSException("You can't use that " + what + " anymore.");
			}
			uses.use(name);
			if (menu != null)
				menu.autosave();
			MiscUtil.statusMessage(player, "&6[Uses remaining for this " + what + ": &e" + uses.getRemainingUses(name) + "&6]");
		}
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
	 * Get the remaining use details for this menu item
	 *
	 * @return	The remaining use details
	 */
	public SMSRemainingUses getUseLimits() {
		return uses;
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
	public String formatUses(Player player) {
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
		return MiscUtil.deColourise(label).compareTo(MiscUtil.deColourise(other.label));
	}
	
	Map<String, Object> freeze() {
		Map<String, Object> map = new HashMap<String, Object>();
		
		map.put("label", MiscUtil.unParseColourSpec(label));
		map.put("command", command);
		map.put("message", MiscUtil.unParseColourSpec(message));
		map.put("usesRemaining", uses.freeze());
		
		return map;
	}

	void autosave() {
		if (menu != null)
			menu.autosave();
	}
}
