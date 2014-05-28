package me.desht.scrollingmenusign;

import me.desht.dhutils.ItemGlow;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PermissionUtils;
import me.desht.scrollingmenusign.enums.ReturnStatus;
import me.desht.scrollingmenusign.parser.CommandUtils;
import me.desht.scrollingmenusign.util.SMSUtil;
import me.desht.scrollingmenusign.views.CommandTrigger;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

import java.util.*;

public class SMSMenuItem implements Comparable<SMSMenuItem>, SMSUseLimitable {
    private final String label;
    private final String command;
    private final String message;
    private final List<String> lore;
    private final ItemStack icon;
    private final String altCommand;
    private final String permissionNode;
    private SMSRemainingUses uses;
    private final SMSMenu menu;

    public SMSMenuItem(SMSMenu menu, String label, String command, String message) {
        this(menu, label, command, message, null);
    }

    public SMSMenuItem(SMSMenu menu, String label, String command, String message, String iconMaterialName) {
        this(menu, label, command, message, iconMaterialName, new String[0]);
    }

    public SMSMenuItem(SMSMenu menu, String label, String command, String message, String iconMaterialName, String[] lore) {
        Validate.notNull(menu, "menu may not be null");
        Validate.notNull(label, "label may not be null");
        Validate.notNull(command, "command may not be null");

        this.menu = menu;
        this.label = label;
        this.command = command;
        this.altCommand = "";
        this.message = message;
        this.permissionNode = "";
        try {
            this.icon = SMSUtil.parseMaterialSpec(iconMaterialName);
        } catch (IllegalArgumentException e) {
            throw new SMSException("invalid material '" + iconMaterialName + "'");
        }
        this.lore = new ArrayList<String>();
        for (String l : lore) {
            this.lore.add(MiscUtil.parseColourSpec(l));
        }
        this.uses = new SMSRemainingUses(this);
    }

    public SMSMenuItem(SMSMenu menu, ConfigurationSection node) throws SMSException {
        SMSPersistence.mustHaveField(node, "label");
        SMSPersistence.mustHaveField(node, "command");
        SMSPersistence.mustHaveField(node, "message");

        this.menu = menu;
        this.label = SMSUtil.unEscape(node.getString("label"));
        this.command = StringEscapeUtils.unescapeHtml(node.getString("command"));
        this.altCommand = StringEscapeUtils.unescapeHtml(node.getString("altCommand", ""));
        this.message = SMSUtil.unEscape(node.getString("message"));
        this.icon = SMSUtil.parseMaterialSpec(node.getString("icon"));
        this.uses = new SMSRemainingUses(this, node.getConfigurationSection("usesRemaining"));
        this.lore = new ArrayList<String>();
        this.permissionNode = node.getString("permission", "");
        if (node.contains("lore")) {
            for (String l : node.getStringList("lore")) {
                lore.add(SMSUtil.unEscape(l));
            }
        }
    }

    private SMSMenuItem(Builder builder) {
        this.menu = builder.menu;
        this.label = builder.label;
        this.message = builder.message;
        this.lore = builder.lore;
        this.command = builder.command;
        this.altCommand = builder.altCommand;
        this.permissionNode = builder.permissionNode;
        this.icon = builder.icon;
        if (builder.glow && builder.icon != null && ScrollingMenuSign.getInstance().isProtocolLibEnabled()) {
            ItemGlow.setGlowing(this.icon, true);
        }
        this.uses = builder.uses == null ? new SMSRemainingUses(this) : builder.uses;
    }

    /**
     * Get a string representation of the icon's material name.
     *
     * @return the icon material & data as a string
     * @deprecated call {@code toString()} on {@link #getIconMaterial()} if you really need this
     */
    @Deprecated
    public String getIconMaterialName() {
        return getIconMaterial() == null ? null : getIconMaterial().toString();
    }

    /**
     * Get the label for this menu item
     *
     * @return The label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Get the label for this menu item with all colour codes removed
     *
     * @return The label
     */
    public String getLabelStripped() {
        return ChatColor.stripColor(label);
    }

