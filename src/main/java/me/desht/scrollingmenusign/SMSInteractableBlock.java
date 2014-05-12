package me.desht.scrollingmenusign;

import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockRedstoneEvent;

/**
 * Represents any block which can feed Bukkit event information into SMS.
 */
public interface SMSInteractableBlock {
    public void processEvent(ScrollingMenuSign plugin, BlockDamageEvent event);

    public void processEvent(ScrollingMenuSign plugin, BlockBreakEvent event);

    public void processEvent(ScrollingMenuSign plugin, BlockPhysicsEvent event);

    public void processEvent(ScrollingMenuSign plugin, BlockRedstoneEvent event);
}
