package me.desht.scrollingmenusign.expector;

import org.bukkit.entity.Player;

import me.desht.scrollingmenusign.SMSException;

public abstract class ExpectBase {

	public abstract void doResponse(Player p) throws SMSException;

}
