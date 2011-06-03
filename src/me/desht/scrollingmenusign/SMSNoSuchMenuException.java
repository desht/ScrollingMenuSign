package me.desht.scrollingmenusign;

public class SMSNoSuchMenuException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2131688652829977326L;

	String error;

	public SMSNoSuchMenuException() {
		super();
	}
	
	public SMSNoSuchMenuException(String errorMsg) {
		super();
		error = errorMsg;
	}

	public String getError() {
		return error;
	}
}
