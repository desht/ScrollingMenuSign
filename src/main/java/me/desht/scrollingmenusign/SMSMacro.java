package me.desht.scrollingmenusign;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.bukkit.configuration.ConfigurationSection;

/**
 * @author desht
 *
 */
public class SMSMacro implements SMSPersistable {
	//	private static Configuration cmdSet;
	private final static Map<String, SMSMacro> allMacros = new HashMap<String, SMSMacro>();

	private final String macroName;
	private final List<String> macroDefinition;

	/**
	 * Create a new macro object.
	 * 
	 * @param macroName		Name of the new macro	
	 */
	SMSMacro(String macroName) {
		this.macroName = macroName;
		this.macroDefinition = new ArrayList<String>();
	}

	SMSMacro(ConfigurationSection node) {
		this.macroName = node.getString("name");
		this.macroDefinition = node.getStringList("definition");
	}

	/**
	 * Add a line to the macro.
	 * 
	 * @param line		Line to add
	 */
	public void addLine(String line) {
		macroDefinition.add(line);
	}

	/**
	 * Insert a line in the macro at the given position
	 * 
	 * @param line		Line to add
	 * @param index		Position at which to insert
	 */
	public void addLine(String line, int index) {
		macroDefinition.add(index, line);
	}

	/**
	 * Remove the line at the given index
	 * 
	 * @param index		Position at which to remove
	 */
	public void removeLine(int index) {
		macroDefinition.remove(index);
	}

	/**
	 * Remove the line matching the given text
	 * 
	 * @param line	Text to match
	 */	
	public void removeLine(String line) {
		macroDefinition.remove(line);
	}

	/**
	 * Get a list of the lines for the macro
	 * 
	 * @return	A list of strings
	 */
	public List<String>	getLines() {
		return macroDefinition;
	}

	/* (non-Javadoc)
	 * @see me.desht.scrollingmenusign.Freezable#freeze()
	 */
	@Override
	public Map<String, Object> freeze() {
		Map<String, Object> map = new HashMap<String, Object>();

		map.put("name", macroName);
		map.put("definition", macroDefinition);

		return map;
	}

	/* (non-Javadoc)
	 * @see me.desht.scrollingmenusign.Freezable#getName()
	 */
	@Override
	public String getName() {
		return macroName;
	}

	/* (non-Javadoc)
	 * @see me.desht.scrollingmenusign.Freezable#getSaveFolder()
	 */
	@Override
	public File getSaveFolder() {
		return DirectoryStructure.getMacrosFolder();
	}

	private void deleteCommon() {
		allMacros.remove(macroName);
	}

	void deletePermanent() {
		deleteCommon();
		SMSPersistence.unPersist(this);
	}

	void deleteTemporary() {
		deleteCommon();
	}

	void autosave() {
		SMSPersistence.save(this);
	}

	static void addMacro(SMSMacro m) {
		allMacros.put(m.getName(), m);
		m.autosave();
	}

	/**
	 * Retrieve the macro with the given name.  Equivalent to calling getMacro(macroName, false)
	 * 
	 * @param macroName		Name of macro to get
	 * @return	The macro object
	 * @throws SMSException		if there is no macro of that name
	 */
	public static SMSMacro getMacro(String macroName) throws SMSException {
		return getMacro(macroName, false);	
	}

	/**
	 * @param macroName 	Name of macro to get
	 * @param autoCreate	If true and there is no macro of that name, create a new macro
	 * @return	The macro object
	 * @throws SMSException if autoCreate is false and there is no macro of that name
	 */
	public static SMSMacro getMacro(String macroName, boolean autoCreate) throws SMSException {
		if (!allMacros.containsKey(macroName)) {
			if (autoCreate) {
				addMacro(new SMSMacro(macroName));
			} else {
				throw new SMSException("No such macro " + macroName);
			}
		}

		return allMacros.get(macroName);
	}

	/**
	 * Add a command to a macro.  If the macro does not exist,
	 * a new empty macro will be automatically created.
	 * 
	 * @param macro		The macro to add the command to
	 * @param cmd		The command to add
	 */
	public static void addCommand(String macro, String cmd) {
		try {
			SMSMacro m = getMacro(macro, true);
			m.addLine(cmd);
			m.autosave();
		} catch (SMSException e) {
			// should not get here
			e.printStackTrace();
		}
	}

	/**
	 * Add a command to a macro, at a given position.  If the macro does not exist,
	 * a new empty macro will be automatically created.
	 * 
	 * @param macro		The macro to add the command to
	 * @param cmd		The command to add
	 * @param index		The index at which to add the command (0 is start of the macro)
	 */
	public static void insertCommand(String macro, String cmd, int index) {
		try {
			SMSMacro m = getMacro(macro, true);
			m.addLine(cmd, index);
			m.autosave();
		} catch (SMSException e) {
			// should not get here
			e.printStackTrace();
		}
	}

	/**
	 * Get a list of all known macro names.
	 * 
	 * @return	A list of strings, each of which is a macro name
	 */
	public static List<String> getMacros() {
		return new ArrayList<String>(allMacros.keySet());
	}

	/**
	 * Return an unsorted list of all the known macros
	 * Equivalent to calling <b>listMacros(false)</b>
	 * @return A list of SMSMacro objects
	 */
	public static List<SMSMacro> listMacros() {
		return listMacros(false);
	}

	/**
	 * Return a list of all the known macros
	 * 
	 * @param isSorted		Whether or not to sort the macros by name
	 * @return	A list of SMSMacro objects
	 */
	public static List<SMSMacro> listMacros(boolean isSorted) {
		if (isSorted) {
			SortedSet<String> sorted = new TreeSet<String>(allMacros.keySet());
			List<SMSMacro> res = new ArrayList<SMSMacro>();
			for (String name : sorted) {
				res.add(allMacros.get(name));
			}
			return res;
		} else {
			return new ArrayList<SMSMacro>(allMacros.values());
		}
	}

	/**
	 * Check to see if the given macro exists.
	 * 
	 * @param macro		The macro to check for
	 * @return	True if the macro exists, false otherwise
	 */
	public static boolean hasMacro(String macro) {
		return allMacros.containsKey(macro);
	}

	/**
	 * Get a list of the commands in the given macro.  If the macro does not exist,
	 * a new empty macro will be automatically created.
	 * 
	 * @param macro		The macro to check
	 * @return	A list of strings, each of which is a command in the macro
	 */
	public static List<String> getCommands(String macro) {
		try {
			return getMacro(macro, true).getLines();
		} catch (SMSException e) {
			// should not get here
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Remove a macro.
	 * 
	 * @param macro		The macro to remove
	 */
	public static void removeMacro(String macro) {
		try {
			SMSMacro m = getMacro(macro, true);
			m.deletePermanent();
		} catch (SMSException e) {
			// should not get here
			e.printStackTrace();
		}
	}

	/**
	 * Remove a command from a macro.
	 * 
	 * @param macro		The macro to modify
	 * @param index		The index of the command to remove (0 is the first command)
	 */
	public static void removeCommand(String macro, int index) {
		try {
			SMSMacro m = getMacro(macro, true);
			m.removeLine(index);
			m.autosave();
		} catch (SMSException e) {
			// should not get here
			e.printStackTrace();
		}
	}
}
