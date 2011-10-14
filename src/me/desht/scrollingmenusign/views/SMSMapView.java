package me.desht.scrollingmenusign.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.map.MapFont;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.map.MinecraftFont;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.scrollingmenusign.spout.SpoutUtils;
import me.desht.scrollingmenusign.views.map.SMSMapRenderer;
import me.desht.util.MiscUtil;

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

	/**
	 * Create a new map view on the given menu.  The view name is chosen automatically.
	 * 
	 * @param menu	The menu to attach the new view to
	 */
	public SMSMapView (SMSMenu menu) {
		this(null, menu);
	}

	/**
	 * Create a new map view on the given menu.
	 * 
	 * @param name	The new view's name.
	 * @param menu	The menu to attach the new view to.
	 */
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

	protected void thaw(ConfigurationSection node) {
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
		if (mapView == null) {
			MiscUtil.log(Level.WARNING, "No such map view for map ID " + id);
			return;
		}

		for (MapRenderer r : mapView.getRenderers()) {
			previousRenderers.add(r);
			mapView.removeRenderer(r);
		}
		mapView.addRenderer(getMapRenderer());

		allMapViews.put(mapView.getId(), this);

		if (ScrollingMenuSign.getInstance().isSpoutEnabled())
			SpoutUtils.setSpoutMapName(mapView.getId(), getMenu().getTitle());

		autosave();
	}

	/* (non-Javadoc)
	 * @see me.desht.scrollingmenusign.views.SMSView#deletePermanent()
	 */
	@Override
	public void deletePermanent() {
		if (mapView != null) {
			if (ScrollingMenuSign.getInstance().isSpoutEnabled())
				SpoutUtils.setSpoutMapName(mapView.getId(), "map_" + mapView.getId());
			allMapViews.remove(mapView.getId());
			mapView.removeRenderer(getMapRenderer());
			for (MapRenderer r : previousRenderers) {
				mapView.addRenderer(r);
			}
		}
		super.deletePermanent();
	}

	/**
	 * Get the Bukkit @see org.bukkit.map.MapView associated with this map view object.
	 * 
	 * @return	The Bukkit MapView object
	 */
	public MapView getMapView() {
		return mapView;
	}

	/**
	 * Get the custom map renderer for this map view object.
	 * 
	 * @return	The SMSMapRenderer object
	 */
	public SMSMapRenderer getMapRenderer() {
		return mapRenderer;
	}

	/**
	 * Get the X co-ordinate to start drawing at - the left-hand bounds of the drawing space
	 * 
	 * @return	The X co-ordinate
	 */
	public int getX() {
		return x;
	}

	/**
	 * Set the X co-ordinate to start drawing at - the left-hand bounds of the drawing space
	 * 
	 * @param x	The X co-ordinate
	 */
	public void setX(int x) {
		this.x = x;
	}

	/**
	 * Get the Y co-ordinate to start drawing at - the upper bounds of the drawing space
	 * 
	 * @return	The Y co-ordinate
	 */
	public int getY() {
		return y;
	}

	/**
	 * Set the Y co-ordinate to start drawing at - the upper bounds of the drawing space
	 * 
	 * @param y		The Y co-ordinate
	 */
	public void setY(int y) {
		this.y = y;
	}

	/**
	 * Get the width of the drawing area on the map
	 * 
	 * @return	The width
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * Set the width of the drawing area on the map
	 * 
	 * @param width	The width
	 */
	public void setWidth(int width) {
		this.width = width;
	}

	/**
	 * Get the height of the drawing area on the map
	 * 
	 * @return	The height
	 */
	public int getHeight() {
		return height;
	}

	/**
	 * Set the height of the drawing area on the map
	 * 
	 * @param height	The height
	 */
	public void setHeight(int height) {
		this.height = height;
	}

	/**
	 * Get the pixel spacing between each line of text in the menu
	 * 
	 * @return	The spacing
	 */
	public int getLineSpacing() {
		return lineSpacing;
	}

	/**
	 * Set the pixel spacing between each line of text in the menu
	 * 
	 * @param lineSpacing	The spacing
	 */
	public void setLineSpacing(int lineSpacing) {
		this.lineSpacing = lineSpacing;
	}

	/**
	 * Get the font used for drawing menu text
	 * 
	 * @return	The font
	 */
	public MapFont getMapFont() {
		return mapFont;
	}

	/**
	 * Set the font used for drawing menu text
	 * 
	 * @param mapFont	The font
	 */
	public void setMapFont(MapFont mapFont) {
		this.mapFont = mapFont;
	}

	/* (non-Javadoc)
	 * @see me.desht.scrollingmenusign.views.SMSScrollableView#update(java.util.Observable, java.lang.Object)
	 */
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

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "map id: " + (mapView == null ? "NONE" : mapView.getId());
	}

	/**
	 * Given a map ID, return the map view object for that ID, if any.
	 * 
	 * @param mapId	The ID of the map
	 * @return	The SMSMapView object for the ID, or null if this map ID isn't used for a SMSMapView
	 */
	public static SMSMapView getViewForId(short mapId) {
		return allMapViews.get(mapId);
	}

	/**
	 * Check if the given map ID is used for a SMSMapView
	 * 
	 * @param mapId	The ID of the map
	 * @return	true if the ID is used for a SMSMapView, false otherwise
	 */
	public static boolean checkForMapId(short mapId) {
		return allMapViews.containsKey(mapId);
	}

	/**
	 * Convenience routine.  Add the given mapId as a view on the given menu.
	 * 
	 * @param mapId
	 * @param menu
	 * @return	The SMSMapView object that was just created
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

	@Override
	public String getType() {
		return "map";
	}
}
