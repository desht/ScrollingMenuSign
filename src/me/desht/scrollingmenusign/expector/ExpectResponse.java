package me.desht.scrollingmenusign.expector;

import java.util.HashMap;
import java.util.Map;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.enums.ExpectAction;

import org.bukkit.entity.Player;

public class ExpectResponse {

	private final Map<String, ExpectData> exp = new HashMap<String, ExpectData>();

	public ExpectResponse() {
	}

	public void expectingResponse(Player p, ExpectAction action, ExpectData data) {
		expectingResponse(p, action, data, null);
	}

	public void expectingResponse(Player p, ExpectAction action, ExpectData data, String expectee) {
		if (expectee != null) {
			exp.put(genKey(expectee, action), data);
		} else {
			exp.put(genKey(p, action), data);
		}
		data.setAction(action);
	}

	public boolean isExpecting(Player p, ExpectAction action) {
		return exp.containsKey(genKey(p, action));
	}

	public void handleAction(Player p, ExpectAction action) throws SMSException {
		exp.get(genKey(p, action)).doResponse(p);
		cancelAction(p, action);
	}

	public void cancelAction(Player p, ExpectAction action) {
		exp.remove(genKey(p, action));
	}

	public ExpectData getAction(Player p, ExpectAction action) {
		return exp.get(genKey(p, action));
	}

	private String genKey(Player p, ExpectAction action) {
		return p.getName() + ":" + action.toString();
	}

	private String genKey(String name, ExpectAction action) {
		return name + ":" + action.toString();
	}
}
