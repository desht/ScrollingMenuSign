package me.desht.scrollingmenusign.listeners;

import me.desht.register.payment.Methods;
import me.desht.scrollingmenusign.ScrollingMenuSign;

import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerListener;

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
        if (this.methods != null && this.methods.hasMethod()) {
            Boolean check = this.methods.checkDisabled(event.getPlugin());

            if (check) {
                ScrollingMenuSign.setEconomy(null);
                System.out.println("[" + plugin.getDescription().getName() + "] Payment method was disabled. No longer accepting payments.");
            }
        }
    }

    @Override
    public void onPluginEnable(PluginEnableEvent event) {
        // Check to see if we need a payment method
        if (!this.methods.hasMethod()) {
            if (this.methods.setMethod(event.getPlugin())) {
            	ScrollingMenuSign.setEconomy(methods.getMethod());
                System.out.println("[" + plugin.getDescription().getName() + "] Payment method found ("
                                   + ScrollingMenuSign.getEconomy().getName() + " version: " + ScrollingMenuSign.getEconomy().getVersion() + ")");
            }
        }
    }
}
