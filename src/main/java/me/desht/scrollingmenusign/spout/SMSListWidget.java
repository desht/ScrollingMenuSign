package me.desht.scrollingmenusign.spout;

import java.util.logging.Level;

import me.desht.scrollingmenusign.SMSConfig;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenuItem;
import me.desht.scrollingmenusign.util.MiscUtil;
import me.desht.scrollingmenusign.util.PermissionsUtils;
import me.desht.scrollingmenusign.views.SMSScrollableView;
import me.desht.scrollingmenusign.views.SMSSpoutView;

import org.bukkit.ChatColor;
import org.bukkit.configuration.Configuration;
import org.getspout.spoutapi.gui.Color;
import org.getspout.spoutapi.gui.GenericListWidget;
import org.getspout.spoutapi.gui.ListWidgetItem;
import org.getspout.spoutapi.gui.Scrollable;
import org.getspout.spoutapi.player.SpoutPlayer;

public class SMSListWidget extends GenericListWidget {
	private static final float THRESHOLD = 129;
	
	private SMSSpoutView view;
	private SpoutPlayer sp;
	private String defaultTextColor = ChatColor.BLACK.toString();
	private boolean ignoreNextSelection;

	public SMSListWidget(SpoutPlayer sp, SMSSpoutView view) {
		this.view = view;
		this.sp = sp;
		
		repaint();
	}

	public void ignoreNextSelection(boolean ignore) {
		this.ignoreNextSelection = ignore;
	}

	public Scrollable updateBackground() {
		Configuration cfg = SMSConfig.getConfig();
		String bgCol = view.getAttributeAsString(SMSSpoutView.BACKGROUND, cfg.getString("sms.spout.list_background"));
		Color c;
		try {
			c = new Color(bgCol);
			String a = view.getAttributeAsString(SMSSpoutView.ALPHA);
			if (a != null && !a.isEmpty()) {
				c.setAlpha((float) Double.parseDouble(a));
			} else {
				c.setAlpha((float) cfg.getDouble("sms.spout.list_alpha"));
			}
		} catch (NumberFormatException	e) {
			MiscUtil.log(Level.WARNING, "Invalid Spout view colour/alpha specification: using default background");
			c = new Color(cfg.getDefaults().getString("sms.spout.list_background"));
			c.setAlpha(0.5f);
		}
//		System.out.println("background = " + c.toString());
		
		// choose a contrasting text colour - black for a pale background, white for a dark background
		int luminance = (int) Math.sqrt(c.getRedI() * c.getRedI() * 0.241 + c.getGreenI() * c.getGreenI() * 0.691 + c.getBlueI() * c.getBlueI() * 0.068);
		if (luminance > THRESHOLD) {
			defaultTextColor = ChatColor.BLACK.toString();
		} else {
			defaultTextColor = ChatColor.WHITE.toString();
		}
		return setBackgroundColor(c);
	}
	
	@Override
	public void onSelected(int idx, boolean doubleClicked) {
		if (idx < 0) {
			return;
		}
		if (ignoreNextSelection) {
			// this allows us to scroll with the keyboard - we can stop here if this selection was due to
			// the player scrolling up or down with the cursor keys
			ignoreNextSelection = false;
			return;
		}
		
		// bit of a hack - cast to SMSScrollableView to avoid an unnecessary call to SpoutViewPopup.scrollTo()
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
		updateBackground();
		populateMenu();
	}

	private void populateMenu() {
		clear();
		boolean showCommand = SMSConfig.getConfig().getBoolean("sms.spout.show_command_text") && PermissionsUtils.isAllowedTo(sp, "scrollingmenusign.commands.show");
		for (SMSMenuItem item : view.getMenu().getItems()) {
			addItem(new ListWidgetItem(defaultTextColor + item.getLabel(), showCommand ? item.getCommand() : ""));
		}
	}
	
}
