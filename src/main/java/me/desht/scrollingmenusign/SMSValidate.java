package me.desht.scrollingmenusign;

/**
 * Validation convenience methods.
 */
public class SMSValidate {
	public static void isTrue(boolean cond, String err) {
		if (!cond) throw new SMSException(err);
	}

	public static void isFalse(boolean cond, String err) {
		if (cond) throw new SMSException(err);
	}

	public static void notNull(Object o, String err) {
		if (o == null) throw new SMSException(err);
	}
}
