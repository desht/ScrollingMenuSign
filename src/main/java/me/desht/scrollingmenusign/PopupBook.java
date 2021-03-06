package me.desht.scrollingmenusign;

import java.lang.ref.WeakReference;

import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.views.PoppableView;
import me.desht.scrollingmenusign.views.SMSView;

import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;


/**
 * @author des
 *         <p/>
 *         Represents a written book that can be used to trigger poppable views.
 */
public class PopupBook {
    private static final int VIEW_TYPE = 2;
    private static final int VIEW_NAME = 3;
    private static final int MENU_NAME = 4;

    private final WeakReference<SMSView> view;
    private final WeakReference<Player> player;

    /**
     * Private constructor (use PopupBook.get()).  Create a popup book object from a
     * written book item.
     *
     * @param player the player
     * @param bi     the book item
     */
    private PopupBook(Player player, ItemStack bi) {
        BookMeta bm = (BookMeta) bi.getItemMeta();

        String viewType = bm.getPage(VIEW_TYPE).split(" ")[1];
        String viewName = bm.getPage(VIEW_NAME);
        String menuName = bm.getPage(MENU_NAME);

        if (!ScrollingMenuSign.getInstance().getHandler().checkMenu(menuName)) {
            // the menu's been deleted? the book's of no use anymore
            throw new SMSException("Missing menu " + menuName);
        }

        SMSView wantedView = null;
        if (!ScrollingMenuSign.getInstance().getViewManager().checkForView(viewName)) {
            // the view could have been deleted - see if the menu has any other views of the same type
            for (SMSView view : ScrollingMenuSign.getInstance().getViewManager().listViews()) {
                if (view.getNativeMenu().getName().equals(menuName) && view.getType().equals(viewType)) {
                    wantedView = view;
                    break;
                }
            }
            if (wantedView != null) {
                // update the book to refer to the new view we found
                bm.setPage(VIEW_NAME, wantedView.getName());
            }
        } else {
            wantedView = ScrollingMenuSign.getInstance().getViewManager().getView(viewName);
        }

        SMSValidate.isTrue(wantedView != null && wantedView instanceof PoppableView, "Invalid view: " + viewName);
        this.player = new WeakReference<Player>(player);
        this.view = new WeakReference<SMSView>(wantedView);
    }

    /**
     * Create a popup book object for the given player and view.
     *
     * @param player the player object
     * @param view   the view
     */
    public PopupBook(Player player, SMSView view) {
        SMSValidate.isTrue(view instanceof PoppableView, "Invalid view: " + view.getName());
        this.player = new WeakReference<Player>(player);
        this.view = new WeakReference<SMSView>(view);
    }

    public SMSView getView() {
        return view.get();
    }

    /**
     * Toggle the popped state of the view this book refers to.
     */
    public void toggle() {
        Player p = this.player.get();
        SMSView v = getView();
        if (p != null && v != null) {
            v.ensureAllowedToUse(p);
            ((PoppableView) v).toggleGUI(p);
        }
    }

    /**
     * Get the book item corresponding to this popup book.
     *
     * @return an ItemStack of 1 written book with the title and pages filled in
     */
    public ItemStack toItemStack() {
        return toItemStack(1);
    }

    /**
     * Get one or more book item corresponding to this popup book.
     *
     * @param amount the number of books
     * @return an ItemStack of one or mote written books with the title and pages filled in
     */
    public ItemStack toItemStack(int amount) {
        Player p = this.player.get();
        SMSView v = getView();
        if (v == null || p == null) {
            return null;
        }
        ItemStack item = new ItemStack(Material.WRITTEN_BOOK, amount);

        BookMeta bm = (BookMeta) item.getItemMeta();

        bm.setTitle(v.doVariableSubstitutions(null, v.getNativeMenu().getTitle()));
        bm.setAuthor(p.getDisplayName());
        bm.setPages("Left Click to Use!",
                "sms " + v.getType() + " view",
                v.getName(),
                v.getNativeMenu().getName());
        item.setItemMeta(bm);

        return item;
    }

    /**
     * Get the popup book that the player is holding, if any.
     *
     * @param p the player
     * @return the book, or null if the player is not holding one
     * @throws SMSException if the player is holding a popup book, but it's not valid
     */
    public static PopupBook get(Player p) {
        if (!holding(p)) {
            return null;
        }
        try {
            return new PopupBook(p, p.getItemInHand());
        } catch (SMSException e) {
            destroy(p);
            return null;
        }
    }

    /**
     * Check if the player is holding a popup book.
     *
     * @param p the player
     * @return true if the player is holding a popup book
     */
    public static boolean holding(Player p) {
        if (p.getItemInHand().getType() != Material.WRITTEN_BOOK) {
            return false;
        }
        BookMeta bm = (BookMeta) p.getItemInHand().getItemMeta();
        return bm.getPageCount() >= 4 && bm.getPage(VIEW_TYPE).matches("^sms [\\w-]+ view$");
    }

    /**
     * Destroys the item in the player's hand.  Doesn't check first to see if it's a
     * popup book - would usually be called if PopupBook.get() throws a SMSException.
     *
     * @param player the player object
     */
    private static void destroy(Player player) {
        player.setItemInHand(new ItemStack(Material.AIR));
        MiscUtil.statusMessage(player, "Your book suddenly vanishes in a puff of smoke!");
        player.playEffect(player.getLocation().add(player.getLocation().getDirection()), Effect.SMOKE, BlockFace.UP);
    }
}
