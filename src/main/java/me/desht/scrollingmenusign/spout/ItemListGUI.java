package me.desht.scrollingmenusign.spout;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSMenuItem;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.enums.SMSUserAction;
import me.desht.scrollingmenusign.util.MiscUtil;
import me.desht.scrollingmenusign.views.SMSSpoutView;

import org.bukkit.ChatColor;
import org.getspout.spoutapi.event.screen.ButtonClickEvent;
import org.getspout.spoutapi.gui.Button;
import org.getspout.spoutapi.gui.Container;
import org.getspout.spoutapi.gui.GenericButton;
import org.getspout.spoutapi.gui.GenericContainer;
import org.getspout.spoutapi.gui.GenericLabel;
import org.getspout.spoutapi.gui.GenericPopup;
import org.getspout.spoutapi.gui.Label;
import org.getspout.spoutapi.gui.Screen;
import org.getspout.spoutapi.gui.Widget;
import org.getspout.spoutapi.gui.WidgetAnchor;
import org.getspout.spoutapi.player.SpoutPlayer;

public class ItemListGUI extends GenericPopup {
	private static final int GUTTER_HEIGHT = 1;
	private static final int BUTTON_HEIGHT = 20;
	private static final int BUTTON_WIDTH = 200;
	private static final int TITLE_HEIGHT = 15;
	private static final int TITLE_WIDTH = 100;
	private static final int SCROLL_WIDTH = 15;

	private SpoutPlayer sp;
	private SMSSpoutView view;
	private boolean poppedUp;
	private Map<UUID, SMSUserAction> actionMap = new HashMap<UUID, SMSUserAction>();
	private Container mainBox, buttonBox;

	public ItemListGUI(SpoutPlayer sp, SMSSpoutView view) {
		this.sp = sp;
		this.view = view;
		this.poppedUp = false;

		Screen mainScreen = sp.getMainScreen();

		mainBox = (Container) new GenericContainer();

		Label title = new GenericLabel(view.getMenu().getTitle());
		title.setX((mainScreen.getWidth() - TITLE_WIDTH) / 2).setY(5).setWidth(TITLE_WIDTH).setHeight(TITLE_HEIGHT);
		title.setAuto(false);

		buttonBox = new GenericContainer();
		int x = (mainScreen.getWidth() - BUTTON_WIDTH) / 2;
		buttonBox.setX(x).setY(TITLE_HEIGHT+10).setWidth(BUTTON_WIDTH).setHeight(mainScreen.getHeight() - (TITLE_HEIGHT + 15));
		buttonBox.setAlign(WidgetAnchor.TOP_CENTER);

		createItemButtons();

		Container scrollBox = new GenericContainer(
		                                           createScrollButton("/\\", SMSUserAction.SCROLLUP),
		                                           createScrollButton("\\/", SMSUserAction.SCROLLDOWN)
				);
		scrollBox.setX(x - (SCROLL_WIDTH + 2)).setY(TITLE_HEIGHT + 10).setWidth(SCROLL_WIDTH).setHeight(200);

		Label indicator = new GenericLabel(ChatColor.YELLOW + "<");
		indicator.setX(x + BUTTON_WIDTH + 2).setY(TITLE_HEIGHT + 15).setWidth(BUTTON_HEIGHT - 6).setHeight(BUTTON_HEIGHT - 6);
		indicator.setAuto(false);
		
		mainBox.addChild(title);
		mainBox.addChild(buttonBox);
		mainBox.addChild(scrollBox);
		mainBox.addChild(indicator);
		
		this.attachWidget(ScrollingMenuSign.getInstance(), mainBox);
	}

	private Button createScrollButton(String text, SMSUserAction action) {
		Button b = new GenericButton(text);
		b.setAuto(true).setAlign(WidgetAnchor.CENTER_CENTER);
		b.setMinHeight(15).setMaxHeight(15);
		actionMap.put(b.getId(), action);
		return b;
	}

	private void createItemButtons() {
		int idx = view.getScrollPos(sp.getName());
		int nItems = view.getMenu().getItemCount();
		int maxButtons = buttonBox.getHeight() / (BUTTON_HEIGHT + GUTTER_HEIGHT);
		for (int i = 1; i <= nItems && i <= maxButtons; i++) {
			Button button = new GenericButton(view.getMenu().getItem(idx).getLabel());
			button.setMinHeight(BUTTON_HEIGHT).setMinWidth(200).setFixed(true).setMargin(GUTTER_HEIGHT);
			buttonBox.addChild(button);
			if (++idx > nItems)
				idx = 1;
		}
	}

	public SMSSpoutView getView() {
		return view;
	}

	public boolean isPoppedUp() {
		return poppedUp;
	}

	public void repaint() {
		SMSMenu menu = view.getMenu();
		int nItems = menu.getItemCount();

		Widget[] children = buttonBox.getChildren();
		int maxButtons = buttonBox.getHeight() / (BUTTON_HEIGHT + GUTTER_HEIGHT);

		if (children.length > nItems) {
			// need to reduce the number of buttons shown
			for (int i = 0; i < children.length - nItems; i++) {
				buttonBox.removeChild(children[i]);
			}
		} else if (children.length < nItems && nItems <= maxButtons) {
			// need to increase the number of buttons shown
			for (int i = 0; i < nItems - children.length; i++) {
				Button button = new GenericButton();
				button.setMinHeight(BUTTON_HEIGHT).setMinWidth(200).setFixed(true).setMargin(GUTTER_HEIGHT);
				buttonBox.addChild(button);
			}
		}

		int idx = view.getScrollPos(sp.getName());
		for (Widget w : buttonBox.getChildren()) {
			if (!(w instanceof Button))
				continue;
			Button btn = (Button) w;
			btn.setText(menu.getItem(idx).getLabel());
			if (++idx > nItems)
				idx = 1;
		}

		buttonBox.setDirty(true);
	}

	public void popup() {	
		poppedUp = true;
		sp.getMainScreen().attachPopupScreen(this);
	}

	public void popdown() {
		poppedUp = false;
		sp.getMainScreen().closePopup();
	}

	public void handleButtonClick(ButtonClickEvent event) {
		if (actionMap.containsKey(event.getButton().getId())) {
			SMSUserAction action = actionMap.get(event.getButton().getId());
			try {
				action.execute(sp, getView());
			} catch (SMSException e) {
				MiscUtil.errorMessage(sp, e.getMessage());
			}
		} else {
			SMSMenu menu = getView().getMenu();
			String label = event.getButton().getText();
			int idx = menu.indexOfItem(label);
			if (idx > 0 && idx <= menu.getItemCount()) {
				try {
					SMSMenuItem item = menu.getItem(idx);
					item.execute(event.getPlayer());
					item.feedbackMessage(event.getPlayer());
					getView().onExecuted(event.getPlayer());
				} catch (SMSException e) {
					MiscUtil.errorMessage(event.getPlayer(), e.getMessage());
				}
			} else {
				MiscUtil.log(Level.WARNING, "Unexpected index " + idx + " for [" + label + "], menu " + menu.getName());
			}
		}
	}
}
