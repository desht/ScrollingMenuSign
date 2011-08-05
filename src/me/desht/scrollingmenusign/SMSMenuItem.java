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
	
	public String getLabel() {
		return label;
	}

	public String getCommand() {
		return command;
	}

	public String getMessage() {
		return message;
	}

	public void setGlobalUses(int nUses) {
		uses.clearUses();
		uses.setGlobalUses(nUses);
	}
	
	public void setUses(int nUses) {
		uses.setUses(nUses);
	}
	
	public int getRemainingUses(Player player) {
		return uses.getRemainingUses(player.getName());
	}
	
	public void clearUses(Player player) {
		uses.clearUses(player.getName());
		if (menu != null)
			menu.autosave();
	}
	
	public void clearUses() {
		uses.clearUses();
		if (menu != null)
			menu.autosave();
	}
	
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
	
	public void feedbackMessage(Player player) {
		SMSMacro.sendFeedback(player, getMessage());
	}

	@Override
	public String toString() {
		return "SMSMenuItem [label=" + label + ", command=" + command + ", message=" + message + "]";
	}
	
	String formatUses() {
		return uses.toString();
	}
	
	String formatUses(Player player) {
		if (player == null) {
			return formatUses();
		} else {
			return uses.toString(player.getName());
		}
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((command == null) ? 0 : command.hashCode());
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result + ((message == null) ? 0 : message.hashCode());
		return result;
	}

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
