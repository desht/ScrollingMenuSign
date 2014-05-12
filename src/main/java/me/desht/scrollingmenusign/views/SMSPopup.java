package me.desht.scrollingmenusign.views;

import org.bukkit.entity.Player;

/**
 * Represents the component of a {@link PoppableView} that is actually popped up or down.
 */
public interface SMSPopup {
    public abstract SMSView getView();

    public abstract void repaint();

    public abstract boolean isPoppedUp(Player p);

    public abstract void popup(Player p);

    public abstract void popdown(Player p);
}