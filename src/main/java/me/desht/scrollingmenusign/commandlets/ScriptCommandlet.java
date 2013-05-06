package me.desht.scrollingmenusign.commandlets;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import me.desht.dhutils.LogUtils;
import me.desht.scrollingmenusign.DirectoryStructure;
import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSValidate;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.views.SMSView;

import org.bukkit.command.CommandSender;

public class ScriptCommandlet extends BaseCommandlet {

	public ScriptCommandlet() {
		super("SCRIPT");
	}

	@Override
	public void execute(ScrollingMenuSign plugin, CommandSender sender, SMSView view, String cmd, String[] args) {
		SMSValidate.isTrue(args.length >= 2, "Usage: " + cmd + " <script-name>");

		ScriptEngineManager manager = new ScriptEngineManager();		
		String scriptName = args[1];
		int idx = scriptName.lastIndexOf('.');
		String ext = scriptName.substring(idx + 1);
		ScriptEngine engine = manager.getEngineByExtension(ext);
		SMSValidate.notNull(engine, "no scripting engine for " + scriptName);
		
		engine.put("view", view);
		engine.put("commandSender", sender);
		File scriptFile = new File(DirectoryStructure.getScriptsFolder(), scriptName);
		try {
			engine.eval(new BufferedReader(new FileReader(scriptFile)));
		} catch (FileNotFoundException e) {
			throw new SMSException("no such script " + scriptName);
		} catch (ScriptException e) {
			LogUtils.warning("Script " + scriptName + " encountered an error:");
			LogUtils.warning("  " + e.getMessage());
			throw new SMSException("script encountered an error (see server log)");
		}
	}

}
