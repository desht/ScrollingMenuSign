package me.desht.scrollingmenusign.views;

import org.bukkit.entity.Player;

/**
 * Represents a view that can be popped up or down on the player's screen
 */
public interface PoppableView {
    public void showGUI(Player player);

    public void hideGUI(Player player);

    public void toggleGUI(Player player);

    public boolean hasActiveGUI(Player player);

    public SMSPopup getActiveGUI(Player player);
}
