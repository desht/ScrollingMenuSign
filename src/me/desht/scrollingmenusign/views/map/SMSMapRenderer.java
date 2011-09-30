package me.desht.scrollingmenusign.views.map;

import me.desht.scrollingmenusign.SMSConfig;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.views.SMSMapView;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapFont;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.map.MapFont.CharacterSprite;
import org.getspout.spoutapi.player.SpoutPlayer;

/**
 * @author des
 * 
 */
public class SMSMapRenderer extends MapRenderer {
	private static final String[] DENIED_TEXT = { "This map belongs", "to someone else." };

	SMSMapView smsMapView;

	public SMSMapRenderer(SMSMapView view) {
		smsMapView = view;
	}

	@Override
	public void render(MapView map, MapCanvas canvas, Player player) {
		if (smsMapView.isDirty()) {
			if (!smsMapView.allowedToUse(player)) {
				drawDeniedMessage(canvas);
			} else {
				drawMenu(canvas, player);
			}
			smsMapView.setDirty(false);
			player.sendMap(map);
		}
	}

	private void drawMenu(MapCanvas canvas, Player player) {
		// System.out.println("rendering " + smsMapView.getMenu().getName() + " on map_" + map.getId());
		int y = smsMapView.getY();

		SMSMenu menu = smsMapView.getMenu();

		// If the player is using Spoutcraft, then the menu title is already there,
		// as the name of the map item (renamed in the SMSMapView setup phase), so we
		// don't need to draw it again.
		SpoutPlayer sPlayer = (SpoutPlayer) player;
		if (!sPlayer.isSpoutCraftEnabled()) { 
			String title = menu.getTitle();
			int titleWidth = getWidth(smsMapView.getMapFont(), title);
			drawText(canvas, smsMapView.getX() + (smsMapView.getWidth() - titleWidth) / 2, y, smsMapView.getMapFont(), title);
			y += smsMapView.getMapFont().getHeight() + smsMapView.getLineSpacing();
		}

		String prefix1 = SMSConfig.getConfiguration().getString("sms.item_prefix.not_selected", "  ");
		String prefix2 = SMSConfig.getConfiguration().getString("sms.item_prefix.selected", "> ");

		int nDisplayable = (smsMapView.getHeight() - y) / (smsMapView.getMapFont().getHeight() + smsMapView.getLineSpacing());

		if (menu.getItemCount() > 0) {
			int current = smsMapView.getScrollPos();
			for (int n = 0; n < nDisplayable; n++) {
				String lineText = menu.getItem(current).getLabel();
				if (n == 0) {
					lineText = prefix2 + lineText;
				} else {
					lineText = prefix1 + lineText;
				}
				drawText(canvas, smsMapView.getX(), y, smsMapView.getMapFont(), lineText);
				y += smsMapView.getMapFont().getHeight() + smsMapView.getLineSpacing();
				current++;
				if (current > menu.getItemCount())
					current = 1;
				if (n + 1 >= menu.getItemCount())
					break;
			}
		}
	}

	private void drawDeniedMessage(MapCanvas canvas) {
		MapFont font = smsMapView.getMapFont();
		int h = font.getHeight() + smsMapView.getLineSpacing();
		int y = smsMapView.getY() + (smsMapView.getHeight() - h * DENIED_TEXT.length) / 2;
		for (String s : DENIED_TEXT)	 {
			int x = smsMapView.getX() + (smsMapView.getWidth()  - getWidth(font, s)) / 2;
			drawText(canvas, x, y, font, s);
			y += h;
		}
	}

	static void drawText(MapCanvas canvas, int x, int y, MapFont font, String text) {
		int xStart = x;
		byte color = MapPalette.DARK_GRAY;
		if (!font.isValid(text)) {
			throw new IllegalArgumentException("text contains invalid characters");
		}

		for (int i = 0; i < text.length(); i++) {
			char ch = text.charAt(i);
			if (ch == '\n') {
				x = xStart;
				y += font.getHeight() + 1;
				continue;
			} else if (ch == '\u00A7') {
				i++;
				if (i >= text.length())
					break;
				byte mcColor = Byte.parseByte(text.substring(i, i + 1), 16);
				color = convertMcToPalette(mcColor);
				i++;
			}

			CharacterSprite sprite = font.getChar(text.charAt(i));
			for (int r = 0; r < font.getHeight(); ++r) {
				for (int c = 0; c < sprite.getWidth(); ++c) {
					if (sprite.get(r, c)) {
						canvas.setPixel(x + c, y + r, color);
					}
				}
			}
			x += sprite.getWidth() + 1;
		}
	}

	private static final byte[] colorCache = new byte[16];
	private static final boolean[] colorInited = new boolean[16];

	private static byte convertMcToPalette(byte mcColor) {

		if (colorInited[mcColor]) {
			return colorCache[mcColor];
		}

		byte result = MapPalette.DARK_GRAY;

		switch (mcColor) {
		case 0:
			result = MapPalette.matchColor(0, 0, 0); break;
		case 1:
			result = MapPalette.matchColor(0, 0, 128); break;
		case 2:
			result = MapPalette.matchColor(0, 128, 0); break;
		case 3:
			result = MapPalette.matchColor(0, 128, 128); break;
		case 4:
			result = MapPalette.matchColor(128, 0, 0); break;
		case 5:
			result = MapPalette.matchColor(128, 0, 128); break;
		case 6:
			result = MapPalette.matchColor(128, 128, 0); break;
		case 7:
			result = MapPalette.matchColor(128, 128, 128); break;
		case 8:
			result = MapPalette.matchColor(64, 64, 64); break;
		case 9:
			result = MapPalette.matchColor(0, 0, 255); break;
		case 10:
			result = MapPalette.matchColor(0, 255, 0); break;
		case 11:
			result = MapPalette.matchColor(0, 255, 255); break;
		case 12:
			result = MapPalette.matchColor(255, 0, 0); break;
		case 13:
			result = MapPalette.matchColor(255, 0, 255); break;
		case 14:
			result = MapPalette.matchColor(255, 255, 0); break;
		case 15:
			result = MapPalette.matchColor(255, 255, 255); break;
		}

		colorInited[mcColor] = true;
		colorCache[mcColor] = result;

		return result;
	}

	static int getWidth(MapFont font, String text) {
		return font.getWidth(text.replaceAll("\u00A7.", ""));
	}
}
