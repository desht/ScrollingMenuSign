package me.desht.scrollingmenusign.listeners;

import java.util.logging.Level;

import me.desht.register.payment.Methods;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.util.MiscUtil;

import org.bukkit.Bukkit;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerListener;


public class SMSServerListener extends ServerListener {
	private Methods methods = null;
	
	public SMSServerListener() {
		this.methods = new Methods();
	}
	
	@Override
    public void onPluginDisable(PluginDisableEvent event) {
        // Check to see if the plugin that's being disabled is the one we are using
        if (this.methods != null && Methods.hasMethod()) {
            Boolean check = Methods.checkDisabled(event.getPlugin());

            if (check) {
                ScrollingMenuSign.setEconomy(null);
                MiscUtil.log(Level.INFO, "Payment method was disabled. No longer accepting payments.");
            }
        }
    }

    @Override
    public void onPluginEnable(PluginEnableEvent event) {
    	System.out.println("plugin enable: " + event.getPlugin().getDescription().getName());
        // Check to see if we need a payment method
        if (!Methods.hasMethod()) {
            if (Methods.setMethod(Bukkit.getServer().getPluginManager())) {
            	ScrollingMenuSign.setEconomy(Methods.getMethod());
            	MiscUtil.log(Level.INFO, "Payment method found ("
                                   + ScrollingMenuSign.getEconomy().getName() + " version: " + ScrollingMenuSign.getEconomy().getVersion() + ")");
            }
        }
    }
}
