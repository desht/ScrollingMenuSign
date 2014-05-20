package me.desht.scrollingmenusign.commands;

import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.views.SMSView;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class RepaintCommand extends SMSAbstractCommand {
    public RepaintCommand() {
        super("sms repaint", 0, 1);
        setPermissionNode("sms.commands.repaint");
        setUsage("/<cmd> repaint [<view-name>]");
    }

    @Override
    public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
        if (args.length > 1) {
            SMSView view = getView(sender, args[0]);
            view.setDirty(true);
            MiscUtil.statusMessage(sender, "Marked view &6" + view.getName() + "&- as needing repaint.");
        } else {
            for (SMSView view : getViewManager(plugin).listViews()) {
                view.setDirty(true);
            }
            MiscUtil.statusMessage(sender, "Marked all views as needing repaint.");
        }
        return true;
    }
}
