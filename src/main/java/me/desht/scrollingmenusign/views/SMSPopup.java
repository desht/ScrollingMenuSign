package me.desht.scrollingmenusign.views;

import org.bukkit.entity.Player;


public interface SMSPopup {

	public abstract SMSView getView();

	public abstract void repaint();

//	public abstract void rejustify();

	public abstract boolean isPoppedUp(Player p);

	public abstract void popup(Player p);

	public abstract void popdown(Player p);

}