package me.desht.scrollingmenusign.util;

import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.UUIDFetcher;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.views.SMSView;
import org.bukkit.Bukkit;

import java.util.*;

public class UUIDMigration {

    public static void migrateToUUID(final ScrollingMenuSign plugin) {
        if (plugin.getConfig().getBoolean("sms.uuid_migration_done")) {
            return;
        }
        final Set<String> names = new HashSet<String>();
        for (SMSMenu menu : plugin.getHandler().listMenus()) {
            if (!MiscUtil.looksLikeUUID(menu.getOwner())) {
                names.add(menu.getOwner());
            }
        }
        for (SMSView view : plugin.getViewManager().listViews()) {
            if (!MiscUtil.looksLikeUUID(view.getAttributeAsString(SMSView.OWNER))) {
                names.add(view.getAttributeAsString(SMSView.OWNER));
            }
        }
        if (!names.isEmpty()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, new AsyncTask(plugin, names));
        }
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
                final Map<String, UUID> response = uf.call();
                Bukkit.getScheduler().runTask(plugin, new SyncTask(plugin, response));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static class SyncTask implements Runnable {
        private final Map<String, UUID> response;
        private final ScrollingMenuSign plugin;

        public SyncTask(ScrollingMenuSign plugin, Map<String, UUID> response) {
            this.response = response;
            this.plugin = plugin;
        }

        @Override
        public void run() {
            for (SMSMenu menu : plugin.getHandler().listMenus()) {
                if (!MiscUtil.looksLikeUUID(menu.getOwner())) {
                    UUID uuid = response.get(menu.getOwner());
                    menu.setOwnerId(uuid);
                }
            }
            for (SMSView view : plugin.getViewManager().listViews()) {
                if (!MiscUtil.looksLikeUUID(view.getAttributeAsString(SMSView.OWNER))) {
                    String owner = view.getAttributeAsString(SMSView.OWNER);
                    UUID uuid = response.get(owner);
                    view.setOwnerId(uuid);
                }
            }

            plugin.getConfig().set("sms.uuid_migration_done", true);
            plugin.saveConfig();

            LogUtils.info("Menu & view owner names migrated to UUID format");
        }
    }
}
