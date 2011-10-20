package me.desht.scrollingmenusign.spout;

import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.views.SMSSpoutView;

import org.getspout.spoutapi.gui.Button;
import org.getspout.spoutapi.gui.Container;
import org.getspout.spoutapi.gui.ContainerType;
import org.getspout.spoutapi.gui.GenericButton;
import org.getspout.spoutapi.gui.GenericContainer;
import org.getspout.spoutapi.gui.GenericLabel;
import org.getspout.spoutapi.gui.GenericPopup;
import org.getspout.spoutapi.gui.Label;
import org.getspout.spoutapi.gui.Widget;
import org.getspout.spoutapi.player.SpoutPlayer;

public class ItemListGUI extends GenericPopup {
	private static final int GUTTER_HEIGHT = 2;
	private static final int BUTTON_HEIGHT = 24;
	private static final int VIEW_WIDTH = 200;
	
	private SpoutPlayer sp;
	private SMSSpoutView view;
	private boolean poppedUp;

	private Container mainBox, itemBox;
	
	public ItemListGUI(SpoutPlayer sp, SMSSpoutView view) {
		this.sp = sp;
		this.view = view;
		this.poppedUp = false;
		
		SMSMenu menu = view.getMenu();
		int nItems = menu.getItemCount();
		
		int maxHeight = (sp.getMainScreen().getHeight() * 90) / 100;
		int maxButtons = maxHeight / (BUTTON_HEIGHT + GUTTER_HEIGHT);
		
		mainBox = new GenericContainer();
		
		mainBox.setLayout(ContainerType.VERTICAL);
		mainBox.setX((sp.getMainScreen().getWidth() - VIEW_WIDTH) / 2).setY(10).setWidth(VIEW_WIDTH).setHeight(maxHeight);
		
		itemBox = new GenericContainer();
//		int boxHeight = Math.min(maxHeight, nItems * (BUTTON_HEIGHT + GUTTER_HEIGHT)) - (BUTTON_HEIGHT + GUTTER_HEIGHT);
//		System.out.println("box height = " + boxHeight + ", max = " + maxHeight);
//		itemBox.setX(20).setY(10).setWidth(200); // .setHeight(boxHeight);
		itemBox.setLayout(ContainerType.VERTICAL);
		
		int idx = view.getScrollPos(sp.getName());
		for (int i = 1; i <= maxButtons && i <= nItems; i++) {
			Button btn = new GenericButton(menu.getItem(idx).getLabel());
			btn.setHeight(BUTTON_HEIGHT);
			btn.setMargin(GUTTER_HEIGHT);
			itemBox.addChild(btn);
			idx++;
			if (idx > nItems)
				idx = 1;
		}
		
		Label titleLabel = new GenericLabel(menu.getTitle());
		titleLabel.setHeight(BUTTON_HEIGHT);
		titleLabel.setMargin(GUTTER_HEIGHT);
		mainBox.addChild(titleLabel);
		mainBox.addChild(itemBox);
		
		attachWidget(ScrollingMenuSign.getInstance(), mainBox);
	}

	public SMSSpoutView getView() {
		return view;
	}
	
	public SpoutPlayer getPlayer() {
		return sp;
	}
	
	public boolean isPoppedUp() {
		return poppedUp;
	}

	public void repaint() {
		System.out.println("repaint spout gui for " + view.getMenu().getName());
		
		SMSMenu menu = view.getMenu();
		int nItems = menu.getItemCount();
		int maxButtons = maxHeight / (BUTTON_HEIGHT + GUTTER_HEIGHT);
		
		Widget[] children = itemBox.getChildren();
		if (children.length > nItems) {
			// need to reduce the number of buttons shown
			for (int i = 0; i < children.length - nItems; i++) {
				itemBox.removeChild(children[i]);
			}
//			itemBox.setHeight(Math.min(maxHeight, nItems * (BUTTON_HEIGHT + GUTTER_HEIGHT)));
		} else if (children.length < nItems && nItems < maxButtons) {
			// need to increase the number of buttons shown
			for (int i = 0; i < nItems - children.length; i++) {
				Button btn = new GenericButton();
				btn.setHeight(BUTTON_HEIGHT);
				btn.setMargin(GUTTER_HEIGHT);
				itemBox.addChild(btn);
			}
//			itemBox.setHeight(Math.min(maxHeight, nItems * (BUTTON_HEIGHT + GUTTER_HEIGHT)));
		}
		
		int idx = view.getScrollPos(sp.getName());
		for (Widget w : itemBox.getChildren()) {
			if (!(w instanceof Button))
				continue;
			Button btn = (Button) w;
			btn.setText(menu.getItem(idx).getLabel());
			idx++;
			if (idx > nItems)
				idx = 1;
		}
		
		itemBox.setDirty(true);
	}

	public void popup() {
		System.out.println("pop up spout gui for " + sp.getName() + " - " + view.getMenu().getName());
		
		poppedUp = true;
		sp.getMainScreen().attachPopupScreen(this);
	}

	public void popdown() {
		System.out.println("pop down spout gui for "+ view.getMenu().getName());
		
		poppedUp = false;
		sp.getMainScreen().closePopup();
	}

}
