package me.desht.scrollingmenusign.spout;

import java.net.MalformedURLException;
import java.net.URL;

import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.dhutils.LogUtils;
import me.desht.scrollingmenusign.views.SMSSpoutView;

import org.getspout.spoutapi.gui.GenericLabel;
import org.getspout.spoutapi.gui.GenericPopup;
import org.getspout.spoutapi.gui.GenericTexture;
import org.getspout.spoutapi.gui.Label;
import org.getspout.spoutapi.gui.RenderPriority;
import org.getspout.spoutapi.gui.Screen;
import org.getspout.spoutapi.gui.Texture;
import org.getspout.spoutapi.gui.WidgetAnchor;
import org.getspout.spoutapi.player.SpoutPlayer;

public class SpoutViewPopup extends GenericPopup {
	private static final int LIST_WIDTH = 200;
	private static final int TITLE_HEIGHT = 15;
	private static final int TITLE_WIDTH = 100;

	private final SpoutPlayer sp;
	private final SMSSpoutView view;
	private final Label title;
	private final SMSListWidget listWidget;
	
	private final Texture texture;
	private boolean poppedUp;
	
	public SpoutViewPopup(SpoutPlayer sp, SMSSpoutView view) {
		this.sp = sp;
		this.view = view;
		this.poppedUp = false;

		Screen mainScreen = sp.getMainScreen();

		title = new GenericLabel(view.getMenu().getTitle());
		title.setX((mainScreen.getWidth() - TITLE_WIDTH) / 2).setY(5).setWidth(TITLE_WIDTH).setHeight(TITLE_HEIGHT);
		title.setAuto(false	);
		updateTitleJustification();

		int listX = (mainScreen.getWidth() - LIST_WIDTH) / 2;
		int listY = 5 + 2 + TITLE_HEIGHT;
		
		String textureName = view.getAttributeAsString(SMSSpoutView.TEXTURE);
		texture = makeTexture(textureName);
		
		listWidget = new SMSListWidget(sp, view);
		listWidget.setX(listX).setY(listY).setWidth(LIST_WIDTH).setHeight(LIST_WIDTH);

		this.attachWidget(ScrollingMenuSign.getInstance(), title);
		if (texture != null) {
			texture.setX(listX).setY(listY).setWidth(LIST_WIDTH).setHeight(LIST_WIDTH);
			this.attachWidget(ScrollingMenuSign.getInstance(), texture);
		}
		this.attachWidget(ScrollingMenuSign.getInstance(), listWidget);
	}
	
	private Texture makeTexture(String textureName) {
		Texture t = null;
		if (textureName != null && !textureName.isEmpty()) {
			try {
				URL textureURL = ScrollingMenuSign.makeImageURL(textureName);
				t = new GenericTexture(textureURL.toString());
				t.setDrawAlphaChannel(true);
				t.setPriority(RenderPriority.Highest);	// put it behind the list widget
			} catch (MalformedURLException e) {
				LogUtils.warning("malformed texture URL for spout view " + view.getName() + ": " + e.getMessage());
			}
		}
		return t;
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
	
	public void updateTitleJustification() {
		switch (getView().getTitleJustification()) {
		case LEFT:
			title.setAlign(WidgetAnchor.CENTER_LEFT); break;
		case RIGHT:
			title.setAlign(WidgetAnchor.CENTER_LEFT); break;
		case CENTER:
		default:
			title.setAlign(WidgetAnchor.CENTER_LEFT); break;
		}
	}
}
