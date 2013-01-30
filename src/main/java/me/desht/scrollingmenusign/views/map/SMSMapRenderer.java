package me.desht.scrollingmenusign.views.map;

import java.awt.image.BufferedImage;

import me.desht.scrollingmenusign.views.SMSMapView;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

/**
 * @author des
 * 
 */
public class SMSMapRenderer extends MapRenderer {
	private final SMSMapView smsMapView;

	public SMSMapRenderer(SMSMapView view) {
		super(true);
		smsMapView = view;
	}
	
	@Override
	public void render(MapView map, MapCanvas canvas, Player player) {
		if (smsMapView.isDirty(player.getName())) {
			BufferedImage img = smsMapView.renderImage(player);
			canvas.drawImage(0, 0, img);
			smsMapView.setDirty(player.getName(), false);
			player.sendMap(map);
		}
	}
}
