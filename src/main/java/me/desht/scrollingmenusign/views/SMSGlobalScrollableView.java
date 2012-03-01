package me.desht.scrollingmenusign.views;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.views.redout.Switch;

/**
 * @author desht
 *
 * This is just like a SMSScrollableView but per-player scrolling is false by default.
 * So only one scroll position is maintained for all players.
 * 
 * It also maintains a set of output switches which are powered/unpowered depending on
 * the selected item in this view.
 */
public abstract class SMSGlobalScrollableView extends SMSScrollableView {
	private Set<Switch> switches;
	
	public SMSGlobalScrollableView(SMSMenu menu) {
		this(null, menu);
	}

	public SMSGlobalScrollableView(String name, SMSMenu menu) {
		super(name, menu);
		setPerPlayerScrolling(false);
	}
	
	public void addSwitch(Switch sw) {
		switches.add(sw);
	}
	
	public void removeSwitch(Switch sw) {
		switches.remove(sw);
	}
	
	public void updateSwitchPower() {
		String selected = getMenu().getItemAt(getLastScrollPos()).getLabel();
		
		for (Switch sw : switches) {
			sw.setPowered(sw.getTrigger().equals(selected));
		}
	}
	
	@Override
	public Map<String,Object> freeze() {
		Map<String, Object> map = super.freeze();
		
		List<Map<String,Object>> l = new ArrayList<Map<String,Object>>();
		for (Switch sw : switches) {
			l.add(sw.freeze());
		}
		map.put("switches", l);
		
		return map;
	}
	
	@Override
	protected void thaw(ConfigurationSection node) throws SMSException {
		super.thaw(node);
		
		List<Map<String,Object>> l = node.getMapList("switches");
		for (Map<String,Object> m : l) {
			MemoryConfiguration conf = new MemoryConfiguration();
			for (Entry<String,Object> e : m.entrySet()) {
				conf.set(e.getKey(), e.getValue());
			}
			addSwitch(new Switch(this, conf));
		}
	}
}
