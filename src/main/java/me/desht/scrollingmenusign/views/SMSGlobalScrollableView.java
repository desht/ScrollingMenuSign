package me.desht.scrollingmenusign.views;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.enums.SMSUserAction;
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

	private final Set<Switch> switches = new HashSet<Switch>();

	public final String RS_OUTPUT_MODE = "rsoutputmode";

	public SMSGlobalScrollableView(SMSMenu menu) {
		this(null, menu);
	}

	public SMSGlobalScrollableView(String name, SMSMenu menu) {
		super(name, menu);
		setPerPlayerScrolling(false);
		registerAttribute(RS_OUTPUT_MODE, "selected");
	}

	public void addSwitch(Switch sw) {
		switches.add(sw);
		autosave();
	}

	public void removeSwitch(Switch sw) {
		switches.remove(sw);
		autosave();
	}

	public void updateSwitchPower() {
		String selectedItem = getMenu().getItemAt(getLastScrollPos()).getLabel();

		for (Switch sw : switches) {
			sw.setPowered(sw.getTrigger().equals(selectedItem));
		}
	}

	public void toggleSwitchPower() {
		String selectedItem = getMenu().getItemAt(getLastScrollPos()).getLabel();
		for (Switch sw : switches) {
			if (sw.getTrigger().equals(selectedItem)) {
				sw.setPowered(!sw.getPowered());
			}
		}
	}

	public Set<Switch> getSwitches() {
		return switches;
	}

	@Override
	public Map<String,Object> freeze() {
		Map<String, Object> map = super.freeze();

		Map<String,Map<String,Object>> l = new HashMap<String, Map<String,Object>>();
		for (Switch sw : switches) {
			l.put(sw.getName(), sw.freeze());
		}
		map.put("switches", l);

		return map;
	}

	@Override
	protected void thaw(ConfigurationSection node) throws SMSException {
		super.thaw(node);

		ConfigurationSection sw = node.getConfigurationSection("switches");
		if (sw == null) {
			return;
		}
		for (String k : sw.getKeys(false)) {
			ConfigurationSection conf = node.getConfigurationSection("switches." + k);
			try {
				new Switch(this, conf);
			} catch (IllegalArgumentException e) {
				Switch.deferLoading(this, conf);
			}
		}
		updateSwitchPower();
	}

	@Override
	public void onScrolled(Player player, SMSUserAction action) {
		super.onScrolled(player, action);

		if (getAttributeAsString(RS_OUTPUT_MODE).equalsIgnoreCase("selected")) {
			updateSwitchPower();
		}
	}
	
	@Override
	public void onExecuted(Player player) {
		super.onExecuted(player);
		
		if (getAttributeAsString(RS_OUTPUT_MODE).equalsIgnoreCase("toggle")) {
			toggleSwitchPower();
		}
	}

	@Override
	protected void onAttributeValidate(String attribute, String curVal, String newVal) throws SMSException {
		if (attribute.equalsIgnoreCase(RS_OUTPUT_MODE)) {
			if (!newVal.equalsIgnoreCase("toggle") && !newVal.equalsIgnoreCase("selected")) {
				throw new SMSException("Accepted values are 'selected' and 'toggle'.");
			}
		}
	}
}
