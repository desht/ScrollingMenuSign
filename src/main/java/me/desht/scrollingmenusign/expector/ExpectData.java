package me.desht.scrollingmenusign.expector;

import org.bukkit.entity.Player;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.enums.ExpectAction;

public abstract class ExpectData {
	ExpectAction action;

	public abstract void doResponse(Player p) throws SMSException;

	public void setAction(ExpectAction action) {
		this.action = action;
	}

	public ExpectAction getAction() {
		return action;
	}
}
