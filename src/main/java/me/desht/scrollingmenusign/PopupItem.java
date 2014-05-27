package me.desht.scrollingmenusign;

import com.google.common.base.Joiner;
import me.desht.dhutils.Debugger;
import me.desht.dhutils.ItemGlow;
import me.desht.scrollingmenusign.views.PoppableView;
import me.desht.scrollingmenusign.views.SMSView;
import me.desht.scrollingmenusign.views.ViewManager;
import me.desht.scrollingmenusign.views.hologram.HoloUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.metadata.MetadataValue;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;

public class PopupItem {
    private static final String SEPARATOR = "▶";
    public static final int MENU_NAME_FIELD = 1;
    public static final int VIEW_NAME_FIELD = 2;
    public static final int VIEW_TYPE_FIELD = 3;

    private final WeakReference<SMSView> viewRef;
    private final MaterialData mat;

    private PopupItem(MaterialData mat, SMSView view) {
        SMSValidate.isTrue(view instanceof PoppableView, "View type " + view.getType() + " is not a poppable view");
        this.viewRef = new WeakReference<SMSView>(view);
        this.mat = mat;
    }

    public SMSView getView() {
        return viewRef.get();
    }

    public void toggle(final Player player) {
        final SMSView view = viewRef.get();
        if (view == null) {
            return;
        }
        final PoppableView pop = (PoppableView) view;
        view.ensureAllowedToUse(player);

        // By deferring this, we get a chance to know if the player is using the popup item
        // to interact with a hologram on this tick.  If he is, then don't do any popup/popdown
        // of the hologram.
        Bukkit.getScheduler().runTask(ScrollingMenuSign.getInstance(), new Runnable() {
            @Override
            public void run() {
                long when = getLastHoloInteraction(player);
                if (System.currentTimeMillis() - when > HoloUtil.HOLO_POPDOWN_TIMEOUT) {
                    if (pop.hasActiveGUI(player)) {
                        Debugger.getInstance().debug("popup item: close " + view.getName() + " for " + player.getName());
                        pop.hideGUI(player);
                    } else {
                        Debugger.getInstance().debug("popup item: open " + view.getName() + " for " + player.getName());
                        pop.showGUI(player);
                    }
                }
            }
        });
    }

    private long getLastHoloInteraction(Player player) {
        for (MetadataValue mv : player.getMetadata(HoloUtil.LAST_HOLO_INTERACTION)) {
            if (mv.getOwningPlugin() == ScrollingMenuSign.getInstance()) {
                return (Long) mv.value();
            }
        }
        return 0;
    }

    public ItemStack toItemStack() {
        return toItemStack(1);
    }

    public ItemStack toItemStack(int amount) {
        SMSView view = viewRef.get();
        if (view == null) {
            return null;
        }
        ItemStack res = mat.toItemStack(amount);
        ItemMeta meta = res.getItemMeta();
        meta.setDisplayName(view.getNativeMenu().getTitle());
        meta.setLore(Arrays.asList(Joiner.on(SEPARATOR).join(
                ChatColor.BLACK.toString(),
                view.getNativeMenu().getName(),
                view.getName(),
                view.getType()
        )));
        res.setItemMeta(meta);
        if (ScrollingMenuSign.getInstance().isProtocolLibEnabled()) {
            ItemGlow.setGlowing(res, true);
        }
        return res;
    }

    /**
     * Given an item, attempt to build a PopupItem object.
     *
     * @param stack the item stack
     * @return a PopupItem object, or null if the stack is not a popup item
     * @throws SMSException if the stack is a popup item but the view & menu are not valid
     */
    public static PopupItem get(ItemStack stack) {
        String[] f = getPopupItemFields(stack.getItemMeta());
        if (f == null) {
            return null;
        }

        ViewManager vm = ScrollingMenuSign.getInstance().getViewManager();
        if (vm.checkForView(f[VIEW_NAME_FIELD])) {
            return new PopupItem(stack.getData(), vm.getView(f[VIEW_NAME_FIELD]));
        } else if (ScrollingMenuSign.getInstance().getHandler().checkMenu(f[MENU_NAME_FIELD])) {
            // the view doesn't exist (must have been deleted?)
            // but we can attempt to find another view of the same type
            SMSMenu menu = ScrollingMenuSign.getInstance().getHandler().getMenu(f[MENU_NAME_FIELD]);
            SMSView view = vm.findView(menu, f[VIEW_TYPE_FIELD]);
            SMSValidate.notNull(view, "Menu has no view of type " + f[VIEW_TYPE_FIELD]);
            return new PopupItem(stack.getData(), view);
        } else {
            throw new SMSException("Invalid menu and view");
        }
    }

    public static PopupItem create(ItemStack stack, SMSView view) {
        SMSValidate.isTrue(view instanceof PoppableView, "That view is not a popup view");
        return new PopupItem(stack.getData(), view);
    }

    public static PopupItem get(Player player) {
        return get(player.getItemInHand());
    }

    public static String[] getPopupItemFields(ItemMeta itemMeta) {
        if (itemMeta == null || !itemMeta.hasLore()) {
            return null;
        }
        List<String> lore = itemMeta.getLore();
        if (lore.isEmpty()) {
            return null;
        }
        String line = lore.get(lore.size() - 1);
        String[] f = line.split(SEPARATOR);
        if (f.length != 4) {
            return null;
        }
        return f;
    }
}
