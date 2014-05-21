package me.desht.scrollingmenusign.util;

import me.desht.dhutils.MiscUtil;
import org.apache.commons.lang.StringEscapeUtils;

public class SMSUtil {
    public static String escape(String s) {
        return StringEscapeUtils.escapeHtml(MiscUtil.unParseColourSpec(s));
    }

    public static String unEscape(String s) {
        return MiscUtil.parseColourSpec(StringEscapeUtils.unescapeHtml(s));
    }
}
