package me.desht.scrollingmenusign;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Player;

public class SMSMenuItem implements Comparable<SMSMenuItem> {
	private final String label;
	private final String command;
	private final String message;
	private final SMSRemainingUses uses;
	
	SMSMenuItem(String l, String c, String m) {
		if (l == null || c == null || m == null)
			throw new NullPointerException();
		
		this.label = l;
		this.command = c;
		this.message = m;
		this.uses = new SMSRemainingUses();
	}
	
	@SuppressWarnings("unchecked")
	SMSMenuItem(Map<String, Object> map) {
		this.label = SMSUtils.parseColourSpec(null, (String) map.get("label"));
		this.command = (String) map.get("command");
		this.message = SMSUtils.parseColourSpec(null, (String) map.get("message"));
		this.uses = new SMSRemainingUses((Map<String, Integer>) map.get("usesRemaining"));
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
	
	public void clearUses(Player player) {
		uses.clearUses(player.getName());
	}
	
	public void clearUses() {
		uses.clearUses();
	}
	
	public void execute(Player player) throws SMSException {
		if (uses.getUses(player.getName()) == 0) {
			throw new SMSException("You can't use that menu item anymore");
		}
		uses.use(player.getName());
		SMSMacro.executeCommand(getCommand(), player);
	}
	
	public void feedbackMessage(Player player) {
		SMSMacro.sendFeedback(player, getMessage());
	}

	@Override
	public String toString() {
		return "SMSMenuItem [label=" + label + ", command=" + command + ", message=" + message + "]";
	}
	
	public String getUsesAsString() {
		return uses.toString();
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
}
