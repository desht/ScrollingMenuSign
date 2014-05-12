package me.desht.scrollingmenusign.parser;

import me.desht.scrollingmenusign.views.CommandTrigger;
import org.bukkit.entity.Player;

public interface SubstitutionHandler {
    /**
     * A subsititution handler; given a player and a command trigger object, return
     * a string which will be used to replace the substitution template that this
     * handler is registered for.
     *
     * @param player the player running the command
     * @param trigger the command trigger that caused this command to run
     * @return a replacement string
     */
    public String sub(Player player, CommandTrigger trigger);
}
