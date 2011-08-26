package me.desht.scrollingmenusign.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;

import org.bukkit.Bukkit;
import org.bukkit.map.MapFont;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.map.MinecraftFont;
import org.bukkit.util.config.ConfigurationNode;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.scrollingmenusign.views.map.SMSMapRenderer;

/**
 * @author des
 * 
 * With thanks to dumptruckman for MapActionMenu, upon which much of this is based
 * 
 */
public class SMSMapView extends SMSScrollableView {

	private MapView mapView = null;
	private SMSMapRenderer mapRenderer = null;
	private MapFont mapFont = MinecraftFont.Font;
	private int x, y;
	private int width, height;
	private int lineSpacing;
	private List<MapRenderer> previousRenderers = new ArrayList<MapRenderer>();

	private static Map<Short,SMSMapView> allMapViews = new HashMap<Short, SMSMapView>();
	
	public SMSMapView (SMSMenu menu) {
		this(null, menu);
	}

	public SMSMapView(String name, SMSMenu menu) {
		super(name, menu);

		x = 4;
		y = 10;
		width = 120;
		height = 120;
		lineSpacing = 1;

		mapRenderer = new SMSMapRenderer(this);
	}

	@Override
	public Map<String, Object> freeze() {
		Map<String, Object> map = super.freeze();
		map.put("mapId", mapView == null ? -1 : mapView.getId());
		return map;
	}
	
	protected void thaw(ConfigurationNode node) {
		short mapId = (short) node.getInt("mapId", -1);
		if (mapId >= 0)
			setMapId((short) node.getInt("mapId", 0));
	}

	/**
	 * Associate this view with a map ID.  Removes (and saves) all renderers currently on
	 * the map, and adds our own SMSRenderer to the map.
	 *
	 * @param id
	 */
	public void setMapId(short id) {
		mapView = Bukkit.getServer().getMap(id);

		for (MapRenderer r : mapView.getRenderers()) {
			previousRenderers.add(r);
			mapView.removeRenderer(r);
		}
		mapView.addRenderer(getMapRenderer());
		
		allMapViews.put(mapView.getId(), this);
		
		autosave();
	}

	/* (non-Javadoc)
	 * @see me.desht.scrollingmenusign.views.SMSView#deletePermanent()
	 */
	@Override
	public void deletePermanent() {
		allMapViews.remove(mapView.getId());
		mapView.removeRenderer(getMapRenderer());
		for (MapRenderer r : previousRenderers) {
			mapView.addRenderer(r);
		}
		super.deletePermanent();
	}

	public MapView getMapView() {
		return mapView;
	}

	public SMSMapRenderer getMapRenderer() {
		return mapRenderer;
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getLineSpacing() {
		return lineSpacing;
	}

	public void setLineSpacing(int lineSpacing) {
		this.lineSpacing = lineSpacing;
	}

	public MapFont getMapFont() {
		return mapFont;
	}

	public void setMapFont(MapFont mapFont) {
		this.mapFont = mapFont;
	}

	@Override
	public void update(Observable menu, Object arg1) {
		if (mapView == null)
			return;

		if (mapView.getRenderers().contains(getMapRenderer())) {
			mapView.removeRenderer(getMapRenderer());
		}
		mapView.addRenderer(getMapRenderer());
		setDirty(true);
	}

	public String toString() {
		return "map id: " + mapView.getId();
	}

	public static SMSMapView getViewForId(short mapId) {
		return allMapViews.get(mapId);
	}

	public static boolean checkForMapId(short mapId) {
		return allMapViews.containsKey(mapId);
	}
	
	/**
	 * Convenience routine.  Add the given mapId as a view on the given menu.
	 * 
	 * @param mapId
	 * @param menu
	 * @return
	 * @throws SMSException if the given mapId is already a view
	 */
	public static SMSMapView addMapToMenu(short mapId, SMSMenu menu) throws SMSException {
		if (SMSMapView.checkForMapId(mapId))
			throw new SMSException("This map already has a menu view associated with it");
		
		SMSMapView mapView = new SMSMapView(menu);
		mapView.setMapId(mapId);
		mapView.update(menu, SMSMenuAction.REPAINT);
		
		return mapView;
	}
}
