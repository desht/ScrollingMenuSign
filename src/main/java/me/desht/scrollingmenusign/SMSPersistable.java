package me.desht.scrollingmenusign;

import java.io.File;
import java.util.Map;

public interface SMSPersistable {
	/**
	 * Get the unique name for this freezable object.
	 * @return	The object's unique name.
	 */
	String getName();
	/**
	 * Get the folder on disk where this object's data will be stored.
	 * @return	A File object representing the folder.
	 */
	File getSaveFolder();
	/**
	 * Freeze (serialise) the object's data into a map, keyed by the object's attributes.
	 * The object will implement a constructor which thaws this map back to the original
	 * object.
	 * 
	 * @return	a Map of object attributes
	 */
	Map<String, Object> freeze();
}
