package me.desht.scrollingmenusign.views;


public interface SMSPopup {

	public abstract SMSView getView();

	public abstract boolean isPoppedUp();

	public abstract void repaint();

	public abstract void popup();

	public abstract void popdown();

	public abstract void updateTitleJustification();

}