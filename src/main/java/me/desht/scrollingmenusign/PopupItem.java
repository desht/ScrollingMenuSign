package me.desht.scrollingmenusign;

import com.google.common.base.Joiner;
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

import java.util.Arrays;
import java.util.List;

public class PopupItem {
    private static final String SEPARATOR = "▶";
    public static final int VIEW_NAME = 2;
    public static final int MENU_NAME = 1;
    public static final int VIEW_TYPE = 3;

    private final SMSView view;
    private final MaterialData mat;

    private PopupItem(MaterialData mat, SMSView view) {
        SMSValidate.isTrue(view instanceof PoppableView, "View type " + view.getType() + " is not a view view");
        //noinspection ConstantConditions
        this.view = view;
        this.mat = mat;
    }

    public SMSView getView() {
        return view;
    }

    public void toggle(final Player player) {
        final PoppableView pop = (PoppableView) view;

        if (pop.hasActiveGUI(player)) {
            // Cheeky hack: if the player has very recently interacted with a hologram, we can
            // infer that he's still looking at it, in which case we do *not* pop the view down
            Bukkit.getScheduler().runTask(ScrollingMenuSign.getInstance(), new Runnable() {
                @Override
                public void run() {
                    long when = getLastHoloInteraction(player);
                    if (System.currentTimeMillis() - when > HoloUtil.HOLO_POPDOWN_TIMEOUT) {
                        pop.hideGUI(player);
                    }
                }
            });
        } else {
            pop.showGUI(player);
        }
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
        return toItemStack(MENU_NAME);
    }

    public ItemStack toItemStack(int amount) {
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
        if (!stack.hasItemMeta() || !stack.getItemMeta().hasLore()) {
            return null;
        }
        List<String> lore = stack.getItemMeta().getLore();
        if (lore.isEmpty()) {
            return null;
        }
        String line = lore.get(lore.size() - 1);
        String[] f = line.split(SEPARATOR);
        if (f.length != 3) {
            return null;
        }
        ViewManager vm = ScrollingMenuSign.getInstance().getViewManager();

        if (vm.checkForView(f[VIEW_NAME])) {
            return new PopupItem(stack.getData(), vm.getView(f[VIEW_NAME]));
        } else if (ScrollingMenuSign.getInstance().getHandler().checkMenu(f[MENU_NAME])) {
            // the view doesn't exist (must have been deleted?)
            // but we can attempt to find another view of the same type
            SMSMenu menu = ScrollingMenuSign.getInstance().getHandler().getMenu(f[MENU_NAME]);
            SMSView view = vm.findView(menu, f[VIEW_TYPE]);
            SMSValidate.notNull(view, "Invalid view");
            return new PopupItem(stack.getData(), view);
        } else {
            throw new SMSException("Invalid menu and view");
        }
    }

    public static PopupItem create(ItemStack stack, SMSView view) {
        SMSValidate.isTrue(view instanceof PoppableView, "That view is not a popup view");
        return new PopupItem(stack.getData(), view);
    }
}
