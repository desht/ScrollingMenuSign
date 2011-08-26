package me.desht.scrollingmenusign.views.map;

import me.desht.scrollingmenusign.views.SMSMapView;
import me.desht.util.MiscUtil;

import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapFont;

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
        String[] words = MiscUtil.deColourise(text).split("\\s");
        
        StringBuilder lineBuffer = new StringBuilder();
        int lineWidth = 0;
 
        for (int i = 0; i < words.length; i++) {
        	System.out.println("word = " + words[i] + " font = " + font);
        	int wordWidth = font.getWidth(words[i]);
            if (wordWidth <= xLimit) {
                if (xPos + lineWidth + wordWidth <= xLimit) {
                	lineBuffer.append(words[i]).append(" ");
                    lineWidth = font.getWidth(lineBuffer.toString());
                } else {
                    canvas.drawText(xPos, yPos, font, lineBuffer.toString());
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
                        canvas.drawText(xPos, yPos, font, lineBuffer.toString());
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
            canvas.drawText(xPos, yPos, font, lineBuffer.toString());
            yPos += font.getHeight() + view.getLineSpacing();
        }
        return yPos;
    }
}
