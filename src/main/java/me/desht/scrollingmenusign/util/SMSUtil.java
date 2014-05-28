package me.desht.scrollingmenusign.util;

import me.desht.dhutils.ItemGlow;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.TreeSpecies;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Dye;
import org.bukkit.material.MaterialData;

public class SMSUtil {
    public static String escape(String s) {
        return StringEscapeUtils.escapeHtml(MiscUtil.unParseColourSpec(s));
    }

    public static String unEscape(String s) {
        return MiscUtil.parseColourSpec(StringEscapeUtils.unescapeHtml(s));
    }

    public static ItemStack parseMaterialSpec(String spec) {
        if (spec == null || spec.isEmpty()) {
            return null;
        }

        try {
            String[] fields = spec.split(",");
            MaterialData mat = parseMatAndData(fields[0]);

            int amount = 1;
            boolean glowing = false;
            for (int i = 1; i < fields.length; i++) {
                if (StringUtils.isNumeric(fields[i])) {
                    amount = Integer.parseInt(fields[i]);
                } else if (fields[i].equalsIgnoreCase("glow")) {
                    glowing = true;
                }
            }
            ItemStack stack = mat.toItemStack(amount);
            if (glowing && ScrollingMenuSign.getInstance().isProtocolLibEnabled()) {
                ItemGlow.setGlowing(stack, true);
            }
            return stack;
        } catch (Exception e) {
            LogUtils.warning("Can't parse icon material [" + spec + "]: " + e.getMessage());
            return null;
        }
    }

    private static MaterialData parseMatAndData(String matData) {
        String[] fields = matData.split("[:()]");
        Material mat = Material.matchMaterial(fields[0]);
        if (mat == null) {
            throw new IllegalArgumentException("Unknown material " + fields[0]);
        }
        MaterialData res = new MaterialData(mat);
        if (fields.length > 1) {
            if (StringUtils.isNumeric(fields[1])) {
                res.setData(Byte.parseByte(fields[1]));
            } else {
                switch (mat) {
                    case INK_SACK:
                        Dye dye = new Dye();
                        dye.setColor(DyeColor.valueOf(fields[1].toUpperCase()));
                        res = dye;
                        break;
                    case WOOL:
                    case CARPET:
                    case STAINED_GLASS:
                    case STAINED_GLASS_PANE:
                    case STAINED_CLAY:
                        // maybe one day these will all implement Colorable...
                        DyeColor dc2 = DyeColor.valueOf(fields[1].toUpperCase());
                        res.setData(dc2.getWoolData());
                        break;
                    case SAPLING:
                    case WOOD:
                        TreeSpecies ts = TreeSpecies.valueOf(fields[1].toUpperCase());
                        res.setData(ts.getData());
                        break;
                }
            }
        }
        return res;
    }

    public static String freezeMaterialSpec(ItemStack stack) {
        MaterialData m = stack.getData();
        StringBuilder sb = new StringBuilder(m.getItemType().toString());
        if (stack.getDurability() != 0) {
            sb.append(":").append(stack.getDurability());
        }
        if (stack.getAmount() > 1) {
            sb.append(",").append(Integer.toString(stack.getAmount()));
        }
        if (ScrollingMenuSign.getInstance().isProtocolLibEnabled() && ItemGlow.hasGlow(stack)) {
            sb.append(",").append("glow");
        }
        return sb.toString();
    }
}
