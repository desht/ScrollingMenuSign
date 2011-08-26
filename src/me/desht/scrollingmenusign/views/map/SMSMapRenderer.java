package me.desht.scrollingmenusign.views.map;

import me.desht.scrollingmenusign.SMSConfig;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSMenuItem;
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

	SMSMapView smsMapView;
	
	public SMSMapRenderer(SMSMapView view) {
		smsMapView = view;
	}
	
	@Override
	public void render(MapView map, MapCanvas canvas, Player player) {
		if (smsMapView.isDirty()) {
            System.out.println("rendering " + smsMapView.getMenu().getName() + " on map_" + map.getId());
            int y = smsMapView.getY();
            
            SMSMenu menu = smsMapView.getMenu();
            
            String title = menu.getTitle();
            y = MapUtil.writeLines(smsMapView, canvas, smsMapView.getX(), y, smsMapView.getMapFont(), title);

            String prefix1 = SMSConfig.getConfiguration().getString("sms.item_prefix.not_selected", "  ");
    		String prefix2 = SMSConfig.getConfiguration().getString("sms.item_prefix.selected", "> ");
    		
    		int n = 1;
            for (SMSMenuItem item : menu.getItems()) {
            	String lineText = item.getLabel();
            	if (n == smsMapView.getScrollPos()) {
            		lineText = prefix2 + lineText;
            	} else {
            		lineText = prefix1 + lineText;
            	}
            	y = MapUtil.writeLines(smsMapView, canvas, smsMapView.getX(), y, smsMapView.getMapFont(), lineText);
            	if (y >= smsMapView.getHeight())
            		break;
            	n++;
            }

            smsMapView.setDirty(false);
            player.sendMap(map);
        }
	}

}
