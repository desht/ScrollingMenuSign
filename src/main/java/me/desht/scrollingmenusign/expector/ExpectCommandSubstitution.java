package me.desht.scrollingmenusign.expector;

import me.desht.dhutils.DHUtilsException;
import me.desht.dhutils.Debugger;
import me.desht.dhutils.responsehandler.ExpectBase;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.parser.CommandUtils;
import me.desht.scrollingmenusign.views.CommandTrigger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ExpectCommandSubstitution extends ExpectBase {
    private final String command;
    private final CommandTrigger trigger;
    private final boolean isPassword;

    private String sub;

    public ExpectCommandSubstitution(String command, CommandTrigger trigger, boolean isPassword) {
        this.command = command;
        this.trigger = trigger;
        this.isPassword = isPassword;
    }

    public ExpectCommandSubstitution(String command, CommandTrigger trigger) {
        this(command, trigger, false);
    }

    public String getCommand() {
        return command;
    }

    public String getSub() {
        return sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    public boolean isPassword() {
        return isPassword;
    }

    @Override
    public void doResponse(UUID playerId) {
        final String newCommand;
        if (isPassword) {
            newCommand = command.replaceFirst("<\\$p:.+?>", sub);
        } else {
            newCommand = command.replaceFirst("<\\$:.+?>", sub);
        }

        Debugger.getInstance().debug("command substitution: sub = [" + sub + "], cmd = [" + newCommand + "]");
        try {
            final Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                // Using the scheduler here because this response handler is called by the AsyncPlayerChatEvent
                // event handler, which runs in a different thread.
                Bukkit.getScheduler().scheduleSyncDelayedTask(ScrollingMenuSign.getInstance(), new Runnable() {
                    @Override
                    public void run() {
                        CommandUtils.executeCommand(player, newCommand, trigger);
                    }
                });
            }
        } catch (SMSException e) {
            throw new DHUtilsException(e.getMessage());
        }
    }
}
