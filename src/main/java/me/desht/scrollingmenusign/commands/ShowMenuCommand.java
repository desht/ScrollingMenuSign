package me.desht.scrollingmenusign.commands;

import java.util.List;

import me.desht.dhutils.MessagePager;
import me.desht.dhutils.block.MaterialWithData;
import me.desht.dhutils.commands.AbstractCommand;
import me.desht.scrollingmenusign.PopupBook;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSHandler;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSMenuItem;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.views.SMSMapView;
import me.desht.scrollingmenusign.views.SMSView;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class ShowMenuCommand extends AbstractCommand {

	public ShowMenuCommand() {
		super("sms sh", 0, 1);
		setPermissionNode("scrollingmenusign.commands.show");
		setUsage("/sms show <menu-name>");
		setQuotedArgs(true);
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {	
		SMSHandler handler = ((ScrollingMenuSign)plugin).getHandler();
		
		SMSMenu menu = null;
		SMSView view = null;
		if (args.length > 0) {
			menu = handler.getMenu(args[0]);
		} else {
			notFromConsole(sender);
			Player player = (Player) sender;
			view = SMSView.getTargetedView(player);
			if (view == null) {
				if (player.getItemInHand().getType() == Material.MAP) {		// map
					short mapId = player.getItemInHand().getDurability();
					view = SMSMapView.getViewForId(mapId);
				} else if (PopupBook.holding(player)) {
					PopupBook book = PopupBook.get(player);
					view = book.getView();
				}
			}
			if (view == null) {
				throw new SMSException("No suitable view found.");
			}
			menu = view.getActiveMenu(player.getName());
		}
		
		MessagePager pager = MessagePager.getPager(sender).clear();
		String mo = menu.getOwner().isEmpty() ? "(no one)" : menu.getOwner().replace("&", "&&");
		pager.add(String.format("Menu &e%s&-, Title \"&f%s&-\", Owner &e%s&-",
		                                       menu.getName(),  menu.getTitle(), mo));
		if (!menu.formatUses(sender).isEmpty()) {
			pager.add("Uses: &c" + menu.formatUses(sender));
		}
		if (!menu.getDefaultCommand().isEmpty()) {
			pager.add(" Default command: &f" + menu.getDefaultCommand());
		}
		if (view != null) {
			String owner = view.getAttributeAsString("owner", "(no one)");
			pager.add(String.format("View &e%s&-, Owner &e%s&-", view.getName(), owner));
		}
		
		String defIcon = MaterialWithData.get(plugin.getConfig().getString("sms.inv_view.default_icon", "stone")).toString();
		
		List<SMSMenuItem> items = menu.getItems();
		int n = 1;
		for (SMSMenuItem item : items) {
			String message = item.getMessage();
			String command = item.getCommand().replace(" && ", " &&&& ");
			String uses = item.formatUses(sender);
			String icon = item.getIconMaterial().toString();
			String s = String.format("&e%2d) &f%s &7[%s]", // &e%s&c%s",
					n++, item.getLabel(), command); //, message, item.formatUses(sender));
			pager.add(s);
			if (!message.isEmpty()) pager.add("    &9Feedback: &e" + message);
			if (!uses.isEmpty()) pager.add("    &9Uses: &e" + uses);
			if (!icon.equalsIgnoreCase(defIcon)) pager.add("    &9Icon: &e" + icon);
			String[] lore = item.getLore();
			for (int i = 0; i < lore.length; i++) {
				pager.add((i == 0 ? "    &9Lore: &e" : "          &e") + lore[i]);
			}
		}
		
		pager.showPage();
		
		return true;
	}

}
