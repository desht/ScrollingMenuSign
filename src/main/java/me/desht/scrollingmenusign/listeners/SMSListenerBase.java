package me.desht.scrollingmenusign.listeners;

import me.desht.scrollingmenusign.ScrollingMenuSign;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

public abstract class SMSListenerBase implements Listener {
    protected final ScrollingMenuSign plugin;

    public SMSListenerBase(ScrollingMenuSign plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
}
