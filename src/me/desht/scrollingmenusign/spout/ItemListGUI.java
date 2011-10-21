package me.desht.scrollingmenusign.spout;

import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.views.SMSSpoutView;

import org.getspout.spoutapi.gui.Button;
import org.getspout.spoutapi.gui.Container;
import org.getspout.spoutapi.gui.GenericButton;
import org.getspout.spoutapi.gui.GenericContainer;
import org.getspout.spoutapi.gui.GenericLabel;
import org.getspout.spoutapi.gui.GenericPopup;
import org.getspout.spoutapi.gui.Label;
import org.getspout.spoutapi.gui.Widget;
import org.getspout.spoutapi.gui.WidgetAnchor;
import org.getspout.spoutapi.player.SpoutPlayer;

public class ItemListGUI extends GenericPopup {
	private static final int GUTTER_HEIGHT = 2;
	private static final int BUTTON_HEIGHT = 30;
//	private static final int VIEW_WIDTH = 200;
//	private static final int BUTTON_START_Y = 40;
	
	private SpoutPlayer sp;
	private SMSSpoutView view;
	private boolean poppedUp;

	private Container mainBox, buttonBox;
	
	public ItemListGUI(SpoutPlayer sp, SMSSpoutView view) {
		this.sp = sp;
		this.view = view;
		this.poppedUp = false;
		
		Container mainBox = (Container) new GenericContainer().setAlign(WidgetAnchor.TOP_CENTER).setHeight(240).setWidth(427);
		Label title = new GenericLabel("some text");
		 
		Container buttonBox = new GenericContainer();
		for (int i = 0; i < 6; i++) {
		  Button b = new GenericButton("button " + i);
		  buttonBox.addChild(b);
		}
		mainBox.addChild(title);
		mainBox.addChild(buttonBox);
		this.attachWidget(ScrollingMenuSign.getInstance(), mainBox);
		
//		SMSMenu menu = view.getMenu();
////		int nItems = menu.getItemCount();
//		
////		int maxHeight = (sp.getMainScreen().getHeight() * 90) / 100;
////		int maxButtons = maxHeight / (BUTTON_HEIGHT + GUTTER_HEIGHT);
//		
//		mainBox = new GenericContainer();
//		mainBox.setWidth(200);
//		
////		mainBox.setLayout(ContainerType.VERTICAL);
////		mainBox.setAnchor(WidgetAnchor.TOP_CENTER);
////		mainBox.setWidth(VIEW_WIDTH); // .setHeight(maxHeight);
//		
//		Label titleLabel = new GenericLabel(menu.getTitle());
////		titleLabel.setAlign(WidgetAnchor.CENTER_CENTER);
////		titleLabel.setX(200).setY(10).setHeight(BUTTON_HEIGHT);
////		titleLabel.setMargin(GUTTER_HEIGHT);
//		mainBox.addChild(titleLabel);
//		
////		mainBox.setX((sp.getMainScreen().getWidth() - VIEW_WIDTH) / 2).setY(10).setAnchor(WidgetAnchor.CENTER_CENTER); //.setWidth(VIEW_WIDTH).setHeight(maxHeight);
//		
//		createItemButtons();
//
//		mainBox.addChild(buttonBox);
//		
//		attachWidget(ScrollingMenuSign.getInstance(), mainBox);
	}

	private void createItemButtons() {
		if (buttonBox == null) {
			buttonBox = new GenericContainer();
			buttonBox.setX(200).setY(40).setWidth(200).setHeight(190);
		}
		
//		int yPos = BUTTON_START_Y;
		int idx = view.getScrollPos(sp.getName());
		int nItems = view.getMenu().getItemCount();
		for (int i = 1; i <= nItems; i++) {
			Button button = new GenericButton(view.getMenu().getItem(idx).getLabel());
//			btn.setX(200).setY(yPos);
//			button.setHeight(BUTTON_HEIGHT).setMargin(GUTTER_HEIGHT);
			System.out.println("add button " + button);
			button.setAnchor(WidgetAnchor.TOP_CENTER);
			buttonBox.addChild(button);
			idx++;
			if (idx > nItems)
				idx = 1;
//			yPos += BUTTON_HEIGHT + GUTTER_HEIGHT;
//			if (yPos > buttonBox.getHeight())
//				break;
		}
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
		
		Widget[] children = buttonBox.getChildren();
//		if (children.length != nItems) {
			for (Widget w : children) {
				buttonBox.removeChild(w);
			}
			createItemButtons();
//		}
		
//		if (children.length > nItems) {
//			// need to reduce the number of buttons shown
//			for (int i = 0; i < children.length - nItems; i++) {
//				buttonBox.removeChild(children[i]);
//			}
////			itemBox.setHeight(Math.min(maxHeight, nItems * (BUTTON_HEIGHT + GUTTER_HEIGHT)));
//		} else if (children.length < nItems && nItems < maxButtons) {
//			// need to increase the number of buttons shown
//			for (int i = 0; i < nItems - children.length; i++) {
//				Button btn = new GenericButton();
//				btn.setHeight(BUTTON_HEIGHT);
//				btn.setMargin(GUTTER_HEIGHT);
//				buttonBox.addChild(btn);
//			}
////			itemBox.setHeight(Math.min(maxHeight, nItems * (BUTTON_HEIGHT + GUTTER_HEIGHT)));
//		}
//		
		int idx = view.getScrollPos(sp.getName());
		for (Widget w : buttonBox.getChildren()) {
			if (!(w instanceof Button))
				continue;
			Button btn = (Button) w;
			btn.setText(menu.getItem(idx).getLabel());
			idx++;
			if (idx > nItems)
				idx = 1;
		}
		
		mainBox.setDirty(true);
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
