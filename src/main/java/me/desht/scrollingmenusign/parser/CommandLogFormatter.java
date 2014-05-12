package me.desht.scrollingmenusign.parser;

import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class CommandLogFormatter extends Formatter {
    public String format(LogRecord log) {
        StringBuilder sb = new StringBuilder("[ScrollingMenuSign] ");

        Date date = new Date(log.getMillis());
        sb.append(date.toString()).append(": ");
        sb.append(log.getLevel().getName()).append(": ");
        sb.append(log.getMessage()).append("\n");

        return sb.toString();
    }
}