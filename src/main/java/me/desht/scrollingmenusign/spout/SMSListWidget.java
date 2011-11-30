package me.desht.scrollingmenusign.spout;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenuItem;
import me.desht.scrollingmenusign.util.MiscUtil;
import me.desht.scrollingmenusign.views.SMSScrollableView;
import me.desht.scrollingmenusign.views.SMSSpoutView;

import org.getspout.spoutapi.gui.GenericListWidget;
import org.getspout.spoutapi.gui.ListWidgetItem;
import org.getspout.spoutapi.player.SpoutPlayer;

public class SMSListWidget extends GenericListWidget {
	private SMSSpoutView view;

	public SMSListWidget(SMSSpoutView view) {
		this.view = view;

		for (SMSMenuItem item : view.getMenu().getItems()) {
			this.addItem(new ListWidgetItem(item.getLabel(), ""));
		}
	}

	@Override
	public void onSelected(int idx, boolean doubleClicked) {
		if (idx < 0) {
			return;
		}
		
		// bit of a hack - cast to SMSScrollableView to avoid unnecessary call to scrollTo() for the item list popup
		((SMSScrollableView)view).setScrollPos(getScreen().getPlayer().getName(), idx + 1);

		if (!doubleClicked) {
			return;
		}
		
		SMSMenuItem item = view.getMenu().getItem(idx + 1);
		SpoutPlayer player = getScreen().getPlayer();
		try {
			item.execute(player);
			item.feedbackMessage(player);
			view.onExecuted(player);
		} catch (SMSException e) {
			MiscUtil.errorMessage(player, e.getMessage());
		}
	}
	
	public void repaint() {
		clear();
		
		for (SMSMenuItem item : view.getMenu().getItems()) {
			this.addItem(new ListWidgetItem(item.getLabel(), ""));
		}
	}
}
