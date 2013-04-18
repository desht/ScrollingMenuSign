package me.desht.scrollingmenusign.spout;

import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSMenuItem;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.views.SMSScrollableView;
import me.desht.scrollingmenusign.views.SMSSpoutView;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.getspout.spoutapi.gui.Color;
import org.getspout.spoutapi.gui.GenericListWidget;
import org.getspout.spoutapi.gui.ListWidgetItem;
import org.getspout.spoutapi.gui.Scrollable;
import org.getspout.spoutapi.player.SpoutPlayer;

import com.google.common.base.Joiner;

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

	public SMSSpoutView getView() {
		return view;
	}

	public void ignoreNextSelection(boolean ignore) {
		this.ignoreNextSelection = ignore;
	}

	public Scrollable updateBackground() {
		HexColor cw = (HexColor) view.getAttribute(SMSSpoutView.BACKGROUND);
		double alpha = (Double) view.getAttribute(SMSSpoutView.ALPHA);
		Color c = cw.getColor();
		c.setAlpha((float)alpha);
		LogUtils.finer("updateBackground: view = " + view.getName() + " background = " + c.toString());

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

		if (!doubleClicked && ScrollingMenuSign.getInstance().getConfig().getBoolean("sms.spout.double_click")) {
			return;
		}

		final SpoutPlayer player = getScreen().getPlayer();
		SMSMenu menu = view.getActiveMenu(player.getName());
		SMSMenuItem item = view.getActiveMenuItemAt(sp.getName(), idx + 1);
		try {
			if (item == null) {
				throw new SMSException("spout list widget onSelected: index " + idx + " out of range for " + menu.getName() + " ?");
			}
			item.executeCommand(player, view);
			item.feedbackMessage(player);
			view.onExecuted(player);
			if (menu != view.getActiveMenu(player.getName())) {
				Bukkit.getScheduler().runTaskLater(ScrollingMenuSign.getInstance(), new Runnable() {
					@Override
					public void run() {
						view.showGUI(player);
					}
				}, 2L);
			}
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

		boolean showTooltips = ScrollingMenuSign.getInstance().getConfig().getBoolean("sms.spout.show_tooltips");

		int nItems = view.getActiveMenuItemCount(sp.getName());
		for (int i = 1; i <= nItems; i++) {
			SMSMenuItem item = view.getActiveMenuItemAt(sp.getName(), i);
			String lore = Joiner.on(" ").join(item.getLore());
			addItem(new ListWidgetItem(defaultTextColor + view.variableSubs(item.getLabel()), showTooltips ? lore : ""));
		}
	}

}
