package me.desht.scrollingmenusign.spout;

import me.desht.scrollingmenusign.SMSConfig;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenuItem;
import me.desht.scrollingmenusign.util.MiscUtil;
import me.desht.scrollingmenusign.util.PermissionsUtils;
import me.desht.scrollingmenusign.views.SMSScrollableView;
import me.desht.scrollingmenusign.views.SMSSpoutView;

import org.getspout.spoutapi.gui.Color;
import org.getspout.spoutapi.gui.GenericListWidget;
import org.getspout.spoutapi.gui.ListWidgetItem;
import org.getspout.spoutapi.player.SpoutPlayer;

public class SMSListWidget extends GenericListWidget {
	private SMSSpoutView view;
	private SpoutPlayer sp;

	public SMSListWidget(SpoutPlayer sp, SMSSpoutView view) {
		this.view = view;
		this.sp = sp;
		
		setColor(new Color(0, 0, 0));
		setBackgroundColor(new Color(0.99f, 0.89f, 0.57f, 0.8f));
		
		populateMenu();
	}

	@Override
	public void onSelected(int idx, boolean doubleClicked) {
		if (idx < 0) {
			return;
		}
		
		// bit of a hack - cast to SMSScrollableView to avoid unnecessary call to scrollTo() for the item list popup
		((SMSScrollableView)view).setScrollPos(getScreen().getPlayer().getName(), idx + 1);

		if (!doubleClicked && SMSConfig.getConfig().getBoolean("sms.spout.double_click")) {
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
		populateMenu();
	}

	private void populateMenu() {
		clear();
		boolean showCommand = SMSConfig.getConfig().getBoolean("sms.spout.show_command_text") && PermissionsUtils.isAllowedTo(sp, "scrollingmenusign.commands.show");
		for (SMSMenuItem item : view.getMenu().getItems()) {
			addItem(new ListWidgetItem(item.getLabel(), showCommand ? item.getCommand() : ""));
		}
	}
	
}
