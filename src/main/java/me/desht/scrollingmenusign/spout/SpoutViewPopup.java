package me.desht.scrollingmenusign.spout;

import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.views.SMSSpoutView;

import org.getspout.spoutapi.gui.GenericLabel;
import org.getspout.spoutapi.gui.GenericPopup;
import org.getspout.spoutapi.gui.GenericTexture;
import org.getspout.spoutapi.gui.Label;
import org.getspout.spoutapi.gui.RenderPriority;
import org.getspout.spoutapi.gui.Screen;
import org.getspout.spoutapi.gui.Texture;
import org.getspout.spoutapi.player.SpoutPlayer;

public class SpoutViewPopup extends GenericPopup {
	private static final int LIST_WIDTH = 200;
	private static final int TITLE_HEIGHT = 15;
	private static final int TITLE_WIDTH = 100;

	private SpoutPlayer sp;
	private SMSSpoutView view;
	private boolean poppedUp;
	private Label title;
	private Texture texture;
	private SMSListWidget listWidget;

	public SpoutViewPopup(SpoutPlayer sp, SMSSpoutView view) {
		this.sp = sp;
		this.view = view;
		this.poppedUp = false;

		Screen mainScreen = sp.getMainScreen();

		title = new GenericLabel(view.getMenu().getTitle());
		title.setX((mainScreen.getWidth() - TITLE_WIDTH) / 2).setY(5).setWidth(TITLE_WIDTH).setHeight(TITLE_HEIGHT);
		title.setAuto(false	);

		int listX = (mainScreen.getWidth() - LIST_WIDTH) / 2;
		int listY = 5 + 2 + TITLE_HEIGHT;
		
		texture = new GenericTexture(view.getAttributeAsString(SMSSpoutView.TEXTURE));
		texture.setDrawAlphaChannel(true);
		texture.setX(listX).setY(listY).setWidth(LIST_WIDTH).setHeight(LIST_WIDTH);
		texture.setPriority(RenderPriority.Highest);	// put it behind the list widget
		
		listWidget = new SMSListWidget(sp, view);
		listWidget.setX(listX).setY(listY).setWidth(LIST_WIDTH).setHeight(LIST_WIDTH);

		this.attachWidget(ScrollingMenuSign.getInstance(), title);
		this.attachWidget(ScrollingMenuSign.getInstance(), texture);
		this.attachWidget(ScrollingMenuSign.getInstance(), listWidget);
	}

	public SMSSpoutView getView() {
		return view;
	}

	public boolean isPoppedUp() {
		return poppedUp;
	}

	public void repaint() {
		title.setText(view.getMenu().getTitle());
		texture.setUrl(view.getAttributeAsString(SMSSpoutView.TEXTURE));
		listWidget.repaint();
	}

	public void scrollTo(int scrollPos) {
		listWidget.ignoreNextSelection(true);
		listWidget.setSelection(scrollPos - 1);
//		System.out.println("scroll to " + scrollPos + " = " + listWidget.getSelectedItem().getTitle());
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
