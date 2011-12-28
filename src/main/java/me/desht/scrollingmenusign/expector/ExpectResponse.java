package me.desht.scrollingmenusign.expector;

import java.util.HashMap;
import java.util.Map;

import me.desht.scrollingmenusign.SMSException;

import org.bukkit.entity.Player;

public class ExpectResponse {

	private final Map<String, ExpectBase> exp = new HashMap<String, ExpectBase>();

	public void expectingResponse(Player p, ExpectBase data) {
		expectingResponse(p, data, null);
	}

	private void expectingResponse(Player p, ExpectBase data, String expectee) {
		if (expectee != null) {
			exp.put(genKey(expectee, data.getClass()), data);
		} else {
			exp.put(genKey(p, data.getClass()), data);
		}
	}

	private String genKey(String name, Class<? extends ExpectBase> c) {
		return name + ":" + c.getName();
	}

	private String genKey(Player p, Class<? extends ExpectBase> c) {
		return p.getName() + ":" + c.getName();
	}

	public boolean isExpecting(Player p, Class<? extends ExpectBase> action) {
		return exp.containsKey(genKey(p, action));
	}

	public void handleAction(Player p, Class<? extends ExpectBase> action) throws SMSException {
		ExpectBase e = exp.get(genKey(p, action));
//		exp.get(genKey(p, action)).doResponse(p);
		cancelAction(p, action);
		e.doResponse(p);
	}

	public void cancelAction(Player p, Class<? extends ExpectBase> action) {
		exp.remove(genKey(p, action));
	}

	public ExpectBase getAction(Player p, Class<? extends ExpectBase> action) {
		return exp.get(genKey(p, action));
	}

}
