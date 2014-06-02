package me.desht.scrollingmenusign.views.icon;

import me.desht.dhutils.Debugger;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenuItem;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.enums.ViewJustification;
import me.desht.scrollingmenusign.util.SMSUtil;
import me.desht.scrollingmenusign.views.SMSInventoryView;
import me.desht.scrollingmenusign.views.SMSPopup;
import me.desht.scrollingmenusign.views.SMSView;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import java.util.Collections;

public class IconMenu implements Listener, SMSPopup {
    private static final int INVENTORY_WIDTH = 9;
    private static final int MAX_INVENTORY_ROWS = 6;
    private static final String SMS_CLOSING_ON_COMMAND = "SMS_Closing_On_Command";
    private static final int MAX_TITLE_LENGTH = 32;

    private final SMSInventoryView view;
    private final Player player;

    private int size = 0;
    private ItemStack[] optionIcons;
    private String[] optionNames;
    private boolean popped = false;

    public IconMenu(SMSInventoryView view, Player player) {
        this.view = view;
        this.player = player;

        Debugger.getInstance().debug("icon menu: register events: " + this + " view=" + view.getName() + ", player=" + player.getName());
        Bukkit.getPluginManager().registerEvents(this, ScrollingMenuSign.getInstance());
    }

    @Override
    public SMSView getView() {
        return view;
    }

    @Override
    public boolean isPoppedUp() {
        return popped;
    }

    @Override
    public void popup() {
        if (!isPoppedUp()) {
            popped = true;
            if (size == 0 || getView().isDirty(player)) {
                buildMenu(player);
            }
            String title = getAbbreviatedTitle();
            Inventory inventory = Bukkit.createInventory(player, size, title);
            for (int i = 0; i < size; i++) {
                inventory.setItem(i, optionIcons[i]);
            }
            player.openInventory(inventory);
            getView().setDirty(player, false);
        }
    }

