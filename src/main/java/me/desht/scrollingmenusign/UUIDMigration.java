package me.desht.scrollingmenusign;

import me.desht.dhutils.LogUtils;
import me.desht.dhutils.UUIDFetcher;
import me.desht.scrollingmenusign.views.SMSView;
import org.bukkit.Bukkit;

import java.util.*;

public class UUIDMigration {

	public static void migrateToUUID(final ScrollingMenuSign plugin) {
		if (plugin.getConfig().getBoolean("sms.uuid_migration_done")) {
			return;
		}
		final Set<String> names = new HashSet<String>();
		for (SMSMenu menu : SMSMenu.listMenus()) {
			names.add(menu.getOwner());
		}
		for (SMSView view : plugin.getViewManager().listViews()) {
			names.add(view.getAttributeAsString(SMSView.OWNER));
		}
		if (!names.isEmpty()) {
			Bukkit.getScheduler().runTaskAsynchronously(plugin, new AsyncTask(plugin, names));
		}

		plugin.getConfig().set("sms.uuid_migration_done", true);
		plugin.saveConfig();
	}

	private static class AsyncTask implements Runnable {
		private final Set<String> names;
		private final ScrollingMenuSign plugin;

		public AsyncTask(ScrollingMenuSign plugin, Set<String> names) {
			this.names = names;
			this.plugin = plugin;
		}

		@Override
		public void run() {
			UUIDFetcher uf = new UUIDFetcher(new ArrayList<String>(names));
			try {
				final Map<String,UUID> response = uf.call();
				Bukkit.getScheduler().runTask(plugin, new SyncTask(plugin, response));
				for (String u : response.keySet()) {
					System.out.println(u + " => " + response.get(u));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static class SyncTask implements Runnable {
		private final Map<String,UUID> response;
		private final ScrollingMenuSign plugin;

		public SyncTask(ScrollingMenuSign plugin, Map<String, UUID> response) {
			this.response = response;
			this.plugin = plugin;
		}

		@Override
		public void run() {
			for (SMSMenu menu : SMSMenu.listMenus()) {
				UUID uuid = response.get(menu.getOwner());
				menu.setOwnerId(uuid);
			}
			for (SMSView view : plugin.getViewManager().listViews()) {
				String owner = view.getAttributeAsString(SMSView.OWNER);
				UUID uuid = response.get(owner);
				view.setOwnerId(uuid);
			}
			LogUtils.info("Menu & view owner names migrated to UUID format");
		}
	}
}
