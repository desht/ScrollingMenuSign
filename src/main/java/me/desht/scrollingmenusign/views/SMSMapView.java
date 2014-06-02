package me.desht.scrollingmenusign.views;

import me.desht.dhutils.ConfigurationManager;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MapUtil;
import me.desht.dhutils.PermissionUtils;
import me.desht.scrollingmenusign.*;
import me.desht.scrollingmenusign.enums.ViewJustification;
import me.desht.scrollingmenusign.views.action.ViewUpdateAction;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Observable;

/**
 * This view draws menus on maps.  With thanks to dumptruckman for MapActionMenu, which provided inspiration for this.
 */
public class SMSMapView extends SMSScrollableView {

    private static final String CACHED_FILE_FORMAT = "png";

    // attributes
    public static final String IMAGE_FILE = "imagefile";
    public static final String BACKGROUND = "background";
    public static final String FONT = "font";
    public static final String FONT_SIZE = "fontsize";

    private static final Color DEFAULT_COLOR = new Color(0, 0, 0);

    private MapView mapView = null;
    private final SMSMapRenderer mapRenderer;
    private int x, y;
    private int width, height;
    private int lineSpacing;
    private final List<MapRenderer> previousRenderers = new ArrayList<MapRenderer>();
    private BufferedImage backgroundImage = null;

    private static BufferedImage deniedImage = null;

    /**
     * Create a new map view on the given menu.  The view name is chosen automatically.
     *
     * @param menu The menu to attach the new view to
     */
    public SMSMapView(SMSMenu menu) {
        this(null, menu);
    }

    /**
     * Create a new map view on the given menu.
     *
     * @param name The new view's name.
     * @param menu The menu to attach the new view to.
     */
    public SMSMapView(String name, SMSMenu menu) {
        super(name, menu);

        Configuration config = ScrollingMenuSign.getInstance().getConfig();
        registerAttribute(IMAGE_FILE, "", "Image to use as map background");
        registerAttribute(FONT, config.getString("sms.maps.font"), "Java font for map text drawing");
        registerAttribute(FONT_SIZE, config.getInt("sms.maps.fontsize"), "Font size for map text drawing");
        registerAttribute(BACKGROUND, config.getInt("sms.maps.background"), "Background fill colour");

        x = 4;
        y = 0;
        width = 120;
        height = 128;
        lineSpacing = 0;

        mapRenderer = new SMSMapRenderer(this);
    }

    @Override
    protected boolean isTypeUsable(Player player) {
        SMSMapView mv = ScrollingMenuSign.getInstance().getViewManager().getHeldMapView(player);
        if (mv != null && mv.getMapView().getId() == mapView.getId()) {
            // player is holding this map
            return super.isTypeUsable(player);
        } else {
            // not holding this map? must be in an item frame
            return PermissionUtils.isAllowedTo(player, "scrollingmenusign.use.map.framed");
        }
    }

    private BufferedImage getDeniedImage() {
        if (deniedImage == null) {
            URL u = getClass().getResource("/denied.png");
            if (u != null) {
                try {
                    deniedImage = ImageIO.read(u);
                } catch (IOException e) {
                    LogUtils.warning("error loading access-denied image: " + e.getMessage());
                }
            } else {
                LogUtils.warning("missing resource for access-denied image: " + u);
            }
        }
        return deniedImage;
    }

