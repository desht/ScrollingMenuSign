package me.desht.scrollingmenusign.spout;

import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.views.SMSSpoutView;

import org.getspout.spoutapi.gui.Container;
import org.getspout.spoutapi.gui.GenericContainer;
import org.getspout.spoutapi.gui.GenericLabel;
import org.getspout.spoutapi.gui.GenericPopup;
import org.getspout.spoutapi.gui.Label;
import org.getspout.spoutapi.gui.Screen;
import org.getspout.spoutapi.gui.WidgetAnchor;
import org.getspout.spoutapi.player.SpoutPlayer;

public class SpoutViewPopup extends GenericPopup {
	private static final int LIST_WIDTH = 200;
	private static final int TITLE_HEIGHT = 15;
	private static final int TITLE_WIDTH = 100;

	private SpoutPlayer sp;
	private SMSSpoutView view;
	private boolean poppedUp;
	private Label title;
	private Container mainBox;
	private SMSListWidget listWidget;

	public SpoutViewPopup(SpoutPlayer sp, SMSSpoutView view) {
		this.sp = sp;
		this.view = view;
		this.poppedUp = false;

		Screen mainScreen = sp.getMainScreen();

		mainBox = (Container) new GenericContainer();
		mainBox.setX((mainScreen.getWidth() - LIST_WIDTH) / 2).setY(5).setWidth(LIST_WIDTH).setHeight(mainScreen.getHeight() - 10);
		mainBox.setAlign(WidgetAnchor.TOP_CENTER);

		title = new GenericLabel(view.getMenu().getTitle());
		title.setMaxHeight(TITLE_HEIGHT).setMaxWidth(TITLE_WIDTH).setAnchor(WidgetAnchor.CENTER_CENTER);
		title.setMargin(2);

		listWidget = new SMSListWidget(sp, view);
		listWidget.setMargin(2);

		mainBox.addChild(title);
		mainBox.addChild(listWidget);

		this.attachWidget(ScrollingMenuSign.getInstance(), mainBox);
	}

	public SMSSpoutView getView() {
		return view;
	}

	public boolean isPoppedUp() {
		return poppedUp;
	}

	public void repaint() {
		title.setText(view.getMenu().getTitle());
		listWidget.repaint();
	}

	public void scrollTo(int scrollPos) {
		listWidget.setSelection(scrollPos - 1);
		System.out.println("scroll to " + scrollPos + " = " + listWidget.getSelectedItem().getTitle());
	}

	public void popup() {	
		poppedUp = true;
		sp.getMainScreen().attachPopupScreen(this);
	}

	public void popdown() {
		poppedUp = false;
		sp.getMainScreen().closePopup();
	}

}