    /**
     * Get the command for this menu item
     *
     * @return The command
     */
    public String getCommand() {
        return command;
    }

    /**
     * Get the alternative (secondary) command for this menu item
     *
     * @return the alternative command
     */
    public String getAltCommand() {
        return altCommand;
    }

    /**
     * Get the feedback message for this menu item
     *
     * @return The feedback message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Return the material used for this menu item's icon, in those views which
     * support icons.
     *
     * @return the material used for the menu item's icon
     */
    public MaterialData getIconMaterial() {
        return hasIcon() ? icon.getData() : null;
    }

    /**
     * Check if this menu item has an icon defined.
     *
     * @return true if the item has an icon, false otherwise
     */
    public boolean hasIcon() {
        return icon != null;
    }

    /**
     * Get the item's icon as an item stack.
     *
     * @return a copy of the itme's icon
     */
    public ItemStack getIcon() {
        return icon == null ? null : icon.clone();
    }

    /**
     * Get the lore (tooltip) for this menu item.  Note that not all view types necessarily support
     * display of lore.
     *
     * @return the lore for the menu item, as a String array
     */
    public String[] getLore() {
        return lore.toArray(new String[lore.size()]);
    }

    /**
     * Get the lore (tooltip) for this menu item.  Note that not all view types necessarily support
     * display of lore.
     *
     * @return the lore for the menu item, as a list of String
     */
    public List<String> getLoreAsList() {
        return new ArrayList<String>(lore);
    }

    /**
     * Append a line of text to the item's lore.
     *
     * @param lore the lore text to append
     */
    public void appendLore(String lore) {
        this.lore.add(lore);
    }

    /**
     * Replace the item's lore with a line of text.
     *
     * @param lore the new lore text for the item
     */
    public void setLore(String lore) {
        this.lore.clear();
        this.lore.add(lore);
    }

    /**
     * Get the item's permission node, if any.
     *
     * @return the item's permission node, may be an empty string
     */
    public String getPermissionNode() {
        return permissionNode;
    }

    /**
     * Executes the command for this item
     *
     * @param sender  the command sender who triggered the execution
     * @param trigger the view that triggered this execution
     * @throws SMSException if the usage limit for this player is exhausted
     */
    public void executeCommand(CommandSender sender, CommandTrigger trigger) {
        executeCommand(sender, trigger, false);
    }

    /**
     * Executes the command for this item
     *
     * @param sender  the command sender who triggered the execution
     * @param trigger the view that triggered this execution
     * @param alt     true if the item's alternate command should be executed
     * @throws SMSException if the usage limit for this player is exhausted
     */
    public void executeCommand(CommandSender sender, CommandTrigger trigger, boolean alt) {
        SMSValidate.isTrue(hasPermission(sender), "That item is not available to you.");

        String cmd = getCommand();
        if (alt && !getAltCommand().isEmpty()) {
            cmd = getAltCommand();
        }
        boolean itemUses = false, menuUses = false;
        if (sender instanceof Player) {
            itemUses = verifyRemainingUses(this, (Player) sender);
            menuUses = verifyRemainingUses(menu, (Player) sender);
        }

        if ((cmd == null || cmd.isEmpty()) && !menu.getDefaultCommand().isEmpty()) {
            cmd = menu.getDefaultCommand().replace("<LABEL>", ChatColor.stripColor(getLabel())).replace("<RAWLABEL>", getLabel());
        }

        CommandUtils.executeCommand(sender, cmd, trigger);
        ReturnStatus rs = CommandUtils.getLastReturnStatus();

        if (rs == ReturnStatus.CMD_OK || rs == ReturnStatus.UNKNOWN) {
            if (itemUses) {
                decrementRemainingUses(this, (Player) sender);
            }
            if (menuUses) {
                decrementRemainingUses(menu, (Player) sender);
            }
            if (itemUses || menuUses) {
                menu.autosave();
            }
        }
    }

    /**
     * Executes the command for this item
     *
     * @param sender the command sender who triggered the execution
     * @throws SMSException if the usage limit for this player is exhausted
     */
    public void executeCommand(CommandSender sender) {
        executeCommand(sender, null);
    }

