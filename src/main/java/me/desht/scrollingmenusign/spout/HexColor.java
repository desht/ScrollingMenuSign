package me.desht.scrollingmenusign.spout;

import org.getspout.spoutapi.gui.Color;

/**
 * Wrap a Spout Color object in a class for ease of attribute management.
 */
public class HexColor {
	private final Color color;

	public HexColor(String s) {
		color = new Color(s);
	}

	public Color getColor() {
		return color;
	}

	@Override
	public String toString() {
		return String.format("%02x%02x%02x", color.getRedI(), color.getGreenI(), color.getBlueI());
	}
}
