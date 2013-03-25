package me.desht.scrollingmenusign;

import java.io.File;

import me.desht.dhutils.LogUtils;

public class DirectoryStructure {
	private static File pluginDir;
	private static File dataDir, menusDir, viewsDir, varsDir, macrosDir, imgCacheDir, fontsDir;
	private static File commandFile;

	private static final String dataDirName = "data";
	private static final String menusDirName = "menus";
	private static final String viewsDirName = "views";
	private static final String macrosDirName = "macros";
	private static final String varsDirName = "variables";
	private static final String imgCacheDirName = "imagecache";
	private static final String commandFileName = "commands.yml";
	private static final String fontsDirName = "fonts";

	static void setupDirectoryStructure() {
		pluginDir = ScrollingMenuSign.getInstance().getDataFolder();

		commandFile = new File(pluginDir, commandFileName);
		dataDir = new File(pluginDir, dataDirName);
		menusDir = new File(dataDir, menusDirName);
		viewsDir = new File(dataDir, viewsDirName);
		varsDir = new File(dataDir, varsDirName);
		macrosDir = new File(dataDir, macrosDirName);
		imgCacheDir = new File(pluginDir, imgCacheDirName);
		fontsDir = new File(pluginDir, fontsDirName);

		createDirectory(pluginDir);
		createDirectory(dataDir);
		createDirectory(menusDir);
		createDirectory(viewsDir);
		createDirectory(varsDir);
		createDirectory(macrosDir);
		createDirectory(imgCacheDir);
		createDirectory(fontsDir);
	}

	private static void createDirectory(File dir) {
		if (dir.isDirectory()) {
			return;
		}
		if (!dir.mkdir()) {
			LogUtils.severe("Can't create directory " + dir.getName()); //$NON-NLS-1$
		}
	}

	public static File getCommandFile() {
		return commandFile;
	}

	public static File getPluginFolder() {
		return pluginDir;
	}

	public static File getDataFolder() {
		return dataDir;
	}

	public static File getMenusFolder() {
		return menusDir;
	}

	public static File getMacrosFolder() {
		return macrosDir;
	}

	public static File getViewsFolder() {
		return viewsDir;
	}

	public static File getImgCacheFolder() {
		return imgCacheDir;
	}

	public static File getVarsFolder() {
		return varsDir;
	}

	public static File getFontsFolder() {
		return fontsDir;
	}
}
