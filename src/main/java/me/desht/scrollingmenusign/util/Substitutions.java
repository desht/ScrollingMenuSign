package me.desht.scrollingmenusign.util;

import me.desht.dhutils.ExperienceManager;
import me.desht.dhutils.ItemNames;
import me.desht.dhutils.LogUtils;
import me.desht.scrollingmenusign.SMSValidate;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.commandlets.CooldownCommandlet;
import me.desht.scrollingmenusign.parser.SubstitutionHandler;
import me.desht.scrollingmenusign.variables.VariablesManager;
import me.desht.scrollingmenusign.views.CommandTrigger;
import me.desht.scrollingmenusign.views.SMSView;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Substitutions {
    private static final Pattern userVarSubPat = Pattern.compile("<\\$([A-Za-z0-9_\\.]+)(=.*?)?>");
    private static final Pattern viewVarSubPat = Pattern.compile("<\\$v:([A-Za-z0-9_\\.]+)=(.*?)>");
    private static final Pattern predefSubPat = Pattern.compile("<([A-Z]+)>");

    private static final Map<String, SubstitutionHandler> subs = new HashMap<String, SubstitutionHandler>();

    static {
        setupBuiltinSubHandlers();
    }

    /**
     * Substitute any user-defined variables (/sms var) in the given string.
     *
     * @param player  the player running the command
     * @param command The command string
     * @param missing (returned) a set of variable names with no definitions,
     *                may be null
     * @return The substituted command string
     */
    public static String userVariableSubs(Player player, String command, Set<String> missing) {
        Matcher m = userVarSubPat.matcher(command);
        StringBuffer sb = new StringBuffer(command.length());
        VariablesManager vm = ScrollingMenuSign.getInstance().getVariablesManager();
        while (m.find()) {
            String repl = vm.get(player, m.group(1));
            if (repl == null && m.groupCount() > 1 && m.group(2) != null) {
                repl = m.group(2).substring(1);
            }
            if (repl == null) {
                if (missing == null) {
                    m.appendReplacement(sb, Matcher.quoteReplacement("???"));
                } else {
                    missing.add(m.group(1));
                }
            } else {
                m.appendReplacement(sb, Matcher.quoteReplacement(repl));
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }


    /**
     * Substitute any view-specific variables in the given string.
     *
     * @param view the view
     * @param str the input string
     * @return the substituted string
     */
    public static String viewVariableSubs(SMSView view, String str) {
        Matcher m = viewVarSubPat.matcher(str);
        StringBuffer sb = new StringBuffer(str.length());
        while (m.find()) {
            String repl = view != null && view.checkVariable(m.group(1)) ? view.getVariable(m.group(1)) : m.group(2);
            m.appendReplacement(sb, Matcher.quoteReplacement(repl));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Perform predefined substitutions on the supplied string.
     *
     * @param player the player object
     * @param str the input string
     * @param trigger the command trigger
     * @param missing (returned) a set of variable names with no definitions,
     *                may be null
     * @return the substituted string
     */
    public static String predefSubs(Player player, String str, CommandTrigger trigger, Set<String> missing) {
        Matcher m = predefSubPat.matcher(str);
        StringBuffer sb = new StringBuffer(str.length());
        while (m.find()) {
            String key = m.group(1);
            if (subs.containsKey(key)) {
                String repl = subs.get(key).sub(player, trigger);
                m.appendReplacement(sb, Matcher.quoteReplacement(repl));
            } else {
                if (missing != null) {
                    missing.add(key);
                }
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public static void addSubstitutionHandler(String sub, SubstitutionHandler handler) {
        SMSValidate.isFalse(subs.containsKey(sub), "A handler is already registered for " + sub);
        SMSValidate.isTrue(sub.matches("^[A-Z]+$"), "Substitution string must be all uppercase alphabetic");
        subs.put(sub, handler);
    }

    private static void setupBuiltinSubHandlers() {
        subs.put("X", new SubstitutionHandler() {
            @Override
            public String sub(Player player, CommandTrigger trigger) {
                return Integer.toString(player.getLocation().getBlockX());
            }
        });
        subs.put("Y", new SubstitutionHandler() {
            @Override
            public String sub(Player player, CommandTrigger trigger) {
                return Integer.toString(player.getLocation().getBlockY());
            }
        });
        subs.put("Z", new SubstitutionHandler() {
            @Override
            public String sub(Player player, CommandTrigger trigger) {
                return Integer.toString(player.getLocation().getBlockZ());
            }
        });
        subs.put("NAME", new SubstitutionHandler() {
            @Override
            public String sub(Player player, CommandTrigger trigger) {
                return player.getName();
            }
        });
        subs.put("DNAME", new SubstitutionHandler() {
            @Override
            public String sub(Player player, CommandTrigger trigger) {
                return player.getDisplayName();
            }
        });
        subs.put("UUID", new SubstitutionHandler() {
            @Override
            public String sub(Player player, CommandTrigger trigger) {
                return player.getUniqueId().toString();
            }
        });
        subs.put("N", subs.get("NAME"));
        subs.put("WORLD", new SubstitutionHandler() {
            @Override
            public String sub(Player player, CommandTrigger trigger) {
                return player.getWorld().getName();
            }
        });
        subs.put("I", new SubstitutionHandler() {
            @Override
            public String sub(Player player, CommandTrigger trigger) {
                LogUtils.warning("Command substitution <I> is deprecated and will stop working in a future release.");
                return player.getItemInHand() == null ? "0" : Integer.toString(player.getItemInHand().getTypeId());
            }
        });
        subs.put("INAME", new SubstitutionHandler() {
            @Override
            public String sub(Player player, CommandTrigger trigger) {
                return player.getItemInHand() == null ? "Air" : ItemNames.lookup(player.getItemInHand());
            }
        });
        subs.put("MONEY", new SubstitutionHandler() {
            @Override
            public String sub(Player player, CommandTrigger trigger) {
                if (ScrollingMenuSign.economy != null) {
                    return ScrollingMenuSign.getInstance().isVaultLegacyMode() ?
                            SMSUtil.formatMoney(ScrollingMenuSign.economy.getBalance(player.getName())) :
                            SMSUtil.formatMoney(ScrollingMenuSign.economy.getBalance(player));
                } else {
                    return "0.00";
                }
            }
        });
        subs.put("VIEW", new SubstitutionHandler() {
            @Override
            public String sub(Player player, CommandTrigger trigger) {
                return trigger == null ? "" : trigger.getName();
            }
        });
        subs.put("EXP", new SubstitutionHandler() {
            @Override
            public String sub(Player player, CommandTrigger trigger) {
                return Integer.toString(new ExperienceManager(player).getCurrentExp());
            }
        });
        subs.put("COOLDOWN", new SubstitutionHandler() {
            @Override
            public String sub(Player player, CommandTrigger trigger) {
                CooldownCommandlet cc = (CooldownCommandlet) ScrollingMenuSign.getInstance().getCommandletManager().getCommandlet("COOLDOWN");
                long millis = cc.getLastCooldownTimeRemaining();
                int seconds = (int) (millis / 1000) % 60;
                if (millis < 60000) return String.format("%ds", seconds);
                int minutes = (int) ((millis / (1000 * 60)) % 60);
                if (millis < 3600000)
                    return String.format("%dm %ds", minutes, seconds);
                int hours = (int) ((millis / (1000 * 60 * 60)) % 24);
                return String.format("%dh %dm %ds", hours, minutes, seconds);
            }
        });
    }
}
