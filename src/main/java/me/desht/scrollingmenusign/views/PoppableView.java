package me.desht.scrollingmenusign.views;

import org.bukkit.entity.Player;

/**
 * Represents a view that can be popped up or down on the player's screen
 */
public interface PoppableView {
    public void showGUI(Player p);

    public void hideGUI(Player p);

    public void toggleGUI(Player p);

    public boolean hasActiveGUI(Player p);

    public SMSPopup getActiveGUI(Player p);
}
