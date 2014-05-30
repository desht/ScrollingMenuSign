package me.desht.scrollingmenusign;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import me.desht.dhutils.Debugger;
import me.desht.dhutils.LogUtils;
import me.desht.scrollingmenusign.enums.SMSMenuAction;
import me.desht.scrollingmenusign.variables.SMSVariables;
import me.desht.scrollingmenusign.variables.VariablesManager;
import me.desht.scrollingmenusign.views.SMSView;
import me.desht.scrollingmenusign.views.ViewManager;

import me.desht.scrollingmenusign.views.ViewUpdateAction;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class SMSPersistence {

    private static final FilenameFilter ymlFilter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith(".yml");
        }
    };

    public static void unPersist(SMSPersistable object) {
        File saveFile = new File(object.getSaveFolder(), object.getName() + ".yml");
        if (!saveFile.delete()) {
            LogUtils.warning("can't delete " + saveFile);
        }
    }

    public static void save(SMSPersistable object) {
        File saveFile = new File(object.getSaveFolder(), object.getName() + ".yml");
        YamlConfiguration conf = new YamlConfiguration();
        Map<String, Object> map = object.freeze();
        expandMapIntoConfig(conf, map);
        try {
            conf.save(saveFile);
        } catch (IOException e) {
            LogUtils.severe("Can't save " + saveFile + ": " + e.getMessage());
        }
    }

    /**
     * Given a (possibly) nested map object, expand it into a ConfigurationSection,
     * using recursion if necessary.
     *
     * @param conf The ConfigurationSection to put the map into
     * @param map  The map object
     */
    @SuppressWarnings("unchecked")
    public static void expandMapIntoConfig(ConfigurationSection conf, Map<String, Object> map) {
        for (Entry<String, Object> e : map.entrySet()) {
            if (e.getValue() instanceof Map<?, ?>) {
                ConfigurationSection section = conf.createSection(e.getKey());
                Map<String, Object> subMap = (Map<String, Object>) e.getValue();
                expandMapIntoConfig(section, subMap);
            } else {
                conf.set(e.getKey(), e.getValue());
            }
        }
    }

    public static void loadMacros() {
        for (SMSMacro macro : SMSMacro.listMacros()) {
            macro.deleteTemporary();
        }

        for (File f : DirectoryStructure.getMacrosFolder().listFiles(ymlFilter)) {
            YamlConfiguration conf = YamlConfiguration.loadConfiguration(f);
            SMSMacro m = new SMSMacro(conf);
            SMSMacro.addMacro(m);
        }
        Debugger.getInstance().debug("Loaded " + SMSMacro.listMacros().size() + " macros from file.");
    }

    public static void loadVariables() {
        VariablesManager vm = ScrollingMenuSign.getInstance().getVariablesManager();

        vm.clear();
        for (File f : DirectoryStructure.getVarsFolder().listFiles(ymlFilter)) {
            vm.load(f);
        }
        Debugger.getInstance().debug("Loaded " + vm.listVariables().size() + " variable sets from file.");
    }

    public static void saveMenusAndViews() {
        MenuManager mm = ScrollingMenuSign.getInstance().getMenuManager();
        ViewManager vm = ScrollingMenuSign.getInstance().getViewManager();

        for (SMSMenu menu : mm.listMenus()) {
            save(menu);
        }
        for (SMSView view : vm.listViews()) {
            save(view);
        }
        Debugger.getInstance().debug("saved " + mm.listMenus().size() + " menus and " +
                vm.listViews().size() + " views to file.");
    }

    public static void saveMacros() {
        for (SMSMacro macro : SMSMacro.listMacros()) {
            save(macro);
        }
        Debugger.getInstance().debug("saved " + SMSMacro.listMacros().size() + " macros to file.");
    }

    public static void saveVariables() {
        VariablesManager vm = ScrollingMenuSign.getInstance().getVariablesManager();
        for (SMSVariables variables : vm.listVariables()) {
            save(variables);
        }
        Debugger.getInstance().debug("saved " + vm.listVariables().size() + " variable sets to file.");
    }

    public static void loadMenus() {
        MenuManager mm = ScrollingMenuSign.getInstance().getMenuManager();

        for (SMSMenu menu : mm.listMenus()) {
            menu.deleteTemporary();
        }

        for (File f : DirectoryStructure.getMenusFolder().listFiles(ymlFilter)) {
            try {
                Debugger.getInstance().debug(2, "loading menu: " + f);
                YamlConfiguration conf = YamlConfiguration.loadConfiguration(f);
                SMSMenu menu = new SMSMenu(conf);
                mm.registerMenu(menu.getName(), menu);
            } catch (SMSException e) {
                LogUtils.severe("Can't load menu data from " + f + ": " + e.getMessage());
            }
        }
        Debugger.getInstance().debug("Loaded " + mm.listMenus().size() + " menus from file.");
    }

    public static void loadViews() {
        ViewManager vm = ScrollingMenuSign.getInstance().getViewManager();

        for (SMSView view : vm.listViews()) {
            vm.deleteView(view, false);
        }

        for (File f : DirectoryStructure.getViewsFolder().listFiles(ymlFilter)) {
            Debugger.getInstance().debug(2, "loading view: " + f);
            YamlConfiguration conf = YamlConfiguration.loadConfiguration(f);
            SMSView view = vm.loadView(conf);
            if (view != null) {
                view.getNativeMenu().addObserver(view);
                view.update(view.getNativeMenu(), new ViewUpdateAction(SMSMenuAction.REPAINT));
            }
        }

        Debugger.getInstance().debug("Loaded " + vm.listViews().size() + " views from file.");
    }

    /**
     * Require the presence of the given field in the given configuration.
     *
     * @param node  The node to check
     * @param field The field to check for
     * @throws SMSException if the field is not present in the configuration
     */
    public static void mustHaveField(ConfigurationSection node, String field) throws SMSException {
        SMSValidate.isTrue(node.contains(field), "Field '" + field + "' missing - corrupted save file?");
    }
}