    /**
     * Verify that the given object (item or menu) has not exhausted its usage limits.
     *
     * @param useLimitable the menu or item to check
     * @param player       the player to check
     * @return true if there is a valid usage limit, false if the item has no usage limits at all
     * @throws SMSException if the usage limits for the item were already exhausted
     */
    private boolean verifyRemainingUses(SMSUseLimitable useLimitable, Player player) throws SMSException {
        SMSRemainingUses limits = useLimitable.getUseLimits();

        if (limits.hasLimitedUses()) {
            String desc = limits.getDescription();
            if (limits.getRemainingUses(player) <= 0) {
                throw new SMSException("You can't use that " + desc + " anymore.");
            }
            return true;
        } else {
            return false;
        }
    }

    private void decrementRemainingUses(SMSUseLimitable useLimitable, Player player) {
        SMSRemainingUses limits = useLimitable.getUseLimits();

        if (limits.hasLimitedUses()) {
            String desc = limits.getDescription();
            limits.use(player);
            if ((Boolean) menu.getAttributes().get(SMSMenu.REPORT_USES)) {
                MiscUtil.statusMessage(player, "&6&o[Uses remaining for this " + desc + ": &e&o" + limits.getRemainingUses(player) + "&6&o]");
            }
        }
    }

    /**
     * Displays the feedback message for this menu item
     *
     * @param player Player to show the message to
     */
    public void feedbackMessage(Player player) {
        if (player != null) {
            sendFeedback(player, getMessage());
        }
    }

    private void sendFeedback(Player player, String message) {
        sendFeedback(player, message, new HashSet<String>());
    }

    private void sendFeedback(Player player, String message, Set<String> history) {
        if (message == null || message.length() == 0)
            return;
        if (message.startsWith("%")) {
            // macro expansion
            String macro = message.substring(1);
            if (history.contains(macro)) {
                LogUtils.warning("Recursive loop detected in macro [" + macro + "]!");
                throw new SMSException("Recursive loop detected in macro [" + macro + "]!");
            } else if (SMSMacro.hasMacro(macro)) {
                history.add(macro);
                sendFeedback(player, SMSMacro.getCommands(macro), history);
            } else {
                throw new SMSException("No such macro [" + macro + "].");
            }
        } else {
            MiscUtil.alertMessage(player, message);
        }
    }

