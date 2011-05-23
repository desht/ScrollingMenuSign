package me.desht.scrollingmenusign;

public class SMSMenuItem {
	private String label;
	private String command;
	private String message;
	
	public SMSMenuItem(String l, String c, String m) {
		setLabel(l);
		setCommand(c);
		setMessage(m);
	}
	
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public String getCommand() {
		return command;
	}
	public void setCommand(String command) {
		this.command = command;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
}
