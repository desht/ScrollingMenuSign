package me.desht.scrollingmenusign.commandlets;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

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
	public boolean execute(ScrollingMenuSign plugin, CommandSender sender, SMSView view, String cmd, String[] args) {
		SMSValidate.isTrue(args.length >= 2, "Usage: " + cmd + " <script-name> [<script-args>]");

		ScriptEngineManager manager = new ScriptEngineManager();
		String scriptName = args[1];
		int idx = scriptName.lastIndexOf('.');
		String ext = scriptName.substring(idx + 1);
		ScriptEngine engine = manager.getEngineByExtension(ext);
		SMSValidate.notNull(engine, "no scripting engine for " + scriptName);
		LogUtils.fine("running script " + scriptName + " with " + engine.getFactory().getEngineName());

		Bindings bindings = new SimpleBindings();
		if (args.length > 2) {
			String[] scriptArgs = new String[args.length - 2];
			for (int i = 0; i < scriptArgs.length; i++) {
				scriptArgs[i] = args[i+2];
			}
			bindings.put("args", scriptArgs);
		} else {
			bindings.put("args", new String[0]);
		}
		bindings.put("view", view);
		bindings.put("commandSender", sender);
		bindings.put("result", true);
		File scriptFile = new File(DirectoryStructure.getScriptsFolder(), scriptName);
		boolean retval = true;
		try {
			engine.eval(new BufferedReader(new FileReader(scriptFile)), bindings);
			Object o = bindings.get("result");
			if (o instanceof Boolean) {
				retval = (Boolean) o;
				LogUtils.fine("script " + scriptName + " returns: " + retval);
			}
		} catch (FileNotFoundException e) {
			throw new SMSException("no such script " + scriptName);
		} catch (ScriptException e) {
			LogUtils.warning("Script " + scriptName + " encountered an error:");
			LogUtils.warning("  " + e.getMessage());
			throw new SMSException("script encountered an error (see server log)");
		}
		return retval;
	}
}
