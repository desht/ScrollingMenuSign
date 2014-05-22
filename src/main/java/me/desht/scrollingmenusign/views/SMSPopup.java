package me.desht.scrollingmenusign.views;

import org.bukkit.entity.Player;

/**
 * Represents the component of a {@link PoppableView} that is actually popped up or down.
 */
public interface SMSPopup {
    public SMSView getView();

    public void repaint();

    public boolean isPoppedUp();

    public void popup();

    public void popdown();

    public Player getPlayer();
}