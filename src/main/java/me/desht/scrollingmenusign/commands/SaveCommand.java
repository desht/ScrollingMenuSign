package me.desht.scrollingmenusign.commands;

import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.SMSPersistence;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class SaveCommand extends SMSAbstractCommand {

	public SaveCommand() {
		super("sms save");
		setPermissionNode("scrollingmenusign.commands.save");
		setUsage("/sms save [menus] [macros]");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {

		boolean saveMenus = false;
		boolean saveMacros = false;
		boolean saveAll = false;
		if (args.length == 0) {
			saveAll = true;
		} else {
			for (int i = 0 ; i < args.length; i++) {
				if (args[i].equalsIgnoreCase("menus")) {
					saveMenus = true;
				} else if (args[i].equalsIgnoreCase("macros")) {
					saveMacros = true;
				}
			}
		}
		if (saveAll || saveMenus)
			SMSPersistence.saveMenusAndViews();
		if (saveAll || saveMacros)
			SMSPersistence.saveMacros();

		if (sender != null)
			MiscUtil.statusMessage(sender, "Save complete.");

		return true;
	}

}
