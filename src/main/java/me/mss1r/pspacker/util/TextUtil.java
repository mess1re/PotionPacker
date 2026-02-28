package me.mss1r.pspacker.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextUtil {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final Pattern HEX = Pattern.compile("#([0-9a-fA-F]{6})");

    private TextUtil() {}

    /** Supports "#RRGGBB" -> "<#RRGGBB>" and plain MiniMessage. */
    public static Component mm(String input) {
        if (input == null || input.isEmpty()) return Component.empty();

        Matcher m = HEX.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, "<#" + m.group(1) + ">");
        }
        m.appendTail(sb);

        return MM.deserialize(sb.toString());
    }
}
