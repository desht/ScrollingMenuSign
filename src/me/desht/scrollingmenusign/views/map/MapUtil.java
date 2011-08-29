package me.desht.scrollingmenusign.views.map;

import me.desht.scrollingmenusign.views.SMSMapView;
import me.desht.util.MiscUtil;

import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapFont;
import org.bukkit.map.MapFont.CharacterSprite;
import org.bukkit.map.MapPalette;

/**
 * @author dumptruckman
 * 
 * Adapted to use StringBuilder by desht
 *
 */
public class MapUtil {
    protected static int writeLines(SMSMapView view, MapCanvas canvas, int x, int y, MapFont font, String text) {
        int xPos = x;
        int yPos = y;
        int xLimit = view.getWidth() - x;

        // Bukkit Map routines throw NPE's if you pass colour codes...
        
//        String[] words = MiscUtil.deColourise(text).split("\\s");
        String[] words = text.split("\\s");
        StringBuilder lineBuffer = new StringBuilder();
        int lineWidth = 0;
 
        for (int i = 0; i < words.length; i++) {
        	words[i] = words[i].replaceAll("\u00A7(.)", "\u00A7$1;");
        	System.out.println("word = " + words[i] + " font = " + font);
        	int wordWidth = font.getWidth(words[i]);
            if (wordWidth <= xLimit) {
                if (xPos + lineWidth + wordWidth <= xLimit) {
                	lineBuffer.append(words[i]).append(" ");
                    lineWidth = font.getWidth(lineBuffer.toString());
                } else {
//                    canvas.drawText(xPos, yPos, font, lineBuffer.toString());
                	drawText(canvas, xPos, yPos, font, lineBuffer.toString());
                    lineBuffer.setLength(0);
                    lineWidth = 0;
                    yPos += font.getHeight() + view.getLineSpacing();
                    i--;
                    continue;
                }
            } else {
                char[] chars = words[i].toCharArray();
                for (int j = 0; j < chars.length; j++) {
                    String sChar = Character.toString(chars[j]);
                    int charWidth = font.getWidth(sChar);
                    if (xPos + lineWidth + charWidth < xLimit) {
                    	lineBuffer.append(sChar);
                        lineWidth = font.getWidth(lineBuffer.toString());
                    } else {
                    	drawText(canvas, xPos, yPos, font, lineBuffer.toString());
//                        canvas.drawText(xPos, yPos, font, lineBuffer.toString());
                        lineBuffer.setLength(0);
                        lineWidth = 0;
                        yPos += font.getHeight() + view.getLineSpacing();
                        j--;
                        continue;
                    }
                }
                if (lineWidth != 0) {
                    lineBuffer.append(" ");
                    lineWidth = font.getWidth(lineBuffer.toString());
                }
            }
        }
        if (lineWidth != 0) {
//            canvas.drawText(xPos, yPos, font, lineBuffer.toString());
            drawText(canvas, xPos, yPos, font, lineBuffer.toString());
            yPos += font.getHeight() + view.getLineSpacing();
        }
        return yPos;
    }
    
    private static void drawText(MapCanvas canvas, int x, int y, MapFont font, String text) {
        int xStart = x;
        byte color = MapPalette.DARK_GRAY;
        if (!font.isValid(text)) {
            throw new IllegalArgumentException("text contains invalid characters");
        }

        for (int i = 0; i < text.length(); ++i) {
            char ch = text.charAt(i);
            if (ch == '\n') {
                x = xStart;
                y += font.getHeight() + 1;
                continue;
            } else if (ch == '\u00A7') {
            	i++;
            	if (i >= text.length())
            		break;
            	color = Byte.parseByte(text.substring(i, i + 1), 16);
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

}
