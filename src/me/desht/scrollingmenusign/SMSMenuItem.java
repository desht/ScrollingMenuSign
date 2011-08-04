package me.desht.scrollingmenusign;

public class SMSMenuItem implements Comparable<SMSMenuItem> {
	private String label;
	private String command;
	private String message;
	
	SMSMenuItem(String l, String c, String m) {
		if (l == null || c == null || m == null)
			throw new NullPointerException();
		
		this.label = l;
		this.command = c;
		this.message = m;
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

	@Override
	public String toString() {
		return "SMSMenuItem [label=" + label + ", command=" + command + ", message=" + message + "]";
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
}
