package me.desht.scrollingmenusign;

import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PermissionUtils;
import me.desht.dhutils.block.BlockUtil;
import me.desht.scrollingmenusign.views.SMSGlobalScrollableView;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockRedstoneEvent;

public class TooltipSign implements SMSInteractableBlock {

    private final SMSGlobalScrollableView view;

    public TooltipSign(SMSGlobalScrollableView view) {
        this.view = view;
    }

    @Override
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void processEvent(ScrollingMenuSign plugin, BlockDamageEvent event) {
        if (!view.isOwnedBy(event.getPlayer()) && !PermissionUtils.isAllowedTo(event.getPlayer(), "scrollingmenusign.destroy")) {
            event.setCancelled(true);
        }
    }

    @Override
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void processEvent(ScrollingMenuSign plugin, BlockBreakEvent event) {
        view.removeTooltipSign();
        MiscUtil.statusMessage(event.getPlayer(), String.format("Tooltip sign @ &f%s&- was removed from view &e%s&-.",
                MiscUtil.formatLocation(event.getBlock().getLocation()), view.getName()));
    }

    @Override
    public void processEvent(ScrollingMenuSign plugin, BlockPhysicsEvent event) {
        if (BlockUtil.isAttachableDetached(event.getBlock())) {
            if (plugin.getConfigCache().isPhysicsProtected()) {
                event.setCancelled(true);
            } else {
                LogUtils.info("Tooltip sign for " + view.getName() + " @ " + event.getBlock().getLocation() + " has become detached: deleting");
                view.removeTooltipSign();
            }
        }
    }

    @Override
    public void processEvent(ScrollingMenuSign plugin, BlockRedstoneEvent event) {
        // ignore - tooltip signs don't care about redstone signals
    }

}
