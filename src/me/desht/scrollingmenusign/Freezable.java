package me.desht.scrollingmenusign;

import java.io.File;
import java.util.Map;

public interface Freezable {
	String getName();
	File getSaveFolder();
	Map<String, Object> freeze();
}
