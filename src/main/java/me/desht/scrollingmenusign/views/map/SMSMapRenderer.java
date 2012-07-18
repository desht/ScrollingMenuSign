package me.desht.scrollingmenusign.views.map;

import java.awt.image.BufferedImage;

import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSMenuItem;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.enums.ViewJustification;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.PermissionUtils;
import me.desht.scrollingmenusign.views.SMSMapView;

import org.bukkit.ChatColor;
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
	private static final String[] NOT_OWNER = { "\u00a7oThis map belongs", "\u00a7to someone else." };
	private static final String[] NO_PERM = { "\u00a7oYou do not have", "\u00a7permission to use", "\u00a7map menus." };

	private final SMSMapView smsMapView;
	
	public SMSMapRenderer(SMSMapView view) {
		super(true);
		smsMapView = view;
	}

	@Override
	public void render(MapView map, MapCanvas canvas, Player player) {
		if (smsMapView.isDirty(player.getName())) {
			drawImage(canvas, smsMapView.getImage());
			if (!smsMapView.hasOwnerPermission(player)) {
				drawMessage(canvas, NOT_OWNER);
			} else if (!PermissionUtils.isAllowedTo(player, "scrollingmenusign.use.map")) {
				drawMessage(canvas, NO_PERM);
			} else {
				drawMenu(canvas, player);
			}
			smsMapView.setDirty(player.getName(), false);
			player.sendMap(map);
		}
	}

	private void drawImage(MapCanvas canvas, BufferedImage image) {
		LogUtils.finer(smsMapView.getName() + ": draw background image: " + image);
		if (image != null) {
			canvas.drawImage(0, 0, image);
		}
	}

	private void drawMenu(MapCanvas canvas, Player player) {
		int y = smsMapView.getY();

		SMSMenu menu = smsMapView.getMenu();

		if (ScrollingMenuSign.getInstance().isSpoutEnabled()) {
			// If the player is using Spoutcraft, then the menu title is already there,
			// as the name of the map item (renamed in the SMSMapView setup phase), so we
			// don't need to draw it again.
			SpoutPlayer sPlayer = (SpoutPlayer) player;
			if (sPlayer.isSpoutCraftEnabled()) { 
				// using spoutcraft
				drawText(canvas, ViewJustification.RIGHT, 0, smsMapView.getMapFont(),
				         ChatColor.GRAY.toString() + ChatColor.ITALIC.toString() + "#" + Short.toString(smsMapView.getMapView().getId()));
			} else {
				// no spoutcraft - draw title as normal
				String title = menu.getTitle();
				drawText(canvas, smsMapView.getTitleJustification(), y, smsMapView.getMapFont(), title);
				y += smsMapView.getMapFont().getHeight() + smsMapView.getLineSpacing();
			}
		}

		String prefix1 = ScrollingMenuSign.getInstance().getConfig().getString("sms.item_prefix.not_selected", "  ");
		String prefix2 = ScrollingMenuSign.getInstance().getConfig().getString("sms.item_prefix.selected", "> ");

		int nDisplayable = (smsMapView.getHeight() - y) / (smsMapView.getMapFont().getHeight() + smsMapView.getLineSpacing());

		if (menu.getItemCount() > 0) {
			int current = smsMapView.getScrollPos(player.getName());
			ViewJustification itemJust = smsMapView.getItemJustification();
			for (int n = 0; n < nDisplayable; n++) {
				SMSMenuItem item = menu.getItemAt(current);
				String lineText = item == null ? "???" : item.getLabel();
				if (n == 0) {
					lineText = prefix2 + lineText;
				} else {
					lineText = prefix1 + lineText;
				}
				drawText(canvas, itemJust, y, smsMapView.getMapFont(), lineText);
				y += smsMapView.getMapFont().getHeight() + smsMapView.getLineSpacing();
				current++;
				if (current > menu.getItemCount())
					current = 1;
				if (n + 1 >= menu.getItemCount())
					break;
			}
		}
	}

	private int getXOffset(ViewJustification just, int width) {
		switch (just) {
		case LEFT:
			return smsMapView.getX();
		case CENTER:
			return smsMapView.getX() + (smsMapView.getWidth() - width) / 2;
		case RIGHT:
			return smsMapView.getX() + smsMapView.getWidth() - width;
		default:
			return 0;
		}
	}

	private void drawMessage(MapCanvas canvas, String[] text) {
		MapFont font = smsMapView.getMapFont();
		int h = font.getHeight() + smsMapView.getLineSpacing();
		int y = smsMapView.getY() + (smsMapView.getHeight() - h * text.length) / 2;
		for (String s : text)	 {
			int x = smsMapView.getX() + (smsMapView.getWidth()  - getWidth(font, s)) / 2;
			drawText(canvas, x, y, font, s);
			y += h;
		}
	}

	private void drawText(MapCanvas canvas, ViewJustification just, int y, MapFont font, String text) {
		int textWidth = getWidth(smsMapView.getMapFont(), text);
		drawText(canvas, getXOffset(just, textWidth), y, font, text);
	}

	private void drawText(MapCanvas canvas, int x, int y, MapFont font, String text) {
		if (!font.isValid(text)) {
			throw new IllegalArgumentException("text contains invalid characters");
		}
		int xStart = x;
		byte color = convertMcToPalette((byte)0);
		boolean bold = false, italic = false, strike = false, underline = false;
		
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
				Character c = Character.toLowerCase(text.charAt(i));
				if (c >= '0' && c <= '9' || c >= 'a' && c <= 'f') {
					byte mcColor = Byte.parseByte(c.toString(), 16);
					color = convertMcToPalette(mcColor);
					bold = italic = strike = underline = false;
				} else if (c == 'l') {
					bold = !bold;
				} else if (c == 'm') {
					strike = !strike;
				} else if (c == 'n') {
					underline = !underline;
				} else if (c == 'o') {
					italic = !italic;
				} else if (c == 'r') {
					bold = italic = strike = underline = false;
					color = convertMcToPalette((byte)0);
				}
				continue;
			}

			CharacterSprite sprite = font.getChar(text.charAt(i));
			for (int r = 0; r < font.getHeight(); ++r) {
				for (int c = 0; c < sprite.getWidth(); ++c) {
					int sx = x + c;
					if (sprite.get(r, c)) {
						if (italic) {
							if (r < font.getHeight() / 2) {
								sx = x + c + 1;
							} else {
								sx = x + c;
							}
						}
						canvas.setPixel(sx, y + r, color);
						if (bold) {
							canvas.setPixel(sx + 1, y + r, color);
						}
					}
				}
			}
			if (strike) {
				for (int c = 0; c <= sprite.getWidth(); c++) {
					canvas.setPixel(x + c, y + font.getHeight() / 2 - 1, color);
				}
			}
			if (underline) {
				for (int c = 0; c <= sprite.getWidth(); c++) {
					canvas.setPixel(x + c, y + font.getHeight() - 1, color);
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
		return font.getWidth(text.replaceAll("\u00A7.", "")) + text.length();
	}
}