    private void loadBackgroundImage() {
        backgroundImage = null;

        String file = getAttributeAsString(IMAGE_FILE, "");
        if (file.isEmpty()) {
            return;
        }

        // Load the file from the given URL, and write a cached copy (PNG, 128x128) to our local
        // directory structure.  The cached file can be used for subsequent loads to improve performance.
        try {
            URL url = ScrollingMenuSign.makeImageURL(file);
            File cached = getCachedFile(url);
            BufferedImage resizedImage;
            if (cached != null && cached.canRead()) {
                resizedImage = ImageIO.read(cached);
            } else {
                BufferedImage orig = ImageIO.read(url);
                BufferedImage result = MapUtil.createMapBuffer();
                Graphics2D graphics = result.createGraphics();
                graphics.drawImage(orig, 0, 0, 128, 128, null);
                graphics.dispose();
                resizedImage = MapPalette.resizeImage(orig);
                if (cached != null) {
                    ImageIO.write(resizedImage, CACHED_FILE_FORMAT, cached);
                    LogUtils.info("Cached image " + url + " as " + cached);
                }
            }
            backgroundImage = resizedImage;
        } catch (MalformedURLException e) {
            LogUtils.warning("malformed image URL for map view " + getName() + ": " + e.getMessage());
        } catch (IOException e) {
            LogUtils.warning("cannot load image URL for map view " + getName() + ": " + e.getMessage());
        }
    }

