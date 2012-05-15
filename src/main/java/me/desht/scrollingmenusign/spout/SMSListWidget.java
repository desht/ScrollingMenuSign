package me.desht.scrollingmenusign.spout;

import me.desht.scrollingmenusign.SMSConfig;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenuItem;
import me.desht.scrollingmenusign.util.MiscUtil;
import me.desht.scrollingmenusign.util.PermissionsUtils;
import me.desht.scrollingmenusign.util.SMSLogger;
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
	
	private final SMSSpoutView view;
	private final SpoutPlayer sp;
	
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
			String a = view.getAttributeAsString(SMSSpoutView.ALPHA, cfg.getString("sms.spout.list_alpha"));
			c.setAlpha(Float.parseFloat(a));
		} catch (NumberFormatException	e) {
			SMSLogger.warning("Invalid Spout view colour/alpha specification for " + view.getName() + ": using default settings");
			c = new Color(cfg.getDefaults().getString("sms.spout.list_background"));
			c.setAlpha((float) cfg.getDefaults().getDouble("sms.spout.list_alpha"));
		}
		SMSLogger.finer("updateBackground: view = " + view.getName() + " background = " + c.toString());
		
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
		
		SMSMenuItem item = view.getMenu().getItemAt(idx + 1);
		SpoutPlayer player = getScreen().getPlayer();
		try {
			if (item == null) {
				throw new SMSException("spout list widget onSelected: index " + idx + " out of range for " + view.getMenu().getName() + " ?");
			}
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
