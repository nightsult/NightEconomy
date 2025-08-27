package org.night.nighteconomy.util;

public class ChatUtil {
    private ChatUtil() {}

    public static String translateColors(String input) {
        if (input == null) return "";
        return input.replace('&', '§');
    }
}