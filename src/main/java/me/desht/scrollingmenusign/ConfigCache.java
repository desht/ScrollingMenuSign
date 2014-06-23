package me.desht.scrollingmenusign;

import me.desht.scrollingmenusign.util.SMSUtil;
import org.bukkit.configuration.Configuration;
import org.bukkit.inventory.ItemStack;

/**
 * Cache some config values which are accessed very frequently, to reduce
 * lookup/parsing overheads.
 */
public class ConfigCache {
    private String prefixSelected;
    private String prefixNotSelected;
    private boolean physicsProtected;
    private boolean breakProtected;
    private String submenuBackLabel;
    private ItemStack submenuBackIcon;
    private String submenuTitlePrefix;
    private ItemStack defaultInventoryViewIcon;

    public void processConfig(Configuration conf) {
        setPrefixSelected(conf.getString("sms.item_prefix.selected"));
        setPrefixNotSelected(conf.getString("sms.item_prefix.not_selected"));
        setPhysicsProtected(conf.getBoolean("sms.no_physics"));
        setBreakProtected(conf.getBoolean("sms.no_destroy_signs"));
        setSubmenuBackLabel(conf.getString("sms.submenus.back_item.label"));
        setSubmenuBackIcon(conf.getString("sms.submenus.back_item.material"));
        setSubmenuTitlePrefix(conf.getString("sms.submenus.title_prefix"));
        setDefaultInventoryViewIcon(conf.getString("sms.inv_view.default_icon"));
    }

    public String getPrefixSelected() {
        return prefixSelected;
    }

    public void setPrefixSelected(String prefixSelected) {
        this.prefixSelected = SMSUtil.unEscape(prefixSelected.replace("%", "%%"));
    }

    public String getPrefixNotSelected() {
        return prefixNotSelected;
    }

    public void setPrefixNotSelected(String prefixNotSelected) {
        this.prefixNotSelected = SMSUtil.unEscape(prefixNotSelected.replace("%", "%%"));
    }

    public boolean isPhysicsProtected() {
        return physicsProtected;
    }

    public void setPhysicsProtected(boolean physicsProtected) {
        this.physicsProtected = physicsProtected;
    }

    public boolean isBreakProtected() {
        return breakProtected;
    }

    public void setBreakProtected(boolean breakProtected) {
        this.breakProtected = breakProtected;
    }

    public String getSubmenuBackLabel() {
        return submenuBackLabel;
    }

    public void setSubmenuBackLabel(String submenuBackLabel) {
        this.submenuBackLabel = SMSUtil.unEscape(submenuBackLabel);
    }

    public ItemStack getSubmenuBackIcon() {
        return submenuBackIcon;
    }

    public void setSubmenuBackIcon(String submenuBackIcon) {
        this.submenuBackIcon = SMSUtil.parseMaterialSpec(submenuBackIcon);
    }

    public String getSubmenuTitlePrefix() {
        return submenuTitlePrefix;
    }

    public void setSubmenuTitlePrefix(String submenuTitlePrefix) {
        this.submenuTitlePrefix = SMSUtil.unEscape(submenuTitlePrefix);
    }

    public ItemStack getDefaultInventoryViewIcon() {
        return defaultInventoryViewIcon;
    }

    public void setDefaultInventoryViewIcon(String defaultInventoryViewIcon) {
        this.defaultInventoryViewIcon = SMSUtil.parseMaterialSpec(defaultInventoryViewIcon);
    }
}
