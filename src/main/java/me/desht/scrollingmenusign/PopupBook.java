package me.desht.scrollingmenusign;

import java.lang.ref.WeakReference;

import me.desht.dhutils.BookItem;
import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.views.PoppableView;
import me.desht.scrollingmenusign.views.SMSView;

import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;


/**
 * @author des
 *
 * Represents a written book that can be used to trigger poppable views.
 * 
 */
public class PopupBook {
	
	private final WeakReference<PoppableView> view;
	private final WeakReference<Player> player;

	private PopupBook(Player p, BookItem bi) {
		String[] pages = bi.getPages();
		String viewType = pages[1].split(" ")[0];
		String viewName = pages[2];
		String menuName = pages[3];
		
		if (!ScrollingMenuSign.getInstance().getHandler().checkMenu(menuName)) {
			// the menu's been deleted? the book's of no use anymore
			throw new SMSException("Missing menu " + menuName);
		}
		
		SMSView wantedView = null;
		if (!SMSView.checkForView(viewName)) {
			// the view could have been deleted - see if the menu has any other views of the same type
			for (SMSView view : SMSView.listViews()) {
				if (view.getMenu().getName().equals(menuName) && view.getType().equals(viewType)) {
					wantedView = view;
					break;
				}
			}
			if (wantedView != null) {
				// update the book to refer to the new view we found
				pages[2] = wantedView.getName();
				bi.setPages(pages);
			}
		} else {
			wantedView = SMSView.getView(viewName);
		}
		
		if (wantedView == null || !(wantedView instanceof PoppableView)) {
			// couldn't get a suitable view for this book - destroy it
			throw new SMSException("Missing view " + viewName);
		}
		
		this.player = new WeakReference<Player>(p);
		this.view = new WeakReference<PoppableView>((PoppableView) wantedView);
	}
	
	public void toggle() {
		Player p = this.player.get();
		PoppableView v = this.view.get();
		if (p != null && v != null) {
			v.toggleGUI(p);
		}
	}
	
	/**
	 * Get the popup book that the player is holding, if any.
	 * 
	 * @param p the book, or null if the player is not holding one
	 * @return
	 */
	public static PopupBook get(Player p) {
		if (!holding(p))
			return null;
		BookItem bi = new BookItem(p.getItemInHand());
		return new PopupBook(p, bi);
	}
	
	/**
	 * Check if the player is holding a popup book.
	 * 
	 * @param p
	 * @return
	 */
	public static boolean holding(Player p) {
		if (p.getItemInHand().getType() != Material.WRITTEN_BOOK) {
			return false;
		}
		BookItem bi = new BookItem(p.getItemInHand());
		String[] pages = bi.getPages();
		return pages.length >= 4 || pages[1].endsWith(" view"); 
	}

	public static void destroy(Player p) {
		p.setItemInHand(new ItemStack(0));
		MiscUtil.statusMessage(p, "Your book suddenly vanishes in a puff of smoke!");
		p.playEffect(p.getLocation(), Effect.SMOKE, 4);
	}
}
