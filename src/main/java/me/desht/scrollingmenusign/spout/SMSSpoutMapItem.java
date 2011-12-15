package me.desht.scrollingmenusign.spout;

import org.getspout.spoutapi.material.MaterialData;
import org.getspout.spoutapi.material.item.GenericItem;

public class SMSSpoutMapItem extends GenericItem {

	public SMSSpoutMapItem(int data) {
		super("Map", MaterialData.map.getRawId(), data);
	}
}
