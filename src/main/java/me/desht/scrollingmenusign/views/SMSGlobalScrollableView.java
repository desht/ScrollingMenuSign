package me.desht.scrollingmenusign.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import me.desht.dhutils.PersistableLocation;
import me.desht.scrollingmenusign.RedstoneControlSign;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSMenuItem;
import me.desht.scrollingmenusign.enums.RedstoneOutputMode;
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
	private final Set<RedstoneControlSign> controlSigns = new HashSet<RedstoneControlSign>();

	public final String RS_OUTPUT_MODE = "rsoutputmode";

	public SMSGlobalScrollableView(SMSMenu menu) {
		this(null, menu);
	}

	public SMSGlobalScrollableView(String name, SMSMenu menu) {
		super(name, menu);
		setPerPlayerScrolling(false);
		registerAttribute(RS_OUTPUT_MODE, RedstoneOutputMode.SELECTED);
	}

	public void addSwitch(Switch sw) {
		switches.add(sw);
		autosave();
	}

	public void removeSwitch(Switch sw) {
		switches.remove(sw);
		autosave();
	}

	public void addControlSign(RedstoneControlSign sign) {
		controlSigns.add(sign);
		autosave();
	}

	public void removeControlSign(RedstoneControlSign sign) {
		controlSigns.remove(sign);
		autosave();
	}

	public void updateSwitchPower() {
		SMSMenuItem item = getMenu().getItemAt(getLastScrollPos());
		if (item == null) {
			return;
		}
		String selectedItem = ChatColor.stripColor(item.getLabel());

		for (Switch sw : switches) {
			sw.setPowered(sw.getTrigger().equals(selectedItem));
		}
	}

	public void toggleSwitchPower() {
		SMSMenuItem item = getMenu().getItemAt(getLastScrollPos());
		if (item == null) {
			return;
		}
		String selectedItem = ChatColor.stripColor(item.getLabel());
		for (Switch sw : switches) {
			if (sw.getTrigger().equals(selectedItem)) {
				sw.setPowered(!sw.getPowered());
			}
		}
	}

	public Set<Switch> getSwitches() {
		return switches;
	}

	public Set<RedstoneControlSign> getControlSigns() {
		return controlSigns;
	}

	@Override
	public Map<String,Object> freeze() {
		Map<String, Object> map = super.freeze();

		Map<String,Map<String,Object>> l = new HashMap<String, Map<String,Object>>();
		for (Switch sw : switches) {
			l.put(sw.getName(), sw.freeze());
		}
		map.put("switches", l);

		List<PersistableLocation> locs = new ArrayList<PersistableLocation>();
		for (RedstoneControlSign s : controlSigns) {
			PersistableLocation pl = new PersistableLocation(s.getlocation());
			pl.setSavePitchAndYaw(false);
			locs.add(pl);
		}
		map.put("controlSigns", locs);

		return map;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void thaw(ConfigurationSection node) throws SMSException {
		super.thaw(node);

		ConfigurationSection sw = node.getConfigurationSection("switches");
		if (sw != null) {

			for (String k : sw.getKeys(false)) {
				ConfigurationSection conf = node.getConfigurationSection("switches." + k);
				try {
					new Switch(this, conf);
				} catch (IllegalArgumentException e) {
					// world not loaded
					Switch.deferLoading(this, conf);
				}
			}
			updateSwitchPower();
		}

		List<PersistableLocation> rcSignLocs = (List<PersistableLocation>) node.getList("controlSigns");
		if (rcSignLocs != null) {
			for (PersistableLocation pl : rcSignLocs) {
				try {
					RedstoneControlSign.getControlSign(pl.getLocation(), this);
				} catch (IllegalStateException e) {
					// world not loaded
					RedstoneControlSign.deferLoading(pl.getWorldName(), new Vector(pl.getX(), pl.getY(), pl.getZ()));
				}
			}
		}
	}

	@Override
	public void onScrolled(Player player, SMSUserAction action) {
		super.onScrolled(player, action);

		RedstoneOutputMode mode = (RedstoneOutputMode) getAttribute(RS_OUTPUT_MODE);
		if (mode == RedstoneOutputMode.SELECTED) {
			updateSwitchPower();
		}
	}

	@Override
	public void onExecuted(Player player) {
		super.onExecuted(player);

		RedstoneOutputMode mode = (RedstoneOutputMode) getAttribute(RS_OUTPUT_MODE);
		if (mode == RedstoneOutputMode.TOGGLE) {
			toggleSwitchPower();
		}
	}

}
