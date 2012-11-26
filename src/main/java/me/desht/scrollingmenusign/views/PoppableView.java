package me.desht.scrollingmenusign.views;

import org.bukkit.entity.Player;

public interface PoppableView {
	public void showGUI(Player p);
	public void hideGUI(Player p);
	public void toggleGUI(Player p);
	public boolean hasActiveGUI(Player p);
	public SMSPopup getActiveGUI(Player p);
}
