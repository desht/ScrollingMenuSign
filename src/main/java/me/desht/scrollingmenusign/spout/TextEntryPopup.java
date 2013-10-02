package me.desht.scrollingmenusign.spout;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import me.desht.dhutils.DHUtilsException;
import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.expector.ExpectCommandSubstitution;

import org.bukkit.ChatColor;
import org.getspout.spoutapi.event.screen.ButtonClickEvent;
import org.getspout.spoutapi.gui.GenericButton;
import org.getspout.spoutapi.gui.GenericLabel;
import org.getspout.spoutapi.gui.GenericTextField;
import org.getspout.spoutapi.gui.Label;
import org.getspout.spoutapi.gui.Screen;
import org.getspout.spoutapi.gui.TextField;
import org.getspout.spoutapi.player.SpoutPlayer;

public class TextEntryPopup extends SMSGenericPopup {
	private static final Map<String,TextEntryPopup> allPopups = new HashMap<String, TextEntryPopup>();
	private static final Set<String> visiblePopups = new HashSet<String>();

	private static final String LABEL_COLOUR = ChatColor.YELLOW.toString();
	private static final int BUTTON_HEIGHT = 20;

	private final SpoutPlayer sp;
	private final Label label;
	private final TextField textField;
	private final TextEntryButton okButton, cancelButton;

	public TextEntryPopup(SpoutPlayer sp, String prompt) {
		this.sp = sp;
		Screen mainScreen = sp.getMainScreen();

		int width = mainScreen.getWidth() / 2;
		int x = (mainScreen.getWidth() - width) / 2;
		int y = mainScreen.getHeight() / 2 - 20;

		label = new GenericLabel(LABEL_COLOUR + prompt);
		label.setX(x).setY(y).setWidth(width).setHeight(10);
		y += label.getHeight() + 2;

		textField = new GenericTextField();
		textField.setX(x).setY(y).setWidth(width).setHeight(20);
		textField.setFocus(true	);
		textField.setMaximumCharacters(0);
		y+= textField.getHeight() + 5;

		okButton = new TextEntryButton("OK");
		okButton.setX(x).setY(y).setHeight(BUTTON_HEIGHT);
		cancelButton = new TextEntryButton("Cancel");
		cancelButton.setX(x + okButton.getWidth() + 5).setY(y).setHeight(BUTTON_HEIGHT);

		ScrollingMenuSign plugin = ScrollingMenuSign.getInstance();
		this.attachWidget(plugin, label);
		this.attachWidget(plugin, textField);
		this.attachWidget(plugin, okButton);
		this.attachWidget(plugin, cancelButton);
	}

	public void setPasswordField(boolean isPassword) {
		textField.setPasswordField(isPassword);
	}

	private void setPrompt(String prompt) {
		label.setText(LABEL_COLOUR + prompt);
		textField.setText("");
	}

	public void confirm() {
		ScrollingMenuSign plugin = ScrollingMenuSign.getInstance();

		if (plugin.responseHandler.isExpecting(sp.getName(), ExpectCommandSubstitution.class)) {
			try {
				ExpectCommandSubstitution cs = plugin.responseHandler.getAction(sp.getName(), ExpectCommandSubstitution.class);
				cs.setSub(textField.getText());
				cs.handleAction();
			} catch (DHUtilsException e) {
				MiscUtil.errorMessage(sp, e.getMessage());
			}
		}

		close();
		visiblePopups.remove(sp.getName());
	}

	private void cancel() {
		ScrollingMenuSign.getInstance().responseHandler.cancelAction(sp.getName(), ExpectCommandSubstitution.class);

		close();
		visiblePopups.remove(sp.getName());
	}

	public static void show(SpoutPlayer sp, String prompt) {
		TextEntryPopup popup;
		String name = sp.getName();
		if (!allPopups.containsKey(name)) {
			popup = new TextEntryPopup(sp, prompt);
			allPopups.put(sp.getName(), popup);
		} else {
			popup = allPopups.get(name);
			popup.setPrompt(prompt);
		}

		ScrollingMenuSign plugin = ScrollingMenuSign.getInstance();
		if (plugin.responseHandler.isExpecting(sp.getName(), ExpectCommandSubstitution.class)) {
			ExpectCommandSubstitution cs = plugin.responseHandler.getAction(sp.getName(), ExpectCommandSubstitution.class);
			popup.setPasswordField(cs.isPassword());
		}

		sp.getMainScreen().attachPopupScreen(popup);
		visiblePopups.add(name);
	}

	public static boolean hasActivePopup(String playerName) {
		return visiblePopups.contains(playerName);
	}

	private class TextEntryButton extends GenericButton {
		TextEntryButton(String text) {
			super(text);
		}

		@Override
		public void onButtonClick(ButtonClickEvent event) {
			if (event.getButton() == okButton) {
				confirm();
			} else if (event.getButton() == cancelButton) {
				cancel();
			}
		}
	}
}