    private static File getCachedFile(URL url) {
        byte[] bytes = url.toString().getBytes();
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
            byte[] d = md.digest(bytes);
            BigInteger i = new BigInteger(d);
            return new File(DirectoryStructure.getImgCacheFolder(), String.format("%1$032X", i) + "." + CACHED_FILE_FORMAT);
        } catch (NoSuchAlgorithmException e) {
            LogUtils.warning("Can't get MD5 MessageDigest algorithm, no image caching");
            return null;
        }
    }

    @Override
    public Map<String, Object> freeze() {
        Map<String, Object> map = super.freeze();
        map.put("mapId", mapView == null ? -1 : mapView.getId());
        return map;
    }

    protected void thaw(ConfigurationSection node) throws SMSException {
        super.thaw(node);
        short mapId = (short) node.getInt("mapId", -1);
        if (mapId >= 0)
            setMapId((short) node.getInt("mapId", 0));
    }

    /**
     * Associate this view with a map ID.  Removes (and saves) all renderers currently on
     * the map, and adds our own SMSRenderer to the map.
     *
     * @param id the numeric map ID
     */
    public void setMapId(short id) {
        mapView = Bukkit.getServer().getMap(id);
        if (mapView == null) {
            // This could happen if the Minecraft map data has been lost.  Perhaps the SMS view files have
            // been migrated to a new server?  Anyway, we can get a new Bukkit MapView for this SMS view.
            mapView = Bukkit.getServer().createMap(Bukkit.getServer().getWorlds().get(0));
            LogUtils.warning("View " + getName() + ": missing Bukkit MapView for map ID " + id + " - created new Bukkit MapView with ID " + mapView.getId());
        }

        for (MapRenderer r : mapView.getRenderers()) {
            previousRenderers.add(r);
            mapView.removeRenderer(r);
        }
        mapView.addRenderer(getMapRenderer());

        loadBackgroundImage();

        autosave();
    }

    /**
     * Get the Bukkit @see org.bukkit.map.MapView associated with this map view object.
     *
     * @return The Bukkit MapView object
     */
    public MapView getMapView() {
        return mapView;
    }

    /**
     * Get the custom map renderer for this map view object.
     *
     * @return The SMSMapRenderer object
     */
    public SMSMapRenderer getMapRenderer() {
        return mapRenderer;
    }

    /**
     * Get the X co-ordinate to start drawing at - the left-hand bounds of the drawing space
     *
     * @return The X co-ordinate
     */
    public int getX() {
        return x;
    }

    /**
     * Set the X co-ordinate to start drawing at - the left-hand bounds of the drawing space
     *
     * @param x The X co-ordinate
     */
    public void setX(int x) {
        this.x = x;
    }

    /**
     * Get the Y co-ordinate to start drawing at - the upper bounds of the drawing space
     *
     * @return The Y co-ordinate
     */
    public int getY() {
        return y;
    }

    /**
     * Set the Y co-ordinate to start drawing at - the upper bounds of the drawing space
     *
     * @param y The Y co-ordinate
     */
    public void setY(int y) {
        this.y = y;
    }

    /**
     * Get the width of the drawing area on the map
     *
     * @return The width
     */
    public int getWidth() {
        return width;
    }

    /**
     * Set the width of the drawing area on the map
     *
     * @param width The width
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * Get the height of the drawing area on the map
     *
     * @return The height
     */
    public int getHeight() {
        return height;
    }

    /**
     * Set the height of the drawing area on the map
     *
     * @param height The height
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * Get the pixel spacing between each line of text in the menu
     *
     * @return The spacing
     */
    public int getLineSpacing() {
        return lineSpacing;
    }

    /**
     * Set the pixel spacing between each line of text in the menu
     *
     * @param lineSpacing The spacing
     */
    public void setLineSpacing(int lineSpacing) {
        this.lineSpacing = lineSpacing;
    }

    public BufferedImage getImage() {
        return backgroundImage;
    }

    /**
     * Apply an item name & lore for this map view to the given item (which should be a map!)
     *
     * @param item the map item
     */
    public void setMapItemName(ItemStack item) {
        int nItems = getNativeMenu().getItemCount();
        String loreStr = nItems + (nItems == 1 ? " menu item" : " menu items");
        loreStr = ChatColor.GRAY.toString() + ChatColor.ITALIC.toString() + loreStr;
        List<String> lore = new ArrayList<String>(1);
        lore.add(loreStr);
        ItemMeta im = item.getItemMeta();
        im.setDisplayName(ChatColor.RESET + doVariableSubstitutions(null, getNativeMenu().getTitle()));
        im.setLore(lore);
        item.setItemMeta(im);
    }

    /**
     * Remove any custom item name & lore from the given item.
     *
     * @param item the map item
     */
    public void removeMapItemName(ItemStack item) {
        if (item.getType() != Material.MAP || getMapView().getId() != item.getDurability()) {
            LogUtils.warning("SMSMapView: Attempt to remove item name from non map-view item!");
            return;
        }
        ItemMeta im = item.getItemMeta();
        im.setDisplayName(null);
        im.setLore(null);
        item.setItemMeta(im);
    }

    /* (non-Javadoc)
     * @see me.desht.scrollingmenusign.views.SMSScrollableView#update(java.util.Observable, java.lang.Object)
     */
    @Override
    public void update(Observable menu, Object arg1) {
        super.update(menu, arg1);

        ViewUpdateAction vu = ViewUpdateAction.getAction(arg1);
        if (mapView != null) {
            if (mapView.getRenderers().contains(getMapRenderer())) {
                mapView.removeRenderer(getMapRenderer());
            }
            mapView.addRenderer(getMapRenderer());
            setDirty(vu.getSender() instanceof Player ? (Player) vu.getSender() : null, true);
        }
    }

    @Override
    public void onDeleted(boolean permanent) {
        super.onDeleted(permanent);
        if (permanent && mapView != null) {
            mapView.removeRenderer(getMapRenderer());
            for (MapRenderer r : previousRenderers) {
                mapView.addRenderer(r);
            }
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "map id: " + (mapView == null ? "NONE" : mapView.getId());
    }


    @Override
    public String getType() {
        return "map";
    }

    /* (non-Javadoc)
     * @see me.desht.scrollingmenusign.views.SMSView#onConfigurationChanged(me.desht.dhutils.ConfigurationManager, java.lang.String, java.lang.Object, java.lang.Object)
     */
    @Override
    public void onConfigurationChanged(ConfigurationManager configurationManager, String attribute, Object oldVal, Object newVal) {
        super.onConfigurationChanged(configurationManager, attribute, oldVal, newVal);

        if (attribute.equals(IMAGE_FILE)) {
            loadBackgroundImage();
            setDirty(true);
        }
    }


    @Override
    protected int getHardMaxTitleLines() {
        return 4;
    }

    @Override
    protected int getLineLength() {
        return 30;    // estimate
    }

    /**
     * This method does the actual creation of the map image, to be returned to the Bukkit
     * map renderer for copying to the canvas.
     *
     * @return an Image
     */
    public BufferedImage renderImage(Player player) {
        if (mapView == null) {
            return null;
        }

        BufferedImage result = backgroundImage == null ? MapUtil.createMapBuffer() : deepCopy(backgroundImage);

        Graphics g = result.getGraphics();
        g.setFont(new Font(getAttributeAsString(FONT), 0, (Integer) getAttribute(FONT_SIZE)));
        if (backgroundImage == null) {
            g.setColor(MapUtil.getMapColor((Integer) getAttribute(BACKGROUND)));
            g.fillRect(0, 0, 128, 128);
        }
        g.setColor(DEFAULT_COLOR);

        FontMetrics metrics = g.getFontMetrics();
        SMSMenu menu = getActiveMenu(player);
        Configuration config = ScrollingMenuSign.getInstance().getConfig();

        if (!hasOwnerPermission(player) || !isTypeUsable(player)) {
            BufferedImage di = getDeniedImage();
            if (di != null) {
                g.drawImage(di, 0, 0, null);
            } else {
                drawMessage(g, new String[]{"Access Denied"});
            }
            return result;
        }

        int lineHeight = metrics.getHeight() + getLineSpacing();
        int yPos = getY() + lineHeight;

        // draw the title line(s)
        List<String> titleLines = splitTitle(player);
        for (String line : titleLines) {
            drawText(g, getTitleJustification(), yPos, line);
            yPos += lineHeight;
        }
        Color c = g.getColor();
        g.setColor(MapUtil.getChatColor(7));
        yPos++;
        int lineY = yPos + 1 - lineHeight;
        g.drawLine(x, lineY, x + width, lineY);
        g.setColor(c);

        String prefixNotSel = config.getString("sms.item_prefix.not_selected", "  ");
        String prefixSel = config.getString("sms.item_prefix.selected", "> ");
        ViewJustification itemJust = getItemJustification();
        int pageSize = (getHeight() - yPos) / (metrics.getHeight() + getLineSpacing());
        int scrollPos = getScrollPos(player);
        int menuSize = getActiveMenuItemCount(player);
        // draw the text
        switch (getScrollType()) {
            case SCROLL:
                for (int n = 0; n < pageSize && n < menuSize; n++, yPos += lineHeight) {
                    String prefix = n == 0 ? prefixSel : prefixNotSel;
                    String lineText = getActiveItemLabel(player, scrollPos);
                    drawText(g, itemJust, yPos, prefix + lineText);
                    if (++scrollPos > menuSize) {
                        scrollPos = 1;
                    }
                }
                break;
            case PAGE:
                int pageNum = (scrollPos - 1) / pageSize;
                for (int j = 0, pos = (pageNum * pageSize) + 1; j < pageSize && pos <= menuSize; j++, pos++, yPos += lineHeight) {
                    String pre = pos == scrollPos ? prefixSel : prefixNotSel;
                    String lineText = getActiveItemLabel(player, pos);
                    drawText(g, itemJust, yPos, pre + lineText);
                }
                break;
        }

        // draw the tooltip, if needed
        SMSMenuItem item = menu.getItemAt(getScrollPos(player));
        if (item != null && item.hasPermission(player) && config.getBoolean("sms.maps.show_tooltips")) {
//            String[] lore = item.getLore();
            List<String> lore = doVariableSubstitutions(player, item.getLoreAsList());
            if (!lore.isEmpty()) {
                int offset = getScrollType() == ScrollType.SCROLL ? 3 : (scrollPos % pageSize) + 1;
                int y1 = lineHeight * (titleLines.size() + offset);
                int x1 = x + 10;
                int y2 = y1 + lineHeight * lore.size() + 1;
                int x2 = x + width;
                g.setColor(MapUtil.getMapColor(10));
                g.fillRect(x1, y1, x2 - x1, y2 - y1);
                g.setColor(MapUtil.getMapColor(11));
                g.draw3DRect(x1, y1, x2 - x1, y2 - y1, true);
                yPos = y2 - (2 + lineHeight * (lore.size() - 1));
                g.setClip(x1, y1, x2 - x1, y2 - y1);
                for (String l : lore) {
                    g.setColor(MapUtil.getChatColor(0));
                    drawText(g, x1 + 2, yPos, l);
                    yPos += lineHeight;
                }
            }
        }
        g.dispose();
        return result;
    }

    private void drawText(Graphics g, ViewJustification itemJust, int y, String text) {
        FontMetrics metrics = g.getFontMetrics();
        int textWidth = metrics.stringWidth(text.replaceAll("\u00a7.", ""));
        drawText(g, getXOffset(itemJust, textWidth), y, text);
    }

    private void drawMessage(Graphics g, String[] text) {
        FontMetrics metrics = g.getFontMetrics();
        int h = metrics.getHeight() + getLineSpacing();
        int y = getY() + (getHeight() - h * text.length) / 2;
        for (String s : text) {
            int x = getX() + (getWidth() - metrics.stringWidth(s)) / 2;
            drawText(g, x, y, s);
            y += h;
        }
    }

    private static final byte BOLD = 0x01;
    private static final byte ITALIC = 0x02;
    private static final byte UNDERLINE = 0x04;
    private static final byte STRIKE = 0x08;

    private void drawText(Graphics g, int x, int y, String text) {
        FontMetrics metrics = g.getFontMetrics();

        byte flags = 0;

        g.setColor(MapUtil.getChatColor(0));
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            Character c = text.charAt(i);
            if (c == '\u00a7') {
                // markup code: render what we have so far, then change the font/color
                String s = sb.toString();
                int width = metrics.stringWidth(s);
                int height = metrics.getMaxAscent();
                renderTextElement(g, s, x, y, width, height, flags);
                x += width;
                sb.delete(0, sb.length());
                i++;
                c = Character.toLowerCase(text.charAt(i));
                if (c >= '0' && c <= '9' || c >= 'a' && c <= 'f') {
                    byte mcColor = Byte.parseByte(c.toString(), 16);
                    g.setColor(MapUtil.getChatColor(mcColor));
                    flags = 0x0;
                } else if (c == 'l') {
                    flags |= BOLD;
                } else if (c == 'm') {
                    flags |= STRIKE;
                } else if (c == 'n') {
                    flags |= UNDERLINE;
                } else if (c == 'o') {
                    flags |= ITALIC;
                } else if (c == 'r') {
                    flags = 0;
                    g.setColor(DEFAULT_COLOR);
                }
            } else {
                sb.append(c);
            }
        }
        String s = sb.toString();
        int width = metrics.stringWidth(s);
        int height = metrics.getMaxAscent();
        renderTextElement(g, s, x, y, width, height, flags);
        g.setColor(DEFAULT_COLOR);
    }

    private void renderTextElement(Graphics g, String s, int x, int y, int width, int height, byte flags) {
        Font f = g.getFont();

        int style = 0;
        if ((flags & BOLD) == BOLD) style |= Font.BOLD;
        if ((flags & ITALIC) == ITALIC) style |= Font.ITALIC;
        if (style != 0)
            g.setFont(new Font(f.getFamily(), style, f.getSize()));

        g.drawString(s, x, y);

        if ((flags & UNDERLINE) == UNDERLINE) g.drawLine(x, y, x + width, y);
        if ((flags & STRIKE) == STRIKE)
            g.drawLine(x, y - height / 2, x + width, y - height / 2);
        g.setFont(f);
    }

    private int getXOffset(ViewJustification just, int width) {
        switch (just) {
            case LEFT:
                return getX();
            case CENTER:
                return getX() + (getWidth() - width) / 2;
            case RIGHT:
                return getX() + getWidth() - width;
            default:
                return 0;
        }
    }

    private static BufferedImage deepCopy(BufferedImage bi) {
        ColorModel cm = bi.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    public class SMSMapRenderer extends MapRenderer {
        private final SMSMapView view;

        public SMSMapRenderer(SMSMapView view) {
            super(true);
            this.view = view;
        }

        public SMSMapView getView() {
            return view;
        }

        @Override
        public void render(MapView map, MapCanvas canvas, Player player) {
            if (isDirty(player)) {
                BufferedImage img = renderImage(player);
                canvas.drawImage(0, 0, img);
                setDirty(player, false);
                player.sendMap(map);
            }
        }
    }
}