    @Override
    public void popdown() {
        if (isPoppedUp()) {
            player.closeInventory();
            popped = false;
        }
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    @Override
    public void repaint() {
        getView().setDirty(true);
        if (isPoppedUp()) {
            popdown();
            popup();
        }
    }

    private void buildMenu(Player player) {
        int width = (Integer) getView().getAttribute(SMSInventoryView.WIDTH);
        int spacing = (Integer) getView().getAttribute(SMSInventoryView.SPACING);
        int nItems = getView().getActiveMenuItemCount(player);
        int nRows = Math.min(MAX_INVENTORY_ROWS, (((nItems - 1) / width) + 1) * spacing);

        size = INVENTORY_WIDTH * nRows;
        optionIcons = new ItemStack[size];
        optionNames = new String[size];

        int xOff = getXOffset(width);

        ItemStack defIcon = SMSUtil.parseMaterialSpec(ScrollingMenuSign.getInstance().getConfig().getString("sms.inv_view.default_icon", "STONE"));

        for (int i = 0; i < nItems; i++) {
            int i2 = i * spacing;
            int row = i2 / width;
            int pos = row * INVENTORY_WIDTH + xOff + i2 % width;
            if (pos >= size) {
                LogUtils.warning("inventory view " + getView().getName() + " doesn't have enough slots to display its items");
                break;
            }
            SMSMenuItem menuItem = getView().getActiveMenuItemAt(player, i + 1);
            ItemStack icon = menuItem.hasPermission(player) && menuItem.hasIcon() ? menuItem.getIcon() : defIcon.clone();
            String label = getView().doVariableSubstitutions(player, getView().getActiveItemLabel(player, i + 1));
            ItemMeta im = icon.getItemMeta();
            im.setDisplayName(ChatColor.RESET + label);
            im.setLore(menuItem.hasPermission(player) ?
                    view.doVariableSubstitutions(player, menuItem.getLoreAsList()) :
                    Collections.<String>emptyList());
            icon.setItemMeta(im);
            optionIcons[pos] = icon;
            optionNames[pos] = menuItem.getLabel();
        }

        Debugger.getInstance().debug("built icon menu inventory for " + player.getDisplayName() + ": " + size + " slots");
    }

    private int getMenuIndexForSlot(int invSlot) {
        int width = (Integer) getView().getAttribute(SMSInventoryView.WIDTH);

        int row = invSlot / INVENTORY_WIDTH;
        int col = invSlot % INVENTORY_WIDTH;

        return row * width + (col - getXOffset(width)) + 1;
    }

    private int getXOffset(int width) {
        ViewJustification ij = view.getItemJustification();
        switch (ij) {
            case LEFT:
                return 0;
            case RIGHT:
                return INVENTORY_WIDTH - width;
            default:
                return (INVENTORY_WIDTH - width) / 2;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player) || !(event.getWhoClicked().equals(player))) {
            return;
        }

        String menuTitle = getAbbreviatedTitle();

        if (isPoppedUp() && event.getInventory().getTitle().equals(menuTitle)) {
            Debugger.getInstance().debug("InventoryClickEvent: player = " + player.getDisplayName() + ", view = " + getView().getName() +
                    ", inventory name = " + event.getInventory().getTitle() + ", icon menu = " + this);

            event.setCancelled(true);
            int slot = event.getRawSlot();
            int spacing = (Integer) getView().getAttribute(SMSInventoryView.SPACING);
            int slot2 = slot == 0 ? 0 : ((slot - 1) / spacing) + 1;
            if (slot >= 0 && slot < size && optionNames[slot] != null) {
                OptionClickEvent optionEvent = new OptionClickEvent(player, getMenuIndexForSlot(slot2), optionNames[slot], event.getClick());
                try {
                    view.onOptionClick(optionEvent);
                } catch (SMSException e) {
                    MiscUtil.errorMessage(player, e.getMessage());
                }
                if (optionEvent.willClose()) {
                    player.setMetadata(SMS_CLOSING_ON_COMMAND, new FixedMetadataValue(ScrollingMenuSign.getInstance(), true));
                    Bukkit.getScheduler().runTask(ScrollingMenuSign.getInstance(), new Runnable() {
                        @Override
                        public void run() {
                            player.closeInventory();
                        }
                    });
                }
                if (optionEvent.willDestroy()) {
                    destroy();
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player) || !(event.getPlayer().equals(this.player))) {
            return;
        }

        String menuTitle = getAbbreviatedTitle();

        if (isPoppedUp() && event.getInventory().getTitle().equals(menuTitle)) {
            Debugger.getInstance().debug("InventoryCloseEvent: player = " + player.getDisplayName() + ", view = " + getView().getName() +
                    ", inventory name = " + event.getInventory().getTitle() + ", icon menu = " + this);
            if ((Boolean) getView().getAttribute(SMSInventoryView.NO_ESCAPE) && !isClosingOnCommand(player)) {
                // force the view to be re-shown
                Bukkit.getScheduler().runTask(ScrollingMenuSign.getInstance(), new Runnable() {
                    @Override
                    public void run() {
                        popup();
                    }
                });
            }
            player.removeMetadata(SMS_CLOSING_ON_COMMAND, ScrollingMenuSign.getInstance());
            popped = false;
        }
    }

    private String getAbbreviatedTitle() {
        return StringUtils.abbreviate(getView().doVariableSubstitutions(player, getView().getActiveMenuTitle(player)), MAX_TITLE_LENGTH);
    }

    private boolean isClosingOnCommand(Player p) {
        for (MetadataValue mv : p.getMetadata(SMS_CLOSING_ON_COMMAND)) {
            if (mv.getOwningPlugin() == ScrollingMenuSign.getInstance()) {
                return (Boolean) mv.value();
            }
        }
        return false;
    }

    public void destroy() {
        Debugger.getInstance().debug("destroying icon menu [" + getPlayer().getName() + "] for view: " + view.getName());
        HandlerList.unregisterAll(this);
        popdown();
    }

    public interface OptionClickEventHandler {
        public void onOptionClick(OptionClickEvent event);
    }

    @Override
    public String toString() {
        return "icon menu [player=" + player.getName() + " view=" + view.getName() + " size=" + size + "]";
    }

    public class OptionClickEvent {
        private final Player player;
        private final int index;
        private final String name;
        private final ClickType clickType;

        private boolean close;
        private boolean destroy;

        public OptionClickEvent(Player player, int index, String name, ClickType type) {
            this.player = player;
            this.index = index;
            this.name = name;
            this.close = true;
            this.destroy = false;
            this.clickType = type;
        }

        public Player getPlayer() {
            return player;
        }

        public int getIndex() {
            return index;
        }

        public String getName() {
            return name;
        }

        public boolean willClose() {
            return close;
        }

        public boolean willDestroy() {
            return destroy;
        }

        public ClickType getClickType() {
            return clickType;
        }

        public void setWillClose(boolean close) {
            this.close = close;
        }

        public void setWillDestroy(boolean destroy) {
            this.destroy = destroy;
        }
    }
}
