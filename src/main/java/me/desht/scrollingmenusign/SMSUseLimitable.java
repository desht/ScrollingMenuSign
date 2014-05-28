package me.desht.scrollingmenusign;

import org.bukkit.command.CommandSender;

/**
 * Objects which hold usage-limit data must implement this interface.
 */
public interface SMSUseLimitable {
    void autosave();

    String getDescription();

    String formatUses(CommandSender sender);

    SMSRemainingUses getUseLimits();

    String getLimitableName();
}
