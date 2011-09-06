package me.desht.scrollingmenusign.listeners;

import me.desht.scrollingmenusign.ScrollingMenuSign;

import org.bukkit.Bukkit;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerListener;

import com.LRFLEW.register.payment.Methods;

public class SMSServerListener extends ServerListener {
	private ScrollingMenuSign plugin;
	private Methods methods = null;
	
	public SMSServerListener(ScrollingMenuSign plugin) {
		this.plugin = plugin;
		this.methods = new Methods();
	}
	
	@Override
    public void onPluginDisable(PluginDisableEvent event) {
        // Check to see if the plugin that's being disabled is the one we are using
        if (this.methods != null && Methods.hasMethod()) {
            Boolean check = Methods.checkDisabled(event.getPlugin());

            if (check) {
                ScrollingMenuSign.setEconomy(null);
                System.out.println("[" + plugin.getDescription().getName() + "] Payment method was disabled. No longer accepting payments.");
            }
        }
    }

    @Override
    public void onPluginEnable(PluginEnableEvent event) {
        // Check to see if we need a payment method
        if (!Methods.hasMethod()) {
            if (Methods.setMethod(Bukkit.getServer().getPluginManager())) {
            	ScrollingMenuSign.setEconomy(Methods.getMethod());
                System.out.println("[" + plugin.getDescription().getName() + "] Payment method found ("
                                   + ScrollingMenuSign.getEconomy().getName() + " version: " + ScrollingMenuSign.getEconomy().getVersion() + ")");
            }
        }
    }
}
