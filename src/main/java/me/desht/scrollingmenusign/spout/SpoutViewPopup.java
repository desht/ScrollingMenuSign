package me.desht.scrollingmenusign.spout;

import me.desht.dhutils.LogUtils;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.views.SMSPopup;
import me.desht.scrollingmenusign.views.SMSSpoutView;

import org.bukkit.entity.Player;
import org.getspout.spoutapi.gui.GenericLabel;
import org.getspout.spoutapi.gui.Label;
import org.getspout.spoutapi.gui.Screen;
import org.getspout.spoutapi.gui.WidgetAnchor;
import org.getspout.spoutapi.player.SpoutPlayer;

public class SpoutViewPopup extends SMSGenericPopup implements SMSPopup {
	private static final int LIST_WIDTH = 200;
	private static final int TITLE_HEIGHT = 15;
	private static final int TITLE_WIDTH = 100;

	private final Label title;
	private final SMSSpoutView view;
	private final SMSListWidget listWidget;
	private final SMSListTexture texture;
	private final SpoutPlayer sp;
	
	private boolean poppedUp;

	public SpoutViewPopup(SpoutPlayer sp, SMSSpoutView view) {
		this.sp = sp;
		this.view = view;
		this.poppedUp = false;

		Screen mainScreen = sp.getMainScreen();

		title = new GenericLabel(view.variableSubs(view.getActiveMenu(sp.getName()).getTitle()));
		title.setX((mainScreen.getWidth() - TITLE_WIDTH) / 2).setY(15).setWidth(TITLE_WIDTH).setHeight(TITLE_HEIGHT);
		title.setAnchor(WidgetAnchor.TOP_LEFT);
		title.setAuto(false	);
		rejustify();

		int listX = (mainScreen.getWidth() - LIST_WIDTH) / 2;
		int listY = 5 + 2 + TITLE_HEIGHT;

		texture = new SMSListTexture(this);

		listWidget = new SMSListWidget(sp, view);
		listWidget.setX(listX).setY(listY).setWidth(LIST_WIDTH).setHeight(LIST_WIDTH);

		this.attachWidget(ScrollingMenuSign.getInstance(), title);
		if (texture != null) {
			texture.setX(listX).setY(listY).setWidth(LIST_WIDTH).setHeight(LIST_WIDTH);
			this.attachWidget(ScrollingMenuSign.getInstance(), texture);
		}
		this.attachWidget(ScrollingMenuSign.getInstance(), listWidget);
	}

	/* (non-Javadoc)
	 * @see me.desht.scrollingmenusign.spout.SMSPopup#getView()
	 */
	@Override
	public SMSSpoutView getView() {
		return view;
	}

	/* (non-Javadoc)
	 * @see me.desht.scrollingmenusign.spout.SMSPopup#isPoppedUp()
	 */
	@Override
	public boolean isPoppedUp(Player p) {
		return poppedUp;
	}

	/* (non-Javadoc)
	 * @see me.desht.scrollingmenusign.spout.SMSPopup#repaint()
	 */
	@Override
	public void repaint() {
		title.setText(view.variableSubs(view.getActiveMenuTitle(sp.getName())));
		rejustify();
		texture.updateURL();
		listWidget.repaint();
	}

	/* (non-Javadoc)
	 * @see me.desht.scrollingmenusign.spout.SMSPopup#popup()
	 */
	@Override
	public void popup(Player p) {
		poppedUp = true;
		((SpoutPlayer) p).getMainScreen().attachPopupScreen(this);
	}

	/* (non-Javadoc)
	 * @see me.desht.scrollingmenusign.spout.SMSPopup#popdown()
	 */
	@Override
	public void popdown(Player p) {
		poppedUp = false;
		((SpoutPlayer) p).getMainScreen().closePopup();
	}

	private void rejustify() {
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

	/* (non-Javadoc)
	 * @see me.desht.scrollingmenusign.spout.SMSPopup#scrollTo(int)
	 */
	public void scrollTo(int scrollPos) {
		listWidget.ignoreNextSelection(true);
		listWidget.setSelection(scrollPos - 1);
		
		LogUtils.fine("scroll to " + scrollPos + " = " + listWidget.getSelectedItem().getTitle());
	}
}