    private void sendFeedback(Player player, List<String> messages, Set<String> history) {
        for (String m : messages) {
            sendFeedback(player, m, history);
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "SMSMenuItem [label=" + label + ", command=" + command + ", message=" + message + ", icon=" + icon + "]";
    }

    /**
     * Get the remaining use details for this menu item
     *
     * @return The remaining use details
     */
    @Override
    public SMSRemainingUses getUseLimits() {
        return uses;
    }

    @Override
    public String getLimitableName() {
        return menu.getName() + "/" + getLabelStripped();
    }

    /**
     * Sets the remaining use details for this menu item.
     *
     * @param uses the remaining use details
     */
    public void setUseLimits(SMSRemainingUses uses) {
        this.uses = uses;
    }

    /**
     * Returns a printable representation of the number of uses remaining for this item.
     *
     * @return Formatted usage information
     */
    String formatUses() {
        return uses.toString();
    }

    /**
     * Returns a printable representation of the number of uses remaining for this item, for the given player.
     *
     * @param sender Player to retrieve the usage information for
     * @return Formatted usage information
     */
    @Override
    public String formatUses(CommandSender sender) {
        if (sender instanceof Player) {
            return uses.toString((Player) sender);
        } else {
            return formatUses();
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((command == null) ? 0 : command.hashCode());
        result = prime * result + ((label == null) ? 0 : label.hashCode());
        result = prime * result + ((message == null) ? 0 : message.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SMSMenuItem other = (SMSMenuItem) obj;
        if (command == null) {
            if (other.command != null)
                return false;
        } else if (!command.equals(other.command))
            return false;
        if (label == null) {
            if (other.label != null)
                return false;
        } else if (!label.equals(other.label))
            return false;
        if (message == null) {
            if (other.message != null)
                return false;
        } else if (!message.equals(other.message))
            return false;
        return true;
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     *
     * Two menu items are equal if their labels are the same.  Colour codes do not count, only the text.
     */
    @Override
    public int compareTo(SMSMenuItem other) {
        return getLabelStripped().compareToIgnoreCase(other.getLabelStripped());
    }

    Map<String, Object> freeze() {
        Map<String, Object> map = new HashMap<String, Object>();

        map.put("label", SMSUtil.escape(label));
        map.put("command", SMSUtil.escape(command));
        map.put("altCommand", SMSUtil.escape(altCommand));
        map.put("message", SMSUtil.escape(message));
        if (hasIcon()) {
            map.put("icon", SMSUtil.freezeMaterialSpec(getIcon()));
        }
        map.put("usesRemaining", uses.freeze());
        List<String> lore2 = new ArrayList<String>(lore.size());
        for (String l : lore) {
            lore2.add(SMSUtil.escape(l));
        }
        map.put("lore", lore2);
        map.put("permission", permissionNode);
        return map;
    }

    public void autosave() {
        if (menu != null)
            menu.autosave();
    }

    @Override
    public String getDescription() {
        return "menu item";
    }

    SMSMenuItem uniqueItem() {
        if (menu.getItem(getLabelStripped()) == null) {
            return this;
        }
        // the label already exists in this menu - try to get a unique one
        int n = 0;
        String ls;
        do {
            n++;
            ls = getLabelStripped() + "-" + n;
        } while (menu.getItem(ls) != null);

        return new SMSMenuItem.Builder(menu, label)
                .withCommand(getCommand())
                .withMessage(getMessage())
                .withIcon(getIcon())
                .withAltCommand(getAltCommand())
                .withLore(getLore())
                .withUseLimits(getUseLimits())
                .withPermissionNode(getPermissionNode())
                .build();
    }

    /**
     * Check if the given player has permission to use this specific menu item,
     * as defined by its permission node (see {@link #getPermissionNode()}).
     * Note that this only gives the player permission to execute the menu item,
     * not necessarily permission for any command that may be run.
     *
     * @param sender the player to check
     * @return true if the player may use this item, false otherwise
     */
    public boolean hasPermission(CommandSender sender) {
        return sender == null || permissionNode.isEmpty() || PermissionUtils.isAllowedTo(sender, permissionNode);
    }

    public static class Builder {
        private final SMSMenu menu;
        private final String label;
        private String command = "";
        private String altCommand = "";
        private String message = "";
        private List<String> lore = Collections.emptyList();
        private ItemStack icon;
        private boolean glow;
        private String permissionNode = "";
        private SMSRemainingUses uses;

        public Builder(SMSMenu menu, String label) {
            this.menu = menu;
            this.label = label;
        }

        public Builder withCommand(String command) {
            this.command = command;
            return this;
        }

        public Builder withMessage(String message) {
            this.message = message;
            return this;
        }

        public Builder withAltCommand(String altCommand) {
            this.altCommand = altCommand;
            return this;
        }

        public Builder withLore(String... lore) {
            this.lore = Arrays.asList(lore);
            return this;
        }

        public Builder withLore(List<String> lore) {
            this.lore = new ArrayList<String>(lore);
            return this;
        }

        public Builder withIcon(MaterialData icon) {
            this.icon = icon.toItemStack();
            return this;
        }

        public Builder withIcon(ItemStack icon) {
            this.icon = icon;
            return this;
        }

        public Builder withIcon(String iconMaterialName) {
            try {
                this.icon = SMSUtil.parseMaterialSpec(iconMaterialName);
            } catch (IllegalArgumentException e) {
                throw new SMSException("invalid material '" + iconMaterialName + "'");
            }
            return this;
        }

        public Builder withGlow(boolean glow) {
            this.glow = glow;
            return this;
        }

        public Builder withPermissionNode(String permissionNode) {
            this.permissionNode = permissionNode;
            return this;
        }

        public Builder withUseLimits(SMSRemainingUses uses) {
            this.uses = uses;
            return this;
        }

        public SMSMenuItem build() {
            return new SMSMenuItem(this);
        }
    }
}
