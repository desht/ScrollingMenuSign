package me.desht.scrollingmenusign.views.hologram;

import me.desht.dhutils.MinecraftChatStr;
import me.desht.scrollingmenusign.SMSMenuItem;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.enums.ViewJustification;
import me.desht.scrollingmenusign.views.SMSScrollableView;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class HoloUtil {
    public static final String LAST_HOLO_INTERACTION = "SMS_Last_Holo_Interaction";
    public static final long HOLO_POPDOWN_TIMEOUT = 100; // ms

    public static String[] buildText(SMSScrollableView view, Player player, int nLines) {
        String[] res = new String[nLines];

        String prefixNotSel = ScrollingMenuSign.getInstance().getConfigCache().getPrefixNotSelected();
        String prefixSel = ScrollingMenuSign.getInstance().getConfigCache().getPrefixSelected();

        int maxWidth = 0;

        List<String> titleLines = view.splitTitle(player);
        int nTitleLines = titleLines.size();
        for (int i = 0; i < nTitleLines; i++) {
            res[i] = titleLines.get(i);
            maxWidth = Math.max(maxWidth, MinecraftChatStr.getStringWidth(res[i]));
        }
        for (int i = nTitleLines; i < nLines; i++) {
            res[i] = "";
        }

        int scrollPos = view.getScrollPos(player);
        int menuSize = view.getActiveMenuItemCount(player);
        int pageSize = nLines - nTitleLines;
        switch (view.getScrollType()) {
            case SCROLL:
                for (int j = 0, pos = scrollPos; j < pageSize && j < menuSize; j++) {
                    String prefix = j == 0 ? prefixSel : prefixNotSel;
                    SMSMenuItem item = view.getActiveMenuItemAt(player, pos);
                    String lineText = view.getActiveItemLabel(player, pos);
                    res[j + nTitleLines] = formatItem(prefix, item.getIcon(), lineText);
                    maxWidth = Math.max(maxWidth, MinecraftChatStr.getStringWidth(res[j + nTitleLines]));
                    if (++pos > menuSize) {
                        pos = 1;
                    }
                }
                break;
            case PAGE:
                int pageNum = (scrollPos - 1) / pageSize;
                for (int j = 0, pos = (pageNum * pageSize) + 1; j < pageSize && pos <= menuSize; j++, pos++) {
                    String pre = pos == scrollPos ? prefixSel : prefixNotSel;
                    SMSMenuItem item = view.getActiveMenuItemAt(player, pos);
                    String lineText = view.getActiveItemLabel(player, pos);
                    res[j+ nTitleLines] = formatItem(pre, item.getIcon(), lineText);
                    maxWidth = Math.max(maxWidth, MinecraftChatStr.getStringWidth(res[j + nTitleLines]));
                }
                break;
        }

        for (int i = 0; i < nTitleLines; i++) {
            res[i] = padText(res[i], maxWidth, view.getTitleJustification());
        }
        for (int i = nTitleLines; i < nLines; i++) {
            res[i] = padText(res[i], maxWidth, view.getItemJustification());
        }

        return res;
    }

    @SuppressWarnings("UnusedParameters")
    private static String formatItem(String prefix, ItemStack icon, String lineText) {
// TODO see if there's a way to get both the icon *and* the text on one line
//        if (icon != null) {
//            return prefix + " %item:" + icon.getTypeId() + "," + icon.getDurability() + "% " + lineText;
//        } else {
//            return prefix + lineText;
//        }
        return prefix + lineText;
    }

    private static String padText(String text, int maxWidth, ViewJustification just) {
        switch (just) {
            case LEFT: return MinecraftChatStr.strPadRightChat(text, maxWidth, ' ');
            case CENTER: return MinecraftChatStr.strPadCenterChat(text, maxWidth, ' ');
            case RIGHT: return MinecraftChatStr.strPadLeftChat(text, maxWidth, ' ');
            default: throw new IllegalArgumentException("unknown justification: " + just);
        }
    }
}
